package com.drultralux.betterboilers.block.boiler;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.BlockTileEntity;
import com.drultralux.betterboilers.tile.boiler.TileEntityFurnaceController;
import com.drultralux.betterboilers.client.framework.BBGuiHandler;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockFurnaceController extends BlockTileEntity<TileEntityFurnaceController> implements IBoilerBlock, IFurnaceBlock {

    public static PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockFurnaceController() {
        super(Material.ROCK, "furnace_controller");
        setUnlocalizedName(BetterBoilers.MODID + ".furnace_controller");
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
        if (!world.isRemote && !player.isSneaking()) {
            player.openGui(BetterBoilers.instance, BBGuiHandler.getGuiId("furnace_controller"), world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public Class<TileEntityFurnaceController> getTileEntityClass() {
        return TileEntityFurnaceController.class;
    }

    @Override
    public TileEntityFurnaceController createTileEntity(World world, IBlockState state) {
        return new TileEntityFurnaceController();
    }
}