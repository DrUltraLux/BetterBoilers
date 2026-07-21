package com.drultralux.betterboilers.tile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface IMultiblockPart {
    void handleNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor);
}