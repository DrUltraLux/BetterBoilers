package com.drultralux.betterboilers.tile.turbine;

import com.drultralux.betterboilers.tile.TileEntityMultiblockPart;

public abstract class TileEntityTurbinePart extends TileEntityMultiblockPart<TileEntityTurbineController> {
    @Override
    protected Class<TileEntityTurbineController> getControllerClass() {
        return TileEntityTurbineController.class;
    }
}