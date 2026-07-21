package com.drultralux.betterboilers.tile.boiler;

import javax.annotation.Nullable;

import com.drultralux.betterboilers.util.FluidAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import net.minecraft.util.EnumFacing;

public class TileEntityBoilerValve extends TileEntityBoilerPart{

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (getController()==null) return false; //!important
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (getController()==null) return null; //!important
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) FluidAccess.insertOnly(getController().getTankWater());
        } else {
            return super.getCapability(capability, facing);
        }
    }

}
