package com.drultralux.betterboilers.tile.turbine;

import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.util.BBConfig;
import net.minecraft.block.Block;

public enum CoilTier {
    IRON(ModBlocks.COIL_IRON),
    GOLD(ModBlocks.COIL_GOLD);

    public final Block block;

    CoilTier(Block block) {
        this.block = block;
    }

    public static CoilTier fromBlock(Block block) {
        for (CoilTier tier : values()) {
            if (tier.block == block) {
                return tier;
            }
        }
        return null;
    }

    public double efficiency() {
        switch (this) {
            case IRON: return BBConfig.ironCoilEfficiency;
            case GOLD: return BBConfig.goldCoilEfficiency;
            default: return 0.0;
        }
    }
}
