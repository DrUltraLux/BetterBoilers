package com.drultralux.betterboilers.pipe.heat;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.pipe.PipeNetwork;
import com.drultralux.betterboilers.pipe.PipeType;
import com.drultralux.betterboilers.pipe.TileEntityPipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class TileEntityHeatPipe extends TileEntityPipe {

    private final Map<EnumFacing, PipeHeatHandler> handlers = new EnumMap<>(EnumFacing.class);

    public TileEntityHeatPipe() {
        for (EnumFacing side : EnumFacing.VALUES) {
            handlers.put(side, new PipeHeatHandler(side));
        }
    }

    @Override
    public PipeType getPipeType() {
        return PipeType.HEAT;
    }

    @Override
    public HeatPipeNetwork createNetwork() {
        return new HeatPipeNetwork(world);
    }

    @Override
    public boolean canConnect(EnumFacing side) {
        if (world == null) {
            return false;
        }
        TileEntity neighbor = world.getTileEntity(getPos().offset(side));
        if (neighbor instanceof TileEntityHeatPipe) {
            return true;
        }
        return neighbor != null && HeatCapability.HEAT != null
                && neighbor.hasCapability(HeatCapability.HEAT, side.getOpposite());
    }

    private HeatPipeNetwork network() {
        PipeNetwork<?> net = getNetwork();
        return net instanceof HeatPipeNetwork ? (HeatPipeNetwork) net : null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            EnumFacing side = facing != null ? facing : EnumFacing.UP;
            PipeHeatHandler handler = handlers.get(side);
            if (handler == null) {
                BBLog.warn("Heat pipe at {} has no handler registered for side {} - this should never happen, each side is populated in the constructor", getPos(), side);
                return null;
            }
            T result = castCapabilityHandler(handler);
            return result;
        }
        return super.getCapability(capability, facing);
    }

    /** Bound to one side of this pipe segment, guarded through guarded() exactly like the fluid/energy pipe handlers. */
    private class PipeHeatHandler implements IHeatHandler {
        private final EnumFacing side;

        PipeHeatHandler(EnumFacing side) {
            this.side = side;
        }

        @Override
        public int insertHeat(int maxInsert, boolean simulate) {
            HeatPipeNetwork net = network();
            if (net == null) {
                return 0;
            }
            return guarded(() -> net.insertHeat(maxInsert, simulate), 0);
        }

        @Override
        public int extractHeat(int maxExtract, boolean simulate) {
            HeatPipeNetwork net = network();
            if (net == null) {
                return 0;
            }
            return guarded(() -> net.extractHeat(maxExtract, simulate), 0);
        }

        @Override
        public int getHeatStored() {
            HeatPipeNetwork net = network();
            return net != null ? net.getHeatStored() : 0;
        }

        @Override
        public int getMaxHeatStored() {
            HeatPipeNetwork net = network();
            return net != null ? (int) Math.min(Integer.MAX_VALUE, net.getCapacity()) : 0;
        }

        @Override
        public boolean canInsert() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return true;
        }
    }
}
