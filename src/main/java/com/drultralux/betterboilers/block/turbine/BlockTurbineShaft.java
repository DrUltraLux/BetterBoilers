package com.drultralux.betterboilers.block.turbine;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.turbine.TileEntityTurbineShaft;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

public class BlockTurbineShaft extends BlockTileEntity<TileEntityTurbineShaft> implements ITurbineBlock {

    protected String name;

    public BlockTurbineShaft() {
        super(Material.IRON, "turbine_shaft");

        setCreativeTab(BetterBoilers.creativeTab);
    }

    @Override
    public Class<TileEntityTurbineShaft> getTileEntityClass() {
        return TileEntityTurbineShaft.class;
    }

    @Override
    public TileEntityTurbineShaft createTileEntity(World world, IBlockState state) {
        return new TileEntityTurbineShaft();
    }
}
