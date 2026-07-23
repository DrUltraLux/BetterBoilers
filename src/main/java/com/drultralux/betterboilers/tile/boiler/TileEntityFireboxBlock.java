package com.drultralux.betterboilers.tile.boiler;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.pipe.heat.HeatCapability;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

/**
 * Exposes the furnace controller's heat as an extractable capability directly on the Firebox
 * itself, so a heat pipe or heat sink can connect to any Firebox face (the actual bulk of the
 * structure's surface area) rather than only the Furnace Controller block. Hatch deliberately
 * does not get this - it stays item-only, matching its established role.
 */
public class TileEntityFireboxBlock extends TileEntityFurnacePart {

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (getController() == null) {
            BBLog.debug("Firebox at {} has no controller yet - hasCapability rejected", getPos());
            return false;
        }
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            return true;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (getController() == null) {
            BBLog.debug("Firebox at {} has no controller yet - getCapability rejected", getPos());
            return null;
        }
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            BBLog.debug("Firebox at {} exposing furnace heat: stored={}, capacity={}",
                    getPos(), getController().getHeat().getHeatStored(), getController().getHeat().getMaxHeatStored());
            return castCapabilityHandler(getController().getHeat());
        } else {
            return super.getCapability(capability, facing);
        }
    }
}