package com.drultralux.betterboilers.pipe;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.util.BBConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Single global manager for all pipe networks (fluid/energy/heat) across all worlds.
 *
 * Two independent, config-tunable budgets keep a pathological world (hundreds of separate
 * networks, or a single huge one) from spiking a tick:
 *   - rebuildBlocksPerTick: total pipe blocks visited via BFS this tick, across all in-progress
 *     rebuild jobs combined. A rebuild that doesn't finish within budget just resumes next tick
 *     from where it left off (same tick-budgeted pattern as the multiblock scanner).
 *   - distributeBudgetPerTick: how many *active* networks get their distribute() call this tick.
 *     A round-robin cursor per world+type ensures no network starves if there are more active
 *     networks than the budget allows in one tick - the remainder just gets picked up next tick.
 *
 * Idle networks (needsDistribution() == false) are never counted against the distribute budget -
 * checking that flag is a handful of field reads, not worth batching.
 */
public class PipeNetworkManager {

    public static final PipeNetworkManager INSTANCE = new PipeNetworkManager();

    private PipeNetworkManager() {}

    private final Map<World, WorldState> states = new WeakHashMap<>();

    private WorldState stateFor(World world) {
        return states.computeIfAbsent(world, w -> new WorldState());
    }

    public void markDirty(World world, BlockPos pos, PipeType type) {
        WorldState state = stateFor(world);
        Set<BlockPos> queued = state.queuedDirty.get(type);
        if (queued.add(pos.toImmutable())) {
            state.dirtyQueue.get(type).add(pos.toImmutable());
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        states.remove(event.getWorld());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        WorldState state = states.get(event.world);
        if (state == null) {
            return;
        }

        for (PipeType type : PipeType.values()) {
            processRebuilds(event.world, state, type);
            processDistribution(state, type);
        }
    }

    // --- rebuilds ---

    private void processRebuilds(World world, WorldState state, PipeType type) {
        int budget = BBConfig.pipeRebuildBlocksPerTick;
        int visited = 0;

        Deque<RebuildJob> jobs = state.activeJobs.get(type);
        Deque<BlockPos> dirty = state.dirtyQueue.get(type);
        Set<BlockPos> queuedDirty = state.queuedDirty.get(type);

        while (visited < budget) {
            RebuildJob job = jobs.peekFirst();
            if (job == null) {
                BlockPos seed = dirty.poll();
                if (seed == null) {
                    break; // nothing left to do this tick for this type
                }
                if (!queuedDirty.contains(seed)) {
                    // Already absorbed and rebuilt by an earlier job in this same pass (e.g. a new
                    // pipe bridging two networks marks itself AND its neighbors dirty in the same
                    // tick) - starting a fresh job here would just redundantly re-merge the same
                    // network from scratch and throw away the one just built.
                    continue;
                }
                queuedDirty.remove(seed);
                if (!(world.getTileEntity(seed) instanceof TileEntityPipe)) {
                    continue; // already gone/replaced, nothing to rebuild from here
                }
                job = new RebuildJob(seed, type);
                jobs.addFirst(job);
            }

            visited += runJob(world, job, budget - visited);

            if (job.frontier.isEmpty()) {
                jobs.pollFirst();
                finishJob(world, state, type, job);
                // any dirty positions this job absorbed along the way are already handled -
                // no need to look them up again in the dirty queue.
                for (BlockPos p : job.visited) {
                    queuedDirty.remove(p);
                }
            }
            // if frontier isn't empty, budget ran out - job stays at the front to resume next tick.
        }
    }

    /** Runs one BFS job up to maxSteps block visits. Returns how many blocks were actually visited. */
    private int runJob(World world, RebuildJob job, int maxSteps) {
        int steps = 0;
        PipeType type = job.type;
        while (steps < maxSteps && !job.frontier.isEmpty()) {
            BlockPos pos = job.frontier.poll();
            if (job.visited.contains(pos)) {
                continue;
            }
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof TileEntityPipe) || ((TileEntityPipe) te).getPipeType() != type) {
                continue;
            }
            TileEntityPipe pipe = (TileEntityPipe) te;
            job.visited.add(pos);
            steps++;

            PipeNetwork<?> old = pipe.getNetwork();
            if (old != null && old.valid && !job.oldNetworks.contains(old)) {
                job.oldNetworks.add(old);
            }

            pipe.refreshConnections();
            for (EnumFacing side : EnumFacing.VALUES) {
                if (isSamePipeType(world, pos, side, type)) {
                    BlockPos next = pos.offset(side);
                    if (!job.visited.contains(next)) {
                        job.frontier.add(next);
                    }
                }
            }
        }
        return steps;
    }

    private boolean isSamePipeType(World world, BlockPos pos, EnumFacing side, PipeType type) {
        TileEntity te = world.getTileEntity(pos.offset(side));
        return te instanceof TileEntityPipe && ((TileEntityPipe) te).getPipeType() == type;
    }

    private void finishJob(World world, WorldState state, PipeType type, RebuildJob job) {
        if (job.visited.isEmpty()) {
            return;
        }
        BlockPos anyPos = job.visited.iterator().next();
        TileEntity anyTe = world.getTileEntity(anyPos);
        if (!(anyTe instanceof TileEntityPipe)) {
            return; // gone since the BFS ran - a fresh neighbor-change will re-queue whatever's left
        }

        PipeNetwork<?> network = ((TileEntityPipe) anyTe).createNetwork();
        network.mergeFrom(new ArrayList<>(job.oldNetworks));
        for (PipeNetwork<?> old : job.oldNetworks) {
            old.invalidate();
        }

        for (BlockPos p : job.visited) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileEntityPipe) {
                network.members.add(p);
                ((TileEntityPipe) te).setNetwork(network);
            }
        }

        state.networks.get(type).add(network);
        BBLog.debug("Rebuilt {} pipe network at {}: {} members", type, anyPos, network.size());
    }

    // --- distribution ---

    private void processDistribution(WorldState state, PipeType type) {
        List<PipeNetwork<?>> networks = state.networks.get(type);
        if (networks.isEmpty()) {
            return;
        }

        // prune invalidated networks (replaced by a rebuild, or emptied out to zero members)
        Iterator<PipeNetwork<?>> it = networks.iterator();
        while (it.hasNext()) {
            PipeNetwork<?> n = it.next();
            if (!n.valid || n.size() == 0) {
                it.remove();
            }
        }
        if (networks.isEmpty()) {
            return;
        }

        int budget = BBConfig.pipeDistributeBudgetPerTick;
        int count = networks.size();
        int cursor = state.distributeCursor.get(type) % count;

        int processed = 0;
        int index = cursor;
        while (processed < budget && processed < count) {
            PipeNetwork<?> network = networks.get(index);
            if (network.valid && network.needsDistribution()) {
                network.distribute();
            }
            index = (index + 1) % count;
            processed++;
        }
        state.distributeCursor.put(type, index);
    }

    // --- per-world bookkeeping ---

    private static class WorldState {
        final Map<PipeType, Deque<BlockPos>> dirtyQueue = freshMap(k -> new ArrayDeque<>());
        final Map<PipeType, Set<BlockPos>> queuedDirty = freshMap(k -> new HashSet<>());
        final Map<PipeType, Deque<RebuildJob>> activeJobs = freshMap(k -> new ArrayDeque<>());
        final Map<PipeType, List<PipeNetwork<?>>> networks = freshMap(k -> new ArrayList<>());
        final Map<PipeType, Integer> distributeCursor = new EnumMap<>(PipeType.class);

        WorldState() {
            for (PipeType t : PipeType.values()) {
                distributeCursor.put(t, 0);
            }
        }

        private static <V> Map<PipeType, V> freshMap(Function<PipeType, V> factory) {
            Map<PipeType, V> map = new EnumMap<>(PipeType.class);
            for (PipeType t : PipeType.values()) {
                map.put(t, factory.apply(t));
            }
            return map;
        }
    }

    private static class RebuildJob {
        final PipeType type;
        final Deque<BlockPos> frontier = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
        final Set<PipeNetwork<?>> oldNetworks = new HashSet<>();

        RebuildJob(BlockPos seed, PipeType type) {
            this.type = type;
            frontier.add(seed);
        }
    }
}
