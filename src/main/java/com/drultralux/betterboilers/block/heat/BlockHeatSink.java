package com.drultralux.betterboilers.block.heat;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.heat.TileEntityHeatSink;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockHeatSink extends BlockTileEntity<TileEntityHeatSink> {

    public static PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockHeatSink() {
        super(Material.ROCK, "heat_sink");
        setHardness(1.0f);
        setResistance(5f);
        setCreativeTab(BetterBoilers.creativeTab);
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE);
    }

    public int getMetaFromState(IBlockState state) {
        return state.getValue(ACTIVE) ? 1 : 0;
    }

    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(ACTIVE, meta == 1);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        // Temporary debug readout, same idiom as BlockFurnaceController - no GUI is needed for a
        // heat sink (nothing to configure), this is purely for sanity-checking in-game.
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityHeatSink) {
                TileEntityHeatSink sink = (TileEntityHeatSink) te;
                player.sendMessage(new TextComponentString(
                        "Heat: " + sink.getHeat().getHeatStored() + " / " + sink.getHeat().getMaxHeatStored()));
            } else {
                BBLog.warn("Heat sink block at {} has no TileEntityHeatSink - this should never happen", pos);
            }
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntityHeatSink createTileEntity(World world, IBlockState state) {
        return new TileEntityHeatSink();
    }

    @Override
    public Class<TileEntityHeatSink> getTileEntityClass() {
        return TileEntityHeatSink.class;
    }
}