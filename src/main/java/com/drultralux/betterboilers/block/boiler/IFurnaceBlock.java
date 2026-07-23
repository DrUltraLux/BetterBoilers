package com.drultralux.betterboilers.block.boiler;

/**
 * Marks blocks that belong to the firebox-only multiblock (Firebox, Hatch, FurnaceController).
 * Deliberately narrower than IBoilerBlock, which every boiler-family block implements (tank
 * blocks included) - using IBoilerBlock as the furnace controller's flood-fill membership test
 * would let its scan wander straight through into an adjacent tank structure, since the two
 * multiblocks are typically built touching each other.
 */
public interface IFurnaceBlock {
}
