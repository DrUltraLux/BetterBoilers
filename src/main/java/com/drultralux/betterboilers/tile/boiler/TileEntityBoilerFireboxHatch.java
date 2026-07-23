package com.drultralux.betterboilers.tile.boiler;

import javax.annotation.Nullable;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

import net.minecraft.util.EnumFacing;

public class TileEntityBoilerFireboxHatch extends TileEntityFurnacePart {   // was: extends TileEntityBoilerPart

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (getController()==null) return false;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (getController()==null) return null;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return castCapabilityHandler(getController().getInv());
        } else {
            return super.getCapability(capability, facing);
        }
    }

}