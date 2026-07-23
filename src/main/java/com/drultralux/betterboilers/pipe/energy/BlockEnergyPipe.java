package com.drultralux.betterboilers.pipe.energy;

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
 * Connected-pipe geometry, matching Gadgetry's own cable/pipe system exactly - see
 * BlockFluidPipe for the full explanation, this is the same pattern for energy.
 */
public class BlockEnergyPipe extends BlockTileEntity<TileEntityEnergyPipe> {

    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");

    public BlockEnergyPipe() {
        super(Material.IRON, "energy_pipe");
        setHardness(1.0f);
        setResistance(5f);
        setCreativeTab(BetterBoilers.creativeTab);
        setLightOpacity(0);
    }

    @Override
    public Class<TileEntityEnergyPipe> getTileEntityClass() {
        return TileEntityEnergyPipe.class;
    }

    @Nullable
    @Override
    public TileEntityEnergyPipe createTileEntity(World world, IBlockState state) {
        return new TileEntityEnergyPipe();
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
        if (!(te instanceof TileEntityEnergyPipe)) {
            return state;
        }
        TileEntityEnergyPipe pipe = (TileEntityEnergyPipe) te;
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
        if (te instanceof TileEntityEnergyPipe) {
            TileEntityEnergyPipe pipe = (TileEntityEnergyPipe) te;
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