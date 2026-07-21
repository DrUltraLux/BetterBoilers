package com.drultralux.betterboilers.block.turbine;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.turbine.TileEntityTurbineBlade;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

public class BlockTurbineBlade extends BlockTileEntity<TileEntityTurbineBlade> implements ITurbineBlock {

    public BlockTurbineBlade() {
        super(Material.IRON, "turbine_blade");

        setCreativeTab(BetterBoilers.creativeTab);
    }

    @Override
    public Class<TileEntityTurbineBlade> getTileEntityClass() {
        return TileEntityTurbineBlade.class;
    }

    @Override
    public TileEntityTurbineBlade createTileEntity(World world, IBlockState state) {
        return new TileEntityTurbineBlade();
    }
}