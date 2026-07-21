package com.drultralux.betterboilers.tile.turbine;

import com.drultralux.betterboilers.util.BBConfig;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class TurbineStage {
    public final int bladeCount;
    public final int armLength;
    public final CoilTier tier;
    public final int validArmCount;
    private final List<BlockPos> armPositions;

    public TurbineStage(int bladeCount, int armLength, CoilTier tier, int validArmCount, List<BlockPos> armPositions) {
        this.bladeCount = bladeCount;
        this.armLength = armLength;
        this.tier = tier;
        this.validArmCount = validArmCount;
        this.armPositions = armPositions;
    }

    public double captureCapacity() {
        return (double) validArmCount * armLength * BBConfig.turbineBaseCapturePerBlade;
    }

    public List<BlockPos> armPositions() {
        return armPositions;
    }
}