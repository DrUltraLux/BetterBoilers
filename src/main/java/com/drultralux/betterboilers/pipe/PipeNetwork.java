package com.drultralux.betterboilers.pipe;

import com.drultralux.betterboilers.BBLog;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A connected group of same-type pipes sharing one pooled resource.
 *
 * Networks are never persisted to NBT. They are rebuilt lazily by {@link PipeNetworkManager}
 * whenever topology changes (pipe placed/broken/neighbor changed) or on world load, and are
 * cheap to throw away and re-derive - the only thing worth preserving across a rebuild is the
 * pooled resource itself, which {@link #mergeFrom(List)} is responsible for carrying over.
 *
 * {@code H} is the capability interface external tiles expose for this resource (IFluidHandler,
 * IEnergyStorage, IHeatHandler). The pull/push transfer loop below is identical in shape for
 * every resource type, so it lives here exactly once; leaf classes only supply the small pieces
 * that actually differ (which capability to look for, how much a resource unit takes, and how to
 * call the specific Forge/our-own API to move it).
 */
public abstract class PipeNetwork<H> {

    protected final World world;
    public final Set<BlockPos> members = new HashSet<>();

    /** Set false once this network has been replaced by a rebuild. A stale reference should never be used again. */
    public boolean valid = true;

    protected PipeNetwork(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

    public int size() {
        return members.size();
    }

    public void invalidate() {
        valid = false;
    }

    /** Capacity contributed by a single pipe segment - total network capacity is this times member count. */
    protected abstract long getCapacityPerSegment();

    public long getCapacity() {
        return (long) size() * getCapacityPerSegment();
    }

    /** Max amount this network may pull in and push out, combined, in one tick - independent of network size. */
    protected abstract int getMaxTransferPerTick();

    /** The capability external tiles must expose for this network to talk to them. */
    protected abstract Capability<H> getCapabilityType();

    /** Current amount held in this network's pool, expressed as a plain quantity regardless of resource type. */
    public abstract long getStoredAmount();

    /**
     * Called once, right after construction and before this network is assigned to any member,
     * to carry over pooled resource from whichever old network(s) are being merged/split here.
     * Takes a wildcarded list rather than List&lt;PipeNetwork&lt;H&gt;&gt; because the caller
     * (PipeNetworkManager) collects old networks from a BFS without static knowledge that they
     * all share this network's H - each implementation is expected to validate with instanceof
     * and log via BBLog rather than assume, exactly as it would for any other untrusted input.
     * Implementations should be conservative: prefer not to destroy resource silently, and log
     * via BBLog when something unexpected has to be discarded, but never exceed the new capacity.
     */
    public abstract void mergeFrom(List<PipeNetwork<?>> oldNetworks);

    /** Cheap per-tick check - only skips distribute() entirely for networks with no reason to ever run it. */
    public boolean needsDistribution() {
        return true; // idle networks still need to check for adjacent sources every tick
    }

    /** Pull up to maxAmount from an external source handler into this network's pool. Returns the amount actually pulled. */
    protected abstract int pullFrom(H source, int maxAmount);

    /** Push up to maxAmount from this network's pool into an external sink handler. Returns the amount actually pushed. */
    protected abstract int pushTo(H sink, int maxAmount);

    /**
     * Move pooled resource for one tick: pull from adjacent sources, then push to adjacent sinks.
     * Never called reactively from inside a capability call - only from PipeNetworkManager's
     * scheduled tick step. Final because every resource type shares this exact shape; only
     * pullFrom/pushTo differ per type.
     */
    public final void distribute() {
        pullFromSources();
        pushToSinks();
    }

    private void pullFromSources() {
        long room = getCapacity() - getStoredAmount();
        if (room <= 0) {
            return;
        }
        int budget = (int) Math.max(0, Math.min(room, getMaxTransferPerTick()));
        if (budget <= 0) {
            return;
        }

        Capability<H> capabilityType = getCapabilityType();
        if (capabilityType == null) {
            BBLog.warn("Pipe network for {} has no capability type registered yet - skipping distribution this tick", getClass().getSimpleName());
            return;
        }

        for (BlockPos pos : members) {
            if (budget <= 0) {
                break;
            }
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof TileEntityPipe)) {
                continue;
            }
            TileEntityPipe pipe = (TileEntityPipe) te;
            for (EnumFacing side : EnumFacing.VALUES) {
                if (budget <= 0) {
                    break;
                }
                TileEntity neighbor = world.getTileEntity(pos.offset(side));
                if (neighbor == null || neighbor instanceof TileEntityPipe
                        || !neighbor.hasCapability(capabilityType, side.getOpposite())) {
                    continue;
                }
                if (!pipe.beginTransfer()) {
                    continue;
                }
                try {
                    H handler = neighbor.getCapability(capabilityType, side.getOpposite());
                    if (handler == null) {
                        continue;
                    }
                    int pulled = pullFrom(handler, budget);
                    if (pulled > 0) {
                        budget -= pulled;
                        neighbor.markDirty();
                    }
                } finally {
                    pipe.endTransfer();
                }
            }
        }
    }

    private void pushToSinks() {
        long stored = getStoredAmount();
        if (stored <= 0) {
            return;
        }
        int budget = (int) Math.max(0, Math.min(stored, getMaxTransferPerTick()));
        if (budget <= 0) {
            return;
        }

        Capability<H> capabilityType = getCapabilityType();
        if (capabilityType == null) {
            BBLog.warn("Pipe network for {} has no capability type registered yet - skipping distribution this tick", getClass().getSimpleName());
            return;
        }

        for (BlockPos pos : members) {
            if (budget <= 0 || getStoredAmount() <= 0) {
                break;
            }
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof TileEntityPipe)) {
                continue;
            }
            TileEntityPipe pipe = (TileEntityPipe) te;
            for (EnumFacing side : EnumFacing.VALUES) {
                if (budget <= 0 || getStoredAmount() <= 0) {
                    break;
                }
                TileEntity neighbor = world.getTileEntity(pos.offset(side));
                if (neighbor == null || neighbor instanceof TileEntityPipe
                        || !neighbor.hasCapability(capabilityType, side.getOpposite())) {
                    continue;
                }
                if (!pipe.beginTransfer()) {
                    continue;
                }
                try {
                    H handler = neighbor.getCapability(capabilityType, side.getOpposite());
                    if (handler == null) {
                        continue;
                    }
                    int pushed = pushTo(handler, budget);
                    if (pushed > 0) {
                        budget -= pushed;
                        neighbor.markDirty();
                    }
                } finally {
                    pipe.endTransfer();
                }
            }
        }
    }
}
