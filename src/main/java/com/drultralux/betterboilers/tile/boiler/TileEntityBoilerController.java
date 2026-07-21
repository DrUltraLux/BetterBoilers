package com.drultralux.betterboilers.tile.boiler;

import com.drultralux.betterboilers.block.boiler.BlockBoilerController;
import com.drultralux.betterboilers.block.boiler.IBoilerBlock;
import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.tile.IControlledPart;
import com.drultralux.betterboilers.tile.IFieldProvider;
import com.drultralux.betterboilers.tile.TileEntityMultiblockController;
import com.drultralux.betterboilers.util.BBConfig;
import com.drultralux.betterboilers.network.BBNetwork;
import com.drultralux.betterboilers.util.ReadableDoubleTank;
import com.google.common.base.Predicates;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntityBoilerController extends TileEntityMultiblockController implements ITickable, IFieldProvider, IControlledPart<TileEntityBoilerController> {

    public FluidTank tankWater;
    public FluidTank tankSteam;
    private ReadableDoubleTank cap;
    public ItemStackHandler inv;
    private int boilerBlockCount = 0;
    private int fireboxBlockCount = 0;
    public int pumpCount = 0;

    private int currentProcessTime;
    private static final int PROCESS_LENGTH = BBConfig.ticksToBoil;
    private int[] currentFuelTime = new int[3];
    private int[] maxFuelTime = new int[3];
    private int fuelThisTick = 0;

    protected int getMaxBlocksPerMultiblock() { return BBConfig.defaultMaxMultiblock; }

    public TileEntityBoilerController getController() {
        return this;
    }

    public void setController(TileEntityBoilerController controller) {
    }

    public TileEntityBoilerController() {
        this.inv = new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileEntityFurnace.isItemFuel(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
            }
        };

        this.tankWater = new FluidTank(1000) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return fluid != null && fluid.getFluid() == FluidRegistry.WATER;
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
    }

    public void update() {
        tickScan((w, p) -> w.getBlockState(p).getBlock() instanceof IBoilerBlock, this::isValid);

        if (!world.isRemote) {
            if (pumpCount == 0) {
                if (canProcessFluid()) {
                    if (consumeFuel(0)) currentProcessTime += fireboxBlockCount;
                    if (consumeFuel(1)) currentProcessTime += fireboxBlockCount;
                    if (consumeFuel(2)) currentProcessTime += fireboxBlockCount;
                    if (tankWater.getFluidAmount() == 0) currentProcessTime = 0;
                    if (currentProcessTime >= PROCESS_LENGTH) {
                        tankWater.drain(2 * BBConfig.steamPerBoil, true);
                        tankSteam.fill(new FluidStack(ModBlocks.FLUID_STEAM, BBConfig.steamPerBoil), true);
                        currentProcessTime -= PROCESS_LENGTH;
                    }
                }
            } else {
                if (canPumpFluid()) {
                    fuelThisTick = 0;
                    if (consumeFuel(0)) fuelThisTick++;
                    if (consumeFuel(1)) fuelThisTick++;
                    if (consumeFuel(2)) fuelThisTick++;
                    double fuelPerTick = (double) BBConfig.steamPerBoil / (double) BBConfig.ticksToBoil;
                    int toProcess = (int) Math.ceil(fireboxBlockCount * fuelThisTick * fuelPerTick * BBConfig.pumpMultiplier);
                    tankWater.drain(2 * toProcess, true);
                    tankSteam.fill(new FluidStack(ModBlocks.FLUID_STEAM, toProcess), true);
                }
            }
        }
    }

    public boolean isValid(World world, List<BlockPos> blocks) {
        int minY = 255;
        int validBlockCount = 0;
        for(BlockPos pos : blocks) minY = Math.min(pos.getY(), minY);
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
                validBlockCount++;
            }
            if (world.getBlockState(pos).getBlock() == ModBlocks.BOILER
                    || world.getBlockState(pos).getBlock() == ModBlocks.VENT
                    || world.getBlockState(pos).getBlock() == ModBlocks.VALVE
                    || world.getBlockState(pos).getBlock() == ModBlocks.PUMP) {
                if (pos.getY() == minY) {
                    status = "msg.bb.badBoiler";
                    return false;
                }
                validBlockCount++;
            }
            if (world.getBlockState(pos).getBlock() == ModBlocks.FIREBOX
                    || world.getBlockState(pos).getBlock() == ModBlocks.HATCH) {
                if (pos.getY() != minY) {
                    status = "msg.bb.badFirebox";
                    return false;
                }
                validBlockCount++;
            }
        }
        if (validBlockCount < BBConfig.defaultMinMultiblock) {
            status = "msg.bb.tooSmall";
            return false;
        }
        return true;
    }

    @Override
    public void onAssemble(World world, List<BlockPos> blocks) {
        boilerBlockCount = 0;
        fireboxBlockCount = 0;
        pumpCount = 0;
        for (BlockPos pos : blocks) {
            if (world.getBlockState(pos).getBlock() == ModBlocks.BOILER
                    || world.getBlockState(pos).getBlock() == ModBlocks.VENT
                    || world.getBlockState(pos).getBlock() == ModBlocks.VALVE) {
                boilerBlockCount++;
            }
            if (world.getBlockState(pos).getBlock() == ModBlocks.FIREBOX
                    || world.getBlockState(pos).getBlock() == ModBlocks.HATCH
                    || world.getBlockState(pos).getBlock() == ModBlocks.BOILER_CONTROLLER) {
                fireboxBlockCount++;
            }
            if (world.getBlockState(pos).getBlock() == ModBlocks.PUMP) {
                boilerBlockCount++;
                pumpCount++;
            }
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileEntityBoilerPart) {
                ((TileEntityBoilerPart)te).setController(this);
            }
        }
        tankWater.setCapacity(1000*boilerBlockCount);
        tankSteam.setCapacity(500*boilerBlockCount);
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
        tag.setInteger("FireboxCount", fireboxBlockCount);
        tag.setInteger("PumpCount", pumpCount);
        tag.setIntArray("CurrentFuelTime", currentFuelTime);
        tag.setIntArray("MaxFuelTime", maxFuelTime);
        tag.setTag("WaterTank", tankWater.writeToNBT(new NBTTagCompound()));
        tag.setTag("SteamTank", tankSteam.writeToNBT(new NBTTagCompound()));
        tag.setTag("Inventory", inv.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        boilerBlockCount = compound.getInteger("BoilerCount");
        fireboxBlockCount = compound.getInteger("FireboxCount");
        pumpCount = compound.getInteger("PumpCount");
        tankWater.setCapacity(1000 * boilerBlockCount);
        tankSteam.setCapacity(500 * boilerBlockCount);
        currentFuelTime = compound.getIntArray("CurrentFuelTime");
        maxFuelTime = compound.getIntArray("MaxFuelTime");
        tankWater.readFromNBT(compound.getCompoundTag("WaterTank"));
        tankSteam.readFromNBT(compound.getCompoundTag("SteamTank"));
        inv.deserializeNBT(compound.getCompoundTag("Inventory"));
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

    private boolean canProcessFluid() {
        FluidStack tankDrained = tankWater.drain(2*BBConfig.steamPerBoil, false);
        int tankFilled = tankSteam.fill(new FluidStack(ModBlocks.FLUID_STEAM, BBConfig.steamPerBoil), false);
        return (tankDrained != null && tankFilled == BBConfig.steamPerBoil);
    }

    private boolean canPumpFluid() {
        double fuelPerTick = (double) BBConfig.steamPerBoil / (double) BBConfig.ticksToBoil;
        int toProcess = (int)Math.ceil(fireboxBlockCount * 3 * fuelPerTick * BBConfig.pumpMultiplier);
        FluidStack tankDrained = tankWater.drain(2*toProcess, false);
        int tankFilled = tankSteam.fill(new FluidStack(ModBlocks.FLUID_STEAM, toProcess), false);
        return (tankDrained != null && tankFilled == toProcess);
    }

    private boolean consumeFuel(int slot) {
        if (currentFuelTime[slot] <= 0) {
            ItemStack usedFuel = inv.extractItem(slot, 1, false);
            if (!usedFuel.isEmpty()) {
                int newFuelTicks = 5 * TileEntityFurnace.getItemBurnTime(usedFuel);
                maxFuelTime[slot] = newFuelTicks;
                currentFuelTime[slot] = newFuelTicks;
            } else {
                return false;
            }
        }
        currentFuelTime[slot] -= fireboxBlockCount;
        return true;
    }

    public int getField(int id) {
        switch (id) {
            case 0: return currentProcessTime;
            case 1: return PROCESS_LENGTH;
            case 2: return currentFuelTime[0];
            case 3: return maxFuelTime[0];
            case 4: return currentFuelTime[1];
            case 5: return maxFuelTime[1];
            case 6: return currentFuelTime[2];
            case 7: return maxFuelTime[2];
            default: return 0;
        }
    }

    public void setField(int id, int value) {
        switch (id) {
            case 0: currentProcessTime = value; break;
            case 2: currentFuelTime[0] = value; break;
            case 3: maxFuelTime[0] = value; break;
            case 4: currentFuelTime[1] = value; break;
            case 5: maxFuelTime[1] = value; break;
            case 6: currentFuelTime[2] = value; break;
            case 7: maxFuelTime[2] = value; break;
            default: break;
        }
    }

    public int getFieldCount() {
        return 8;
    }

    public FluidTank getTankWater() {
        return tankWater;
    }

    public FluidTank getTankSteam() {
        return tankSteam;
    }

    public ItemStackHandler getInv() {
        return inv;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        } else
            return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inv;
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) cap;
        } else {
            return super.getCapability(capability, facing);
        }
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