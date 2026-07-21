package com.drultralux.betterboilers.block.turbine;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.turbine.TileEntityTurbineCoil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

public class BlockTurbineCoilIron extends BlockTileEntity<TileEntityTurbineCoil> implements ITurbineBlock {

    public BlockTurbineCoilIron() {
        super(Material.IRON, "turbine_coil_iron");

        setCreativeTab(BetterBoilers.creativeTab);
    }

    @Override
    public Class<TileEntityTurbineCoil> getTileEntityClass() {
        return TileEntityTurbineCoil.class;
    }

    @Override
    public TileEntityTurbineCoil createTileEntity(World world, IBlockState state) {
        return new TileEntityTurbineCoil();
    }
}
