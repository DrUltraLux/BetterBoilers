package com.drultralux.betterboilers.tile;

import com.drultralux.betterboilers.BBLog;
import com.google.common.base.Predicates;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

public abstract class TileEntityMultiblockPart<T extends TileEntityMultiblockController> extends TileEntity implements IMultiblockPart, IControlledPart<T> {
    private T controller;
    private Vec3i controllerPos;

    protected abstract Class<T> getControllerClass();

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (controllerPos != null) {
            compound.setInteger("ControllerOffsetX", controllerPos.getX());
            compound.setInteger("ControllerOffsetY", controllerPos.getY());
            compound.setInteger("ControllerOffsetZ", controllerPos.getZ());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("ControllerOffsetX")) {
            controllerPos = new Vec3i(
                    compound.getInteger("ControllerOffsetX"),
                    compound.getInteger("ControllerOffsetY"),
                    compound.getInteger("ControllerOffsetZ"));
        } else {
            controllerPos = null;
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    /**
     * Explicitly pushes this part's current controllerPos to nearby players, mirroring the
     * pattern the controllers themselves already use. Without this, a part's client-side copy
     * never learns which controller it belongs to until its chunk happens to fully reload from
     * scratch - a plain markDirty() call does NOT trigger tile-entity network sync by itself, so
     * a pipe's connected-geometry check (which reads getController() on the client to decide
     * whether to draw a connection arm toward this block) would see a stale, permanently-null
     * controller even though the block is fully functional server-side.
     */
    private void syncToNearbyPlayers() {
        if (!hasWorld() || getWorld().isRemote) return;
        WorldServer ws = (WorldServer) getWorld();
        Chunk c = getWorld().getChunkFromBlockCoords(getPos());
        SPacketUpdateTileEntity packet = new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
        for (EntityPlayerMP player : getWorld().getPlayers(EntityPlayerMP.class, Predicates.alwaysTrue())) {
            if (ws.getPlayerChunkMap().isPlayerWatchingChunk(player, c.x, c.z)) {
                player.connection.sendPacket(packet);
            }
        }
    }

    public boolean hasController() {
        return getController() != null;
    }

    @Override
    public T getController() {
        if (!hasWorld()) return null;
        if (controller != null && controller.isInvalid()) controller = null;
        if (controller == null && controllerPos != null) {
            BlockPos pos = getPos().add(controllerPos);
            TileEntity te = getWorld().getTileEntity(pos);
            if (getControllerClass().isInstance(te)) {
                controller = getControllerClass().cast(te);
            } else {
                controllerPos = null;
                BBLog.debug("The network member at {}, {}, {} failed to find its controller", getPos().getX(), getPos().getY(), getPos().getZ());
            }
        }
        return controller;
    }

    @Override
    public void setController(T controller) {
        if (!hasWorld()) return;
        if (controller == null) {
            controllerPos = null;
        } else {
            controllerPos = controller.getPos().subtract(getPos());
        }
        this.controller = controller;
        syncToNearbyPlayers();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void handleNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        if (hasController()) {
            getController().requestRescan();
        }
    }

    /**
     * Java's type system cannot prove that a Forge capability-token equality check
     * (capability == SomeCapability.INSTANCE) implies T equals the concrete handler type - that
     * link only exists at runtime, once the token comparison has already passed. This is the
     * single, deliberately isolated place in a part tile's getCapability() where an unchecked
     * cast is unavoidable. Mirrors TileEntityMultiblockController.castCapabilityHandler() for the
     * same reason, kept separate since parts and controllers don't share a common ancestor.
     */
    @SuppressWarnings("unchecked")
    protected static <R> R castCapabilityHandler(Object handler) {
        return (R) handler;
    }
}