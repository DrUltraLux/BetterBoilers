package com.drultralux.betterboilers.tile.boiler;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.block.boiler.BlockFurnaceController;
import com.drultralux.betterboilers.block.boiler.IFurnaceBlock;
import com.drultralux.betterboilers.network.BBNetwork;
import com.drultralux.betterboilers.pipe.heat.HeatCapability;
import com.drultralux.betterboilers.pipe.heat.HeatStorage;
import com.drultralux.betterboilers.tile.IControlledPart;
import com.drultralux.betterboilers.tile.IFieldProvider;
import com.drultralux.betterboilers.tile.TileEntityMultiblockController;
import com.drultralux.betterboilers.util.BBConfig;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Firebox-row multiblock controller. Owns the fuel inventory, burn-time tracking, and firebox
 * animation state carried over from the old (now tank-only) TileEntityBoilerController, but no
 * longer touches water/steam directly - it only accumulates heat into an IHeatHandler that a
 * neighboring heat pipe or heat sink block can extract from. Getting that heat into a tank is
 * now entirely the pipe network's job, not this controller's.
 */
public class TileEntityFurnaceController extends TileEntityMultiblockController implements ITickable, IFieldProvider, IControlledPart<TileEntityFurnaceController> {

    public ItemStackHandler inv;
    private HeatStorage heat;
    private int fireboxBlockCount = 0;

    private int[] currentFuelTime = new int[3];
    private int[] maxFuelTime = new int[3];
    private int lastHeatGeneratedPerTick = 0;

    @Override
    protected int getMaxBlocksPerMultiblock() {
        return BBConfig.defaultMaxMultiblock;
    }

    public TileEntityFurnaceController getController() {
        return this;
    }

    public void setController(TileEntityFurnaceController controller) {
    }

    public TileEntityFurnaceController() {
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

        this.heat = new HeatStorage(0);
    }

    @Override
    public void update() {
        tickScan((w, p) -> w.getBlockState(p).getBlock() instanceof IFurnaceBlock, this::isValid);

        if (!world.isRemote) {
            int activeSlots = 0;
            if (consumeFuel(0)) activeSlots++;
            if (consumeFuel(1)) activeSlots++;
            if (consumeFuel(2)) activeSlots++;

            if (activeSlots > 0) {
                int generated = activeSlots * fireboxBlockCount * BBConfig.heatPerFireboxBlockTick;
                int accepted = heat.generateHeat(generated);
                lastHeatGeneratedPerTick = accepted;
                if (accepted < generated) {
                    BBLog.debug("Furnace controller at {} generated {} heat but only had room for {} - the rest was lost. Add more heat pipes/sinks to draw it off faster.",
                            getPos(), generated, accepted);
                }
            } else {
                lastHeatGeneratedPerTick = 0;
            }
            markDirty();
        }
    }

    public boolean isValid(World world, List<BlockPos> blocks) {
        int minY = 255;
        int fireboxCount = 0;
        for (BlockPos pos : blocks) minY = Math.min(pos.getY(), minY);
        if (this.pos.getY() != minY) {
            status = "msg.bb.badFurnaceController";
            return false;
        }

        for (BlockPos pos : blocks) {
            if (world.getTileEntity(pos) instanceof TileEntityMultiblockController) {
                if (pos != this.getPos()) {
                    status = "msg.bb.tooManyControllers";
                    return false;
                }
            }
            if (world.getBlockState(pos).getBlock() == ModBlocks.FIREBOX) {
                fireboxCount++;
            }
        }
        if (fireboxCount < 1) {
            status = "msg.bb.needsFirebox";
            return false;
        }
        return true;
    }

