package com.drultralux.betterboilers.tile.boiler;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.block.boiler.BlockBoilerController;
import com.drultralux.betterboilers.block.boiler.ITankBlock;
import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.network.BBNetwork;
import com.drultralux.betterboilers.pipe.heat.HeatCapability;
import com.drultralux.betterboilers.pipe.heat.HeatStorage;
import com.drultralux.betterboilers.tile.IControlledPart;
import com.drultralux.betterboilers.tile.IFieldProvider;
import com.drultralux.betterboilers.tile.TileEntityMultiblockController;
import com.drultralux.betterboilers.util.BBConfig;
import com.drultralux.betterboilers.util.ReadableDoubleTank;
import com.google.common.base.Predicates;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Tank-only multiblock controller. Water and heat come in (heat via a neighboring heat pipe or
 * heat sink, not fuel/inventory - that all lives on the Furnace Controller now), steam goes out.
 * No item inventory of any kind belongs here anymore.
 */
public class TileEntityBoilerController extends TileEntityMultiblockController implements ITickable, IFieldProvider, IControlledPart<TileEntityBoilerController> {

    public FluidTank tankWater;
    public FluidTank tankSteam;
    private ReadableDoubleTank cap;
    private HeatStorage heat;
    private int boilerBlockCount = 0;
    private int lastHeatConsumedPerTick = 0;

    protected int getMaxBlocksPerMultiblock() { return BBConfig.defaultMaxMultiblock; }

    public TileEntityBoilerController getController() {
        return this;
    }

    public void setController(TileEntityBoilerController controller) {
    }

