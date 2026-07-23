package com.drultralux.betterboilers.util;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * IFluidHandler capability which combines two tanks into a single interface - waterTank as the
 * fillable input (e.g. for a controller face that should behave like its own Valve), steamTank as
 * the drainable output (behaving like its own Vent). fill()/drain() previously always returned
 * 0/null unconditionally regardless of fluid type, silently rejecting every external pipe
 * connection to the controller block itself - this was the actual bug behind "water won't go
 * in," independent of which pipe mod was doing the pushing.
 */
public class ReadableDoubleTank implements IFluidHandler {
    private final FluidTank waterTank;
    private final FluidTank steamTank;

    public ReadableDoubleTank(FluidTank waterTank, FluidTank steamTank) {
        this.waterTank = Preconditions.checkNotNull(waterTank);
        this.steamTank = Preconditions.checkNotNull(steamTank);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return FluidTankProperties.convert(new FluidTankInfo[]{waterTank.getInfo(), steamTank.getInfo()});
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        return waterTank.fill(resource, doFill);
    }

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) {
            return null;
        }
        if (steamTank.getFluid() == null || !steamTank.getFluid().isFluidEqual(resource)) {
            return null;
        }
        return steamTank.drain(resource.amount, doDrain);
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0 || steamTank.getFluidAmount() <= 0) {
            return null;
        }
        return steamTank.drain(maxDrain, doDrain);
    }
}