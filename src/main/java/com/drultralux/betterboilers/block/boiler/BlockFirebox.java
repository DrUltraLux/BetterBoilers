package com.drultralux.betterboilers.block.boiler;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.boiler.TileEntityFireboxBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

//What does this even do?? o.O
public class BlockFirebox extends BlockTileEntity<TileEntityFireboxBlock> implements IBoilerBlock, IFurnaceBlock {

    protected String name;

    public BlockFirebox(Material material, String name) {
        super(material, name);

        setCreativeTab(BetterBoilers.creativeTab);
    }
    @Override
    public Class<TileEntityFireboxBlock> getTileEntityClass() {
        return TileEntityFireboxBlock.class;
    }

    @Override
    public TileEntityFireboxBlock createTileEntity(World world, IBlockState state) {
        return new TileEntityFireboxBlock();
    }
}
