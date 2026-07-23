package com.drultralux.betterboilers.tile.boiler;

import com.drultralux.betterboilers.tile.TileEntityMultiblockPart;

public abstract class TileEntityFurnacePart extends TileEntityMultiblockPart<TileEntityFurnaceController> {
    @Override
    protected Class<TileEntityFurnaceController> getControllerClass() {
        return TileEntityFurnaceController.class;
    }
}
