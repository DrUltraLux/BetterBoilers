package com.drultralux.betterboilers.block.turbine;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.turbine.TileEntityTurbineCasing;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

public class BlockTurbineCasing extends BlockTileEntity<TileEntityTurbineCasing> implements ITurbineBlock {

    protected String name;

    public BlockTurbineCasing() {
        super(Material.IRON, "turbine_casing");

        setCreativeTab(BetterBoilers.creativeTab);
    }

    @Override
    public Class<TileEntityTurbineCasing> getTileEntityClass() {
        return TileEntityTurbineCasing.class;
    }

    @Override
    public TileEntityTurbineCasing createTileEntity(World world, IBlockState state) {
        return new TileEntityTurbineCasing();
    }
}