    public TileEntityBoilerController() {
        this.tankWater = new FluidTank(1000) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                if (fluid == null || fluid.getFluid() == null) {
                    return false;
                }
                return FluidRegistry.WATER.getName().equals(fluid.getFluid().getName());
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                int filled = super.fill(resource, doFill);
                if (doFill && filled > 0) markDirty();
                return filled;
            }

            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                FluidStack drained = super.drain(maxDrain, doDrain);
                if (doDrain && drained != null) markDirty();
                return drained;
            }
        };

        this.tankSteam = new FluidTank(500) {
            @Override
            public int fill(FluidStack resource, boolean doFill) {
                int filled = super.fill(resource, doFill);
                if (doFill && filled > 0) markDirty();
                return filled;
            }

            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                FluidStack drained = super.drain(maxDrain, doDrain);
                if (doDrain && drained != null) markDirty();
                return drained;
            }
        };

        this.cap = new ReadableDoubleTank(tankWater, tankSteam);
        this.heat = new HeatStorage(0);
    }

    public void update() {
        tickScan((w, p) -> w.getBlockState(p).getBlock() instanceof ITankBlock, this::isValid);

        if (!world.isRemote) {
            int budget = boilerBlockCount * BBConfig.tankHeatConsumedPerTickPerBlock;
            int availableHeat = Math.min(heat.getHeatStored(), budget);
            if (availableHeat > 0) {
                int maxByHeat = availableHeat / BBConfig.heatPerSteamUnit;
                int maxByWater = tankWater.getFluidAmount() / 2;
                int maxBySpace = tankSteam.getCapacity() - tankSteam.getFluidAmount();
                int toProduce = Math.min(maxByHeat, Math.min(maxByWater, maxBySpace));
                BBLog.debug("Boiler at {}: boilerBlockCount={}, budget={}, availableHeat={}, maxByHeat={}, maxByWater={}, maxBySpace={}, toProduce={}",
                        getPos(), boilerBlockCount, budget, availableHeat, maxByHeat, maxByWater, maxBySpace, toProduce);
                if (toProduce > 0) {
                    heat.consumeHeat(toProduce * BBConfig.heatPerSteamUnit);
                    tankWater.drain(2 * toProduce, true);
                    tankSteam.fill(new FluidStack(ModBlocks.FLUID_STEAM, toProduce), true);
                    lastHeatConsumedPerTick = toProduce * BBConfig.heatPerSteamUnit;
                } else {
                    lastHeatConsumedPerTick = 0;
                }
            } else {
                lastHeatConsumedPerTick = 0;
                BBLog.debug("Boiler at {}: boilerBlockCount={}, budget={}, availableHeat=0 - no heat to consume this tick",
                        getPos(), boilerBlockCount, budget);
            }
            markDirty();
        }
    }

    public boolean isValid(World world, List<BlockPos> blocks) {
        int minY = 255;
        int boilerCount = 0;
        int valveCount = 0;
        int ventCount = 0;
        for (BlockPos pos : blocks) minY = Math.min(pos.getY(), minY);
        if (this.pos.getY() != minY) {
            status = "msg.bb.badBoilerController";
            return false;
        }

        for (BlockPos pos : blocks) {
            if (world.getTileEntity(pos) instanceof TileEntityMultiblockController) {
                if (pos != this.getPos()) {
                    status = "msg.bb.tooManyControllers";
                    return false;
                }
            }
            if (world.getBlockState(pos).getBlock() == ModBlocks.BOILER) boilerCount++;
            if (world.getBlockState(pos).getBlock() == ModBlocks.VALVE) valveCount++;
            if (world.getBlockState(pos).getBlock() == ModBlocks.VENT) ventCount++;
        }
        if (boilerCount < 1 || valveCount < 1 || ventCount < 1) {
            status = "msg.bb.needsTankParts";
            return false;
        }
        return true;
    }

    @Override
    public void onAssemble(World world, List<BlockPos> blocks) {
        boilerBlockCount = 0;
        for (BlockPos pos : blocks) {
            if (world.getBlockState(pos).getBlock() == ModBlocks.BOILER
                    || world.getBlockState(pos).getBlock() == ModBlocks.VENT
                    || world.getBlockState(pos).getBlock() == ModBlocks.VALVE) {
                boilerBlockCount++;
            }
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileEntityBoilerPart) {
                ((TileEntityBoilerPart)te).setController(this);
            }
        }
        tankWater.setCapacity(1000*boilerBlockCount);
        tankSteam.setCapacity(500*boilerBlockCount);
        heat.setCapacity(boilerBlockCount * BBConfig.tankHeatCapacityPerBlock);
        heat.setMaxInsert(boilerBlockCount * BBConfig.heatInsertPerTickPerBlock);
        heat.setMaxExtract(0); // the tank only ever accepts heat - it never exports it back out
        markDirty();
    }

    @Override
    public void onDisassemble(World world, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityBoilerPart) {
                ((TileEntityBoilerPart) te).setController(null);
            }
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound tag = super.writeToNBT(compound);
        tag.setInteger("BoilerCount", boilerBlockCount);
        tag.setTag("WaterTank", tankWater.writeToNBT(new NBTTagCompound()));
        tag.setTag("SteamTank", tankSteam.writeToNBT(new NBTTagCompound()));
        tag.setInteger("HeatStored", heat.getHeatStored());
        tag.setInteger("HeatCapacity", heat.getMaxHeatStored());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        boilerBlockCount = compound.getInteger("BoilerCount");
        tankWater.setCapacity(1000 * boilerBlockCount);
        tankSteam.setCapacity(500 * boilerBlockCount);
        tankWater.readFromNBT(compound.getCompoundTag("WaterTank"));
        tankSteam.readFromNBT(compound.getCompoundTag("SteamTank"));
        int capacity = compound.getInteger("HeatCapacity");
        int stored = compound.getInteger("HeatStored");
        heat = new HeatStorage(capacity, capacity, capacity, stored);
        heat.setMaxInsert(boilerBlockCount * BBConfig.heatInsertPerTickPerBlock);
        heat.setMaxExtract(0);
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

    @Override
    public void markDirty() {
        super.markDirty();
        if (!hasWorld() || getWorld().isRemote) return;
        WorldServer ws = (WorldServer)getWorld();
        Chunk c = getWorld().getChunkFromBlockCoords(getPos());
        SPacketUpdateTileEntity packet = new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
        for (EntityPlayerMP player : getWorld().getPlayers(EntityPlayerMP.class, Predicates.alwaysTrue())) {
            if (ws.getPlayerChunkMap().isPlayerWatchingChunk(player, c.x, c.z)) {
                player.connection.sendPacket(packet);
            }
        }
    }

    public int getField(int id) {
        switch (id) {
            case 0: return heat.getHeatStored();
            case 1: return heat.getMaxHeatStored();
            case 2: return lastHeatConsumedPerTick;
            default: return 0;
        }
    }

    public void setField(int id, int value) {
        switch (id) {
            case 2: lastHeatConsumedPerTick = value; break;
            default: break;
        }
    }

    public int getFieldCount() {
        return 3;
    }

    public FluidTank getTankWater() {
        return tankWater;
    }

    public FluidTank getTankSteam() {
        return tankSteam;
    }

    public HeatStorage getHeat() {
        return heat;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return castCapabilityHandler(cap);
        }
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            BBLog.debug("Boiler at {} exposing heat sink: stored={}, capacity={}",
                    getPos(), heat.getHeatStored(), heat.getMaxHeatStored());
            return castCapabilityHandler(heat);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        if (oldState.getBlock()==newState.getBlock()) return false;
        else return super.shouldRefresh(world, pos, oldState, newState);
    }

    public void setControllerStatus(ControllerStatus state, String status) {
        errorReason = new TextComponentTranslation(status);
        if (state == ControllerStatus.ERRORED) {
            world.setBlockState(this.getPos(), ModBlocks.BOILER_CONTROLLER.getDefaultState().withProperty(BlockBoilerController.ACTIVE, false));
        } else {
            world.setBlockState(this.getPos(), ModBlocks.BOILER_CONTROLLER.getDefaultState().withProperty(BlockBoilerController.ACTIVE, true));
        }

        if (!status.equals(lastSentStatusKey)) {
            lastSentStatusKey = status;
            NBTTagCompound payload = new NBTTagCompound();
            payload.setInteger("x", getPos().getX());
            payload.setInteger("y", getPos().getY());
            payload.setInteger("z", getPos().getZ());
            payload.setString("key", status);
            BBNetwork.sendToAllTracking(this, "controller_status", payload);
        }
    }

}