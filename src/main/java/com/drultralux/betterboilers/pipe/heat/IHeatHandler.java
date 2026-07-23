package com.drultralux.betterboilers.pipe.heat;

/**
 * Vanilla Forge 1.12 has no built-in heat capability, so this mirrors IEnergyStorage's shape
 * exactly. Heat pipes expose and consume this today; the future heat system (roadmap item 2 -
 * furnaces, magma blocks, dedicated heat sources) will implement it on the producer/consumer side
 * without any change needed here.
 */
public interface IHeatHandler {

    int insertHeat(int maxInsert, boolean simulate);

    int extractHeat(int maxExtract, boolean simulate);

    int getHeatStored();

    int getMaxHeatStored();

    boolean canInsert();

    boolean canExtract();
}
