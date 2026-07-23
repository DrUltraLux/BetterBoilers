package com.drultralux.betterboilers.pipe.fluid;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.pipe.PipeNetwork;
import com.drultralux.betterboilers.util.BBConfig;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Fluid is the one resource type that can't reuse ScalarPipeNetwork - it needs to track which
 * fluid is currently in the pipe, not just a plain amount. Everything else (the transfer loop,
 * budget tracking, re-entrancy guard) still comes from PipeNetwork.
 */
public class FluidPipeNetwork extends PipeNetwork<IFluidHandler> {

    /** Null means empty. amount is authoritative; capacity is re-derived from member count each tick. */
    private FluidStack fluid = null;

    public FluidPipeNetwork(World world) {
        super(world);
    }

    public FluidStack getFluid() {
        return fluid;
    }

    @Override
    public long getStoredAmount() {
        return fluid != null ? fluid.amount : 0;
    }

    @Override
    protected long getCapacityPerSegment() {
        return BBConfig.fluidPipeCapacityPerSegment;
    }

    @Override
    protected int getMaxTransferPerTick() {
        return BBConfig.fluidPipeMaxTransferPerTick;
    }

    @Override
    protected Capability<IFluidHandler> getCapabilityType() {
        return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }

    @Override
    public void mergeFrom(List<PipeNetwork<?>> oldNetworks) {
        FluidStack best = null;
        for (PipeNetwork<?> old : oldNetworks) {
            if (!(old instanceof FluidPipeNetwork)) {
                BBLog.warn("FluidPipeNetwork merge encountered an incompatible network type ({}) - its contents were discarded",
                        old.getClass().getSimpleName());
                continue;
            }
            FluidStack candidate = ((FluidPipeNetwork) old).fluid;
            if (candidate == null || candidate.amount <= 0) {
                continue;
            }
            if (candidate.getFluid() == null) {
                BBLog.warn("Fluid pipe merge found a FluidStack with a null Fluid type at {} mB - discarding it", candidate.amount);
                continue;
            }
            if (best == null) {
                best = candidate.copy();
            } else if (best.isFluidEqual(candidate)) {
                best.amount += candidate.amount;
            } else if (candidate.amount > best.amount) {
                BBLog.debug("Fluid pipe merge discarded {} mB of {} in favor of a larger, incompatible stack",
                        best.amount, best.getFluid().getName());
                best = candidate.copy();
            } else {
                BBLog.debug("Fluid pipe merge discarded {} mB of {} in favor of an existing, larger stack",
                        candidate.amount, candidate.getFluid().getName());
            }
        }
        if (best != null) {
            long cap = getCapacity();
            if (best.amount > cap) {
                BBLog.debug("Fluid pipe merge produced {} mB which exceeds new capacity {} - clamping", best.amount, cap);
                best.amount = (int) cap;
            }
        }
        this.fluid = best;
    }

    /** Reactive entry point for an external tile pushing fluid directly into this network (e.g. a pump). */
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null) {
            return 0;
        }
        if (resource.getFluid() == null) {
            BBLog.warn("Fluid pipe network received a fill() call with a null Fluid type - ignoring");
            return 0;
        }
        if (resource.amount <= 0) {
            return 0;
        }
        if (fluid != null && !fluid.isFluidEqual(resource)) {
            return 0;
        }
        long room = getCapacity() - getStoredAmount();
        int accepted = (int) Math.max(0, Math.min(room, resource.amount));
        if (accepted <= 0) {
            return 0;
        }
        if (doFill) {
            if (fluid == null) {
                fluid = new FluidStack(resource.getFluid(), accepted);
            } else {
                fluid.amount += accepted;
            }
        }
        return accepted;
    }

    /** Reactive entry point for an external tile pulling fluid directly out of this network. */
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) {
            return null;
        }
        if (resource.getFluid() == null) {
            BBLog.warn("Fluid pipe network received a drain() call with a null Fluid type - ignoring");
            return null;
        }
        if (fluid == null || !fluid.isFluidEqual(resource)) {
            return null;
        }
        return drain(resource.amount, doDrain);
    }

    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (fluid == null || maxDrain <= 0) {
            return null;
        }
        int drained = Math.min(maxDrain, fluid.amount);
        FluidStack result = new FluidStack(fluid.getFluid(), drained);
        if (doDrain) {
            fluid.amount -= drained;
            if (fluid.amount <= 0) {
                fluid = null;
            }
        }
        return result;
    }

    @Override
    protected int pullFrom(IFluidHandler source, int maxAmount) {
        FluidStack simulated = source.drain(maxAmount, false);
        if (simulated == null || simulated.amount <= 0) {
            return 0;
        }
        if (simulated.getFluid() == null) {
            BBLog.warn("A neighbor fluid handler returned a FluidStack with a null Fluid type during simulate-drain - ignoring");
            return 0;
        }
        if (fluid != null && !fluid.isFluidEqual(simulated)) {
            return 0; // pipe already carrying something else - don't mix
        }
        FluidStack actuallyDrained = source.drain(simulated.amount, true);
        if (actuallyDrained == null || actuallyDrained.amount <= 0) {
            return 0;
        }
        if (fluid == null) {
            fluid = actuallyDrained.copy();
        } else {
            fluid.amount += actuallyDrained.amount;
        }
        return actuallyDrained.amount;
    }

    @Override
    protected int pushTo(IFluidHandler sink, int maxAmount) {
        if (fluid == null || fluid.amount <= 0) {
            return 0;
        }
        FluidStack toSend = new FluidStack(fluid.getFluid(), Math.min(maxAmount, fluid.amount));
        int accepted = sink.fill(toSend, false);
        if (accepted <= 0) {
            return 0;
        }
        int actuallyFilled = sink.fill(new FluidStack(fluid.getFluid(), accepted), true);
        if (actuallyFilled <= 0) {
            return 0;
        }
        fluid.amount -= actuallyFilled;
        if (fluid.amount <= 0) {
            fluid = null;
        }
        return actuallyFilled;
    }
}