    @Override
    public void onAssemble(World world, List<BlockPos> blocks) {
        fireboxBlockCount = 0;
        for (BlockPos pos : blocks) {
            if (world.getBlockState(pos).getBlock() == ModBlocks.FIREBOX
                    || world.getBlockState(pos).getBlock() == ModBlocks.HATCH
                    || world.getBlockState(pos).getBlock() == ModBlocks.FURNACE_CONTROLLER) {
                fireboxBlockCount++;
            }
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityFurnacePart) {
                ((TileEntityFurnacePart) te).setController(this);
            }
        }
        heat.setCapacity(fireboxBlockCount * BBConfig.furnaceHeatCapacityPerBlock);
        heat.setMaxExtract(fireboxBlockCount * BBConfig.heatExtractPerTickPerBlock);
        heat.setMaxInsert(0); // the furnace only ever exports heat - it never accepts an external push
        markDirty();
    }

    @Override
    public void onDisassemble(World world, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityFurnacePart) {
                ((TileEntityFurnacePart) te).setController(null);
            }
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound tag = super.writeToNBT(compound);
        tag.setInteger("FireboxCount", fireboxBlockCount);
        tag.setIntArray("CurrentFuelTime", currentFuelTime);
        tag.setIntArray("MaxFuelTime", maxFuelTime);
        tag.setInteger("HeatStored", heat.getHeatStored());
        tag.setInteger("HeatCapacity", heat.getMaxHeatStored());
        tag.setTag("Inventory", inv.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        fireboxBlockCount = compound.getInteger("FireboxCount");

        int[] loadedCurrent = compound.getIntArray("CurrentFuelTime");
        if (loadedCurrent.length == 3) {
            currentFuelTime = loadedCurrent;
        } else {
            BBLog.warn("Furnace controller at {} had a CurrentFuelTime NBT array of length {} (expected 3) - resetting to defaults", getPos(), loadedCurrent.length);
            currentFuelTime = new int[3];
        }

        int[] loadedMax = compound.getIntArray("MaxFuelTime");
        if (loadedMax.length == 3) {
            maxFuelTime = loadedMax;
        } else {
            BBLog.warn("Furnace controller at {} had a MaxFuelTime NBT array of length {} (expected 3) - resetting to defaults", getPos(), loadedMax.length);
            maxFuelTime = new int[3];
        }

        int capacity = compound.getInteger("HeatCapacity");
        int stored = compound.getInteger("HeatStored");
        heat = new HeatStorage(capacity, capacity, capacity, stored);
        heat.setMaxExtract(fireboxBlockCount * BBConfig.heatExtractPerTickPerBlock);
        heat.setMaxInsert(0);

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
        WorldServer ws = (WorldServer) getWorld();
        Chunk c = getWorld().getChunkFromBlockCoords(getPos());
        SPacketUpdateTileEntity packet = new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
        for (EntityPlayerMP player : getWorld().getPlayers(EntityPlayerMP.class, Predicates.alwaysTrue())) {
            if (ws.getPlayerChunkMap().isPlayerWatchingChunk(player, c.x, c.z)) {
                player.connection.sendPacket(packet);
            }
        }
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

    @Override
    public int getField(int id) {
        switch (id) {
            case 0: return heat.getHeatStored();
            case 1: return heat.getMaxHeatStored();
            case 2: return currentFuelTime[0];
            case 3: return maxFuelTime[0];
            case 4: return currentFuelTime[1];
            case 5: return maxFuelTime[1];
            case 6: return currentFuelTime[2];
            case 7: return maxFuelTime[2];
            case 8: return lastHeatGeneratedPerTick;
            default: return 0;
        }
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 2: currentFuelTime[0] = value; break;
            case 3: maxFuelTime[0] = value; break;
            case 4: currentFuelTime[1] = value; break;
            case 5: maxFuelTime[1] = value; break;
            case 6: currentFuelTime[2] = value; break;
            case 7: maxFuelTime[2] = value; break;
            case 8: lastHeatGeneratedPerTick = value; break;
            default: break;
        }
    }

    @Override
    public int getFieldCount() {
        return 9;
    }

    public ItemStackHandler getInv() {
        return inv;
    }

    public HeatStorage getHeat() {
        return heat;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return castCapabilityHandler(inv);
        }
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            BBLog.debug("Furnace Controller at {} exposing heat directly: stored={}, capacity={}",
                    getPos(), heat.getHeatStored(), heat.getMaxHeatStored());
            return castCapabilityHandler(heat);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        if (oldState.getBlock() == newState.getBlock()) return false;
        else return super.shouldRefresh(world, pos, oldState, newState);
    }

    @Override
    public void setControllerStatus(ControllerStatus state, String status) {
        errorReason = new TextComponentTranslation(status);
        if (state == ControllerStatus.ERRORED) {
            world.setBlockState(this.getPos(), ModBlocks.FURNACE_CONTROLLER.getDefaultState().withProperty(BlockFurnaceController.ACTIVE, false));
        } else {
            world.setBlockState(this.getPos(), ModBlocks.FURNACE_CONTROLLER.getDefaultState().withProperty(BlockFurnaceController.ACTIVE, true));
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