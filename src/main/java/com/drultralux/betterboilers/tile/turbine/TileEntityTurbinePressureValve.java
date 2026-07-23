package com.drultralux.betterboilers.tile.turbine;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.util.FluidAccess;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nullable;

public class TileEntityTurbinePressureValve extends TileEntityTurbinePart{

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (getController()==null) {
            BBLog.debug("Pressure Valve at {} has no controller yet - hasCapability rejected", getPos());
            return false;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (getController()==null) {
            BBLog.debug("Pressure Valve at {} has no controller yet - getCapability rejected", getPos());
            return null;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            BBLog.debug("Pressure Valve at {} exposing steam tank, currently holding {}", getPos(), getController().getTankSteam().getFluid());
            return (T) FluidAccess.insertOnly(getController().getTankSteam());
        } else {
            return super.getCapability(capability, facing);
        }
    }

}
