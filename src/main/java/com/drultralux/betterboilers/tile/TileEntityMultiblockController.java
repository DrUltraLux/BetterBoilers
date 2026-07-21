package com.drultralux.betterboilers.tile;

import com.drultralux.betterboilers.util.BBConfig;
import com.drultralux.betterboilers.network.BBNetwork;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiPredicate;

public abstract class TileEntityMultiblockController extends TileEntity implements IMultiblockPart {

    protected int getMaxBlocksPerMultiblock() { return BBConfig.defaultMaxMultiblock; }
    protected String status;
    public TextComponentTranslation errorReason;
    public enum ControllerStatus { ACTIVE, ERRORED }

    private static final int RESCAN_TIME = 100;
    private int currentScanTime = 0;
    private boolean restartRequested = true;

    private boolean scanning = false;
    private ArrayDeque<BlockPos> scanQueue;
    private LinkedHashSet<BlockPos> scanSeen;
    private LinkedHashSet<BlockPos> scanMembers;
    private BiPredicate<World, BlockPos> scanMembership;
    private BiPredicate<World, List<BlockPos>> scanValidator;

    private List<BlockPos> previousMembers = Collections.emptyList();

    private String clientStatusKey = "msg.bb.notYetScanned";

    public void requestRescan() {
        this.restartRequested = true;
    }

    protected boolean consumeRescanTrigger() {
        currentScanTime++;
        if (restartRequested || currentScanTime >= RESCAN_TIME) {
            restartRequested = false;
            currentScanTime = 0;
            return true;
        }
        return false;
    }

    @Override
    public void handleNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        requestRescan();
    }

    protected void tickScan(BiPredicate<World, BlockPos> membership, BiPredicate<World, List<BlockPos>> validator) {
        if (!hasWorld() || world.isRemote) return;

        if (!scanning) {
            currentScanTime++;
            if (restartRequested || currentScanTime >= RESCAN_TIME) {
                startScan(membership, validator);
            } else {
                return;
            }
        }

        int budget = BBConfig.maxBlocksScannedPerTick;
        while (scanning && budget-- > 0) {
            stepScan();
        }
    }

    private void startScan(BiPredicate<World, BlockPos> membership, BiPredicate<World, List<BlockPos>> validator) {
        restartRequested = false;
        currentScanTime = 0;

        scanMembership = membership;
        scanValidator = validator;
        scanQueue = new ArrayDeque<>();
        scanSeen = new LinkedHashSet<>();
        scanMembers = new LinkedHashSet<>();

        onDisassemble(world, previousMembers);

        scanQueue.add(getPos());
        scanSeen.add(getPos());
        scanning = true;
    }

    private void stepScan() {
        if (scanMembers.size() > getMaxBlocksPerMultiblock()) {
            finishScan(false);
            return;
        }
        if (scanQueue.isEmpty()) {
            finishScan(true);
            return;
        }

        BlockPos pos = scanQueue.poll();
        if (scanMembership.test(world, pos)) {
            for (EnumFacing ef : EnumFacing.VALUES) {
                BlockPos p = pos.offset(ef);
                if (scanSeen.contains(p)) continue;
                scanSeen.add(p);
                scanQueue.add(p);
            }
            scanMembers.add(pos);
        }
    }

    private void finishScan(boolean withinSizeLimit) {
        scanning = false;
        List<BlockPos> members = new ArrayList<>(scanMembers);

        if (!withinSizeLimit) {
            setControllerStatus(ControllerStatus.ERRORED, "msg.bb.tooBig");
            previousMembers = Collections.emptyList();
        } else if (!scanValidator.test(world, members)) {
            setControllerStatus(ControllerStatus.ERRORED, status);
            previousMembers = Collections.emptyList();
        } else {
            onAssemble(world, members);
            setControllerStatus(ControllerStatus.ACTIVE, "msg.bb.noIssue");
            previousMembers = members;
        }

        scanQueue = null;
        scanSeen = null;
        scanMembers = null;
    }

    public String getClientStatusKey() {
        return clientStatusKey;
    }

    public void setClientStatusKeyFromNetwork(String key) {
        this.clientStatusKey = key;
    }

    protected String lastSentStatusKey = null;

    /** Pushes the current status directly to one specific player, bypassing the normal
     *  tracking-broadcast mechanism entirely - used when a player opens this controller's
     *  GUI, so they get accurate info immediately regardless of any chunk-watch timing race
     *  (e.g. right after joining a world, before the server has finished registering them
     *  as "watching" this chunk, which would otherwise silently drop a normal status update). */
    public void resyncStatusTo(EntityPlayerMP player) {
        if (lastSentStatusKey == null) return;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("x", getPos().getX());
        payload.setInteger("y", getPos().getY());
        payload.setInteger("z", getPos().getZ());
        payload.setString("key", lastSentStatusKey);
        BBNetwork.sendToPlayer(player, "controller_status", payload);
    }

    public void setControllerStatus(ControllerStatus state, String status) { }

    public abstract void onAssemble(World world, List<BlockPos> blocks);

    public abstract void onDisassemble(World world, List<BlockPos> blocks);

}