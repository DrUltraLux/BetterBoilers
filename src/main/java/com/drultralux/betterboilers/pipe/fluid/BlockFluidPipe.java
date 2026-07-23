package com.drultralux.betterboilers.pipe.fluid;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.pipe.TileEntityPipe;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.block.Block;

import javax.annotation.Nullable;

/**
 * Connected-pipe geometry, matching Gadgetry's own cable/pipe system exactly: a small center
 * cube plus one connection-arm submodel per side that's actually connected, computed live via
 * getActualState() rather than stored in block metadata. TileEntityFluidPipe.canConnect() (used
 * elsewhere for capability wiring already) is exactly the hook Gadgetry's TileCable.canConnect()
 * plays the same role for.
 */
public class BlockFluidPipe extends BlockTileEntity<TileEntityFluidPipe> {

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    public BlockFluidPipe() {
        super(Material.IRON, "fluid_pipe");
        setHardness(1.0f);
        setResistance(5f);
        setCreativeTab(BetterBoilers.creativeTab);
        setLightOpacity(0);
    }

    @Override
    public Class<TileEntityFluidPipe> getTileEntityClass() {
        return TileEntityFluidPipe.class;
    }

    @Nullable
    @Override
    public TileEntityFluidPipe createTileEntity(World world, IBlockState state) {
        return new TileEntityFluidPipe();
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityFluidPipe)) {
            return state;
        }
        TileEntityFluidPipe pipe = (TileEntityFluidPipe) te;
        return state
                .withProperty(NORTH, pipe.canConnect(EnumFacing.NORTH))
                .withProperty(SOUTH, pipe.canConnect(EnumFacing.SOUTH))
                .withProperty(EAST, pipe.canConnect(EnumFacing.EAST))
                .withProperty(WEST, pipe.canConnect(EnumFacing.WEST))
                .withProperty(UP, pipe.canConnect(EnumFacing.UP))
                .withProperty(DOWN, pipe.canConnect(EnumFacing.DOWN));
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        double minX = 0.3125, minY = 0.3125, minZ = 0.3125;
        double maxX = 0.6875, maxY = 0.6875, maxZ = 0.6875;
        if (te instanceof TileEntityFluidPipe) {
            TileEntityFluidPipe pipe = (TileEntityFluidPipe) te;
            if (pipe.canConnect(EnumFacing.UP)) maxY = 1.0;
            if (pipe.canConnect(EnumFacing.DOWN)) minY = 0.0;
            if (pipe.canConnect(EnumFacing.EAST)) maxX = 1.0;
            if (pipe.canConnect(EnumFacing.WEST)) minX = 0.0;
            if (pipe.canConnect(EnumFacing.SOUTH)) maxZ = 1.0;
            if (pipe.canConnect(EnumFacing.NORTH)) minZ = 0.0;
        }
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityPipe) {
            ((TileEntityPipe) te).onNeighborChanged();
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityPipe) {
            ((TileEntityPipe) te).onRemoved();
        }
        super.breakBlock(world, pos, state);
    }
}