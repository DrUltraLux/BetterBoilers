package com.drultralux.betterboilers.block.boiler;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.boiler.TileEntityBoilerFireboxHatch;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

//What does this even do?? o.O
public class BlockFireboxHatch extends BlockTileEntity<TileEntityBoilerFireboxHatch> implements IBoilerBlock, IFurnaceBlock {

    protected String name;

    public BlockFireboxHatch() {
        super(Material.ROCK, "firebox_hatch");

        setCreativeTab(BetterBoilers.creativeTab);
    }

    @Override
    public Class<TileEntityBoilerFireboxHatch> getTileEntityClass() {
        return TileEntityBoilerFireboxHatch.class;
    }

    @Override
    public TileEntityBoilerFireboxHatch createTileEntity(World world, IBlockState state) {
        return new TileEntityBoilerFireboxHatch();
    }
}
