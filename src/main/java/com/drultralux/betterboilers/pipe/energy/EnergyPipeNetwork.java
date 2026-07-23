package com.drultralux.betterboilers.pipe.energy;

import com.drultralux.betterboilers.pipe.ScalarPipeNetwork;
import com.drultralux.betterboilers.util.BBConfig;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * All the actual transfer-loop logic lives in PipeNetwork/ScalarPipeNetwork - this class only
 * supplies the energy-specific glue: which capability to look for, how big a segment is, and how
 * to call IEnergyStorage's particular method names.
 */
public class EnergyPipeNetwork extends ScalarPipeNetwork<IEnergyStorage> {

    public EnergyPipeNetwork(World world) {
        super(world);
    }

    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, getStoredAmount());
    }

    /** Reactive entry point for an external tile pushing energy directly into this network. */
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return (int) receive(maxReceive, simulate);
    }

    /** Reactive entry point for an external tile pulling energy directly out of this network. */
    public int extractEnergy(int maxExtract, boolean simulate) {
        return (int) extract(maxExtract, simulate);
    }

    @Override
    protected long getCapacityPerSegment() {
        return BBConfig.energyPipeCapacityPerSegment;
    }

    @Override
    protected int getMaxTransferPerTick() {
        return BBConfig.energyPipeMaxTransferPerTick;
    }

    @Override
    protected Capability<IEnergyStorage> getCapabilityType() {
        return CapabilityEnergy.ENERGY;
    }

    @Override
    protected long extractFromHandler(IEnergyStorage handler, long maxAmount, boolean simulate) {
        if (!handler.canExtract()) {
            return 0;
        }
        int capped = (int) Math.min(Integer.MAX_VALUE, maxAmount);
        return handler.extractEnergy(capped, simulate);
    }

    @Override
    protected long insertToHandler(IEnergyStorage handler, long maxAmount, boolean simulate) {
        if (!handler.canReceive()) {
            return 0;
        }
        int capped = (int) Math.min(Integer.MAX_VALUE, maxAmount);
        return handler.receiveEnergy(capped, simulate);
    }
}
