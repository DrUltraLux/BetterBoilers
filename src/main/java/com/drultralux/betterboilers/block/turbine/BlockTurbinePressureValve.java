package com.drultralux.betterboilers.block.turbine;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.turbine.TileEntityTurbinePressureValve;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

public class BlockTurbinePressureValve extends BlockTileEntity<TileEntityTurbinePressureValve> implements ITurbineBlock {

    protected String name;

    public BlockTurbinePressureValve() {
        super(Material.IRON, "turbine_pressure_valve");

        setCreativeTab(BetterBoilers.creativeTab);
    }

    @Override
    public Class<TileEntityTurbinePressureValve> getTileEntityClass() {
        return TileEntityTurbinePressureValve.class;
    }

    @Override
    public TileEntityTurbinePressureValve createTileEntity(World world, IBlockState state) {
        return new TileEntityTurbinePressureValve();
    }
}
