package com.drultralux.betterboilers.pipe;

/**
 * The three transport resource types. Pipes only ever connect to same-type neighbors -
 * a fluid pipe never joins an energy network and vice versa.
 */
public enum PipeType {
    FLUID,
    ENERGY,
    HEAT
}
