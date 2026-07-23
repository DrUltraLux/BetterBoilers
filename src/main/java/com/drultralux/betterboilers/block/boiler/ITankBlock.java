package com.drultralux.betterboilers.block.boiler;

/**
 * Marks blocks that belong to the tank multiblock (Boiler, Valve, Vent, BoilerController).
 * Deliberately narrower than IBoilerBlock, which every boiler-family block implements (firebox
 * blocks included) - using IBoilerBlock as the tank controller's flood-fill membership test lets
 * its scan wander straight through into an adjacent firebox structure, since the two multiblocks
 * are typically built touching each other. Symmetric to IFurnaceBlock on the firebox side.
 */
public interface ITankBlock {
}
