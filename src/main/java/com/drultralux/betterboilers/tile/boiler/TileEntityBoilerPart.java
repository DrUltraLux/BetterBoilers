package com.drultralux.betterboilers.tile.boiler;

import com.drultralux.betterboilers.tile.TileEntityMultiblockPart;

public abstract class TileEntityBoilerPart extends TileEntityMultiblockPart<TileEntityBoilerController> {
    @Override
    protected Class<TileEntityBoilerController> getControllerClass() {
        return TileEntityBoilerController.class;
    }
}