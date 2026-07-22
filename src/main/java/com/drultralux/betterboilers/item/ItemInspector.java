package com.drultralux.betterboilers.item;

import com.drultralux.betterboilers.pipe.PipeNetwork;
import com.drultralux.betterboilers.pipe.TileEntityPipe;
import com.drultralux.betterboilers.pipe.energy.EnergyPipeNetwork;
import com.drultralux.betterboilers.pipe.fluid.FluidPipeNetwork;
import com.drultralux.betterboilers.pipe.heat.HeatPipeNetwork;
import com.drultralux.betterboilers.tile.boiler.TileEntityBoilerController;
import com.drultralux.betterboilers.tile.turbine.TileEntityTurbineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public class ItemInspector extends ItemBase {

    public ItemInspector() {
        super("inspector");
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (player.isSneaking()) {
            if (te instanceof TileEntityBoilerController) {
                TileEntityBoilerController controller = (TileEntityBoilerController)te;
                if (!world.isRemote) {
                    player.sendMessage(controller.errorReason);
                }
                return EnumActionResult.SUCCESS;
            }
            if (te instanceof TileEntityTurbineController) {
                TileEntityTurbineController controller = (TileEntityTurbineController)te;
                if (!world.isRemote) {
                    player.sendMessage(controller.errorReason);
                }
                return EnumActionResult.SUCCESS;
            }
            if (te instanceof TileEntityPipe) {
                if (!world.isRemote) {
                    player.sendMessage(new TextComponentString(describePipe((TileEntityPipe) te)));
                }
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    private String describePipe(TileEntityPipe pipe) {
        PipeNetwork<?> network = pipe.getNetwork();
        if (network == null) {
            return "[" + pipe.getPipeType() + " pipe] not yet assigned to a network";
        }
        StringBuilder sb = new StringBuilder("[" + pipe.getPipeType() + " pipe] network size: " + network.size()
                + ", capacity: " + network.getCapacity());
        if (network instanceof FluidPipeNetwork) {
            FluidStack fluid = ((FluidPipeNetwork) network).getFluid();
            sb.append(", contents: ").append(fluid == null ? "empty" : fluid.amount + " mB " + fluid.getFluid().getName());
        } else if (network instanceof EnergyPipeNetwork) {
            sb.append(", stored: ").append(((EnergyPipeNetwork) network).getEnergyStored()).append(" RF");
        } else if (network instanceof HeatPipeNetwork) {
            sb.append(", stored: ").append(((HeatPipeNetwork) network).getHeatStored()).append(" heat");
        }
        return sb.toString();
    }
}
