package com.drultralux.betterboilers.tile;

public interface IControlledPart<T extends TileEntityMultiblockController> {
    void setController(T controller);
    T getController();
}