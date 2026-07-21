package com.drultralux.betterboilers.container;

import com.drultralux.betterboilers.client.GuiTurbine;
import com.drultralux.betterboilers.client.framework.IGuiFactory;
import com.drultralux.betterboilers.tile.turbine.TileEntityTurbineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TurbineContainer extends ContainerFieldSynced {

    public final TileEntityTurbineController turbine;

    public TurbineContainer(InventoryPlayer playerInv, TileEntityTurbineController turbine) {
        super(turbine);
        this.turbine = turbine;

        if (playerInv.player instanceof EntityPlayerMP) {
            turbine.resyncStatusTo((EntityPlayerMP) playerInv.player);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 87 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 145));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return turbine.getWorld().getTileEntity(turbine.getPos()) == turbine
                && playerIn.getDistanceSq(turbine.getPos().getX() + 0.5, turbine.getPos().getY() + 0.5, turbine.getPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();
            if (index < 27) {
                if (!this.mergeItemStack(stackInSlot, 27, 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.mergeItemStack(stackInSlot, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }

    public static class Factory implements IGuiFactory {
        @Override
        public Container constructContainer(EntityPlayer player, TileEntity tile) {
            return new TurbineContainer(player.inventory, (TileEntityTurbineController) tile);
        }

        @SideOnly(Side.CLIENT)
        @Override
        public Object constructGui(EntityPlayer player, TileEntity tile) {
            TurbineContainer container = new TurbineContainer(player.inventory, (TileEntityTurbineController) tile);
            return new GuiTurbine(container, player.inventory);
        }

        @Override
        public String getName() {
            return "turbine_controller";
        }
    }
}