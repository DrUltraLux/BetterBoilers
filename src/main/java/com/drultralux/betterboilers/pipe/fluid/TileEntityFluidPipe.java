package com.drultralux.betterboilers.pipe.fluid;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.pipe.PipeNetwork;
import com.drultralux.betterboilers.pipe.PipeType;
import com.drultralux.betterboilers.pipe.TileEntityPipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class TileEntityFluidPipe extends TileEntityPipe {

    private final Map<EnumFacing, PipeFluidHandler> handlers = new EnumMap<>(EnumFacing.class);

    public TileEntityFluidPipe() {
        for (EnumFacing side : EnumFacing.VALUES) {
            handlers.put(side, new PipeFluidHandler(side));
        }
    }

    @Override
    public PipeType getPipeType() {
        return PipeType.FLUID;
    }

    @Override
    public FluidPipeNetwork createNetwork() {
        return new FluidPipeNetwork(world);
    }

    @Override
    public boolean canConnect(EnumFacing side) {
        if (world == null) {
            return false;
        }
        TileEntity neighbor = world.getTileEntity(getPos().offset(side));
        if (neighbor instanceof TileEntityFluidPipe) {
            return true;
        }
        return neighbor != null && neighbor.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
    }

    private FluidPipeNetwork network() {
        PipeNetwork<?> net = getNetwork();
        return net instanceof FluidPipeNetwork ? (FluidPipeNetwork) net : null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            EnumFacing side = facing != null ? facing : EnumFacing.UP;
            PipeFluidHandler handler = handlers.get(side);
            if (handler == null) {
                BBLog.warn("Fluid pipe at {} has no handler registered for side {} - this should never happen, each side is populated in the constructor", getPos(), side);
                return null;
            }
            T result = castCapabilityHandler(handler);
            return result;
        }
        return super.getCapability(capability, facing);
    }

    /**
     * Bound to one side of this pipe segment. Every entry point runs through guarded(), which
     * checks this tile's own re-entrancy flag before touching the shared network pool, so a
     * neighbor mod that reactively calls back into us mid-call gets refused rather than recursing.
     */
    private class PipeFluidHandler implements IFluidHandler {
        private final EnumFacing side;

        PipeFluidHandler(EnumFacing side) {
            this.side = side;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            FluidPipeNetwork net = network();
            FluidStack contents = net != null ? net.getFluid() : null;
            int capacity = net != null ? (int) Math.min(Integer.MAX_VALUE, net.getCapacity()) : 0;
            return new IFluidTankProperties[] { new PipeTankProperties(contents, capacity) };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            FluidPipeNetwork net = network();
            if (net == null) {
                return 0;
            }
            return guarded(() -> net.fill(resource, doFill), 0);
        }

        @Override
        @Nullable
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidPipeNetwork net = network();
            if (net == null) {
                return null;
            }
            return guarded(() -> net.drain(resource, doDrain), null);
        }

        @Override
        @Nullable
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidPipeNetwork net = network();
            if (net == null) {
                return null;
            }
            return guarded(() -> net.drain(maxDrain, doDrain), null);
        }
    }

    private static class PipeTankProperties implements IFluidTankProperties {
        private final FluidStack contents;
        private final int capacity;

        PipeTankProperties(FluidStack contents, int capacity) {
            this.contents = contents;
            this.capacity = capacity;
        }

        @Override
        @Nullable
        public FluidStack getContents() {
            return contents != null ? contents.copy() : null;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public boolean canFill() {
            return true;
        }

        @Override
        public boolean canDrain() {
            return true;
        }

        @Override
        public boolean canFillFluidType(FluidStack fluidStack) {
            return contents == null || fluidStack == null || contents.isFluidEqual(fluidStack);
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluidStack) {
            return contents != null && (fluidStack == null || contents.isFluidEqual(fluidStack));
        }
    }
}
