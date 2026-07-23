package com.drultralux.betterboilers.util;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import com.drultralux.betterboilers.BBLog;

public class FluidAccess implements IFluidTank, IFluidHandler {
    private final IFluidTank delegate;
    private boolean canExtract = false;
    private boolean canInsert = false;

    private FluidAccess(IFluidTank tank) {
        delegate = tank;
    }

    public static FluidAccess readOnly(IFluidTank tank) {
        FluidAccess result = new FluidAccess(tank);
        result.canExtract = false;
        result.canInsert = false;
        return result;
    }

    public static FluidAccess insertOnly(IFluidTank tank) {
        FluidAccess result = new FluidAccess(tank);
        result.canExtract = false;
        result.canInsert = true;
        return result;
    }

    public static FluidAccess extractOnly(IFluidTank tank) {
        FluidAccess result = new FluidAccess(tank);
        result.canExtract = true;
        result.canInsert = false;
        return result;
    }

    public static FluidAccess fullAccess(IFluidTank tank) {
        FluidAccess result = new FluidAccess(tank);
        result.canExtract = true;
        result.canInsert = true;
        return result;
    }

    @Override
    public FluidStack getFluid() {
        return delegate.getFluid();
    }

    @Override
    public int getFluidAmount() {
        return delegate.getFluidAmount();
    }

    @Override
    public int getCapacity() {
        return delegate.getCapacity();
    }

    @Override
    public FluidTankInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (canInsert) {
            int filled = delegate.fill(resource, doFill);
            BBLog.debug("FluidAccess.fill: resourceAmount={}, resourceFluid={}, doFill={}, canInsert=true, tankCapacity={}, tankAmountBefore={}, tankFluidBefore={}, filled={}",
                    resource == null ? "null" : resource.amount,
                    resource == null || resource.getFluid() == null ? "null" : resource.getFluid().getName(),
                    doFill, delegate.getCapacity(), delegate.getFluidAmount(),
                    delegate.getFluid() == null ? "null" : delegate.getFluid().getFluid().getName(),
                    filled);
            return filled;
        } else {
            BBLog.debug("FluidAccess.fill: resourceAmount={}, doFill={}, canInsert=false - rejected",
                    resource == null ? "null" : resource.amount, doFill);
            return 0;
        }
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (canExtract) {
            FluidStack drained = delegate.drain(maxDrain, doDrain);
            BBLog.debug("FluidAccess.drain(int): maxDrain={}, doDrain={}, canExtract=true, tankAmountBefore={}, tankFluidBefore={}, drainedAmount={}",
                    maxDrain, doDrain, delegate.getFluidAmount(),
                    delegate.getFluid() == null ? "null" : delegate.getFluid().getFluid().getName(),
                    drained == null ? "null" : drained.amount);
            return drained;
        } else {
            BBLog.debug("FluidAccess.drain(int): maxDrain={}, doDrain={}, canExtract=false - rejected", maxDrain, doDrain);
            return null;
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (delegate.getFluid()!=null && resource!=null && delegate.getFluid().isFluidEqual(resource)) {
            FluidStack drained = drain(resource.amount, doDrain);
            BBLog.debug("FluidAccess.drain(FluidStack): resourceAmount={}, resourceFluid={}, doDrain={}, tankFluidBefore={}, drainedAmount={}",
                    resource.amount, resource.getFluid() == null ? "null" : resource.getFluid().getName(), doDrain,
                    delegate.getFluid() == null ? "null" : delegate.getFluid().getFluid().getName(),
                    drained == null ? "null" : drained.amount);
            return drained;
        } else {
            BBLog.debug("FluidAccess.drain(FluidStack): resourceAmount={}, doDrain={}, rejected - tank empty, resource null, or fluid type mismatch (tankFluid={})",
                    resource == null ? "null" : resource.amount, doDrain,
                    delegate.getFluid() == null ? "null" : delegate.getFluid().getFluid().getName());
            return null;//XXX: As soon as Forge fixes things so that empty fluidStacks aren't null, get rid of the nulls
        }
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[] {
                new IFluidTankProperties() {

                    @Override
                    public FluidStack getContents() {
                        return delegate.getFluid();
                    }

                    @Override
                    public int getCapacity() {
                        return delegate.getCapacity();
                    }

                    @Override
                    public boolean canFill() {
                        return canInsert;
                    }

                    @Override
                    public boolean canDrain() {
                        return canExtract;
                    }

                    @Override
                    public boolean canFillFluidType(FluidStack fluidStack) {
                        if (delegate.getFluid()==null) return true;
                        if (fluidStack==null) return false;

                        return delegate.getFluid().isFluidEqual(fluidStack);
                    }

                    @Override
                    public boolean canDrainFluidType(FluidStack fluidStack) {
                        if (delegate.getFluid() == null) return false;
                        if (fluidStack == null) return true;
                        return delegate.getFluid().isFluidEqual(fluidStack);
                    }

                }
        };
    }
}