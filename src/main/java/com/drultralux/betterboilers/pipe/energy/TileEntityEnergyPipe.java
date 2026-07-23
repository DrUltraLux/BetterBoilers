package com.drultralux.betterboilers.pipe.energy;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.pipe.PipeNetwork;
import com.drultralux.betterboilers.pipe.PipeType;
import com.drultralux.betterboilers.pipe.TileEntityPipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class TileEntityEnergyPipe extends TileEntityPipe {

    private final Map<EnumFacing, PipeEnergyHandler> handlers = new EnumMap<>(EnumFacing.class);

    public TileEntityEnergyPipe() {
        for (EnumFacing side : EnumFacing.VALUES) {
            handlers.put(side, new PipeEnergyHandler(side));
        }
    }

    @Override
    public PipeType getPipeType() {
        return PipeType.ENERGY;
    }

    @Override
    public EnergyPipeNetwork createNetwork() {
        return new EnergyPipeNetwork(world);
    }

    @Override
    public boolean canConnect(EnumFacing side) {
        if (world == null) {
            return false;
        }
        TileEntity neighbor = world.getTileEntity(getPos().offset(side));
        if (neighbor instanceof TileEntityEnergyPipe) {
            return true;
        }
        return neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite());
    }

    private EnergyPipeNetwork network() {
        PipeNetwork<?> net = getNetwork();
        return net instanceof EnergyPipeNetwork ? (EnergyPipeNetwork) net : null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            EnumFacing side = facing != null ? facing : EnumFacing.UP;
            PipeEnergyHandler handler = handlers.get(side);
            if (handler == null) {
                BBLog.warn("Energy pipe at {} has no handler registered for side {} - this should never happen, each side is populated in the constructor", getPos(), side);
                return null;
            }
            T result = castCapabilityHandler(handler);
            return result;
        }
        return super.getCapability(capability, facing);
    }

    /** Bound to one side of this pipe segment, guarded through guarded() exactly like the fluid pipe's handler. */
    private class PipeEnergyHandler implements IEnergyStorage {
        private final EnumFacing side;

        PipeEnergyHandler(EnumFacing side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            EnergyPipeNetwork net = network();
            if (net == null) {
                return 0;
            }
            return guarded(() -> net.receiveEnergy(maxReceive, simulate), 0);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            EnergyPipeNetwork net = network();
            if (net == null) {
                return 0;
            }
            return guarded(() -> net.extractEnergy(maxExtract, simulate), 0);
        }

        @Override
        public int getEnergyStored() {
            EnergyPipeNetwork net = network();
            return net != null ? net.getEnergyStored() : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            EnergyPipeNetwork net = network();
            return net != null ? (int) Math.min(Integer.MAX_VALUE, net.getCapacity()) : 0;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
