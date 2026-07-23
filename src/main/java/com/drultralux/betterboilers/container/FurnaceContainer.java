package com.drultralux.betterboilers.container;

import com.drultralux.betterboilers.client.GuiFurnaceController;
import com.drultralux.betterboilers.client.framework.IGuiFactory;
import com.drultralux.betterboilers.tile.boiler.TileEntityFurnaceController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;

public class FurnaceContainer extends ContainerFieldSynced {

    public final TileEntityFurnaceController furnace;

    public FurnaceContainer(InventoryPlayer playerInv, TileEntityFurnaceController furnace) {
        super(furnace);
        this.furnace = furnace;

        if (playerInv.player instanceof EntityPlayerMP) {
            furnace.resyncStatusTo((EntityPlayerMP) playerInv.player);
        }

        for (int i = 0; i < 3; i++) {
            addSlotToContainer(new SlotItemHandler(furnace.getInv(), i, 9 + i * 18, 64));
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
        return furnace.getWorld().getTileEntity(furnace.getPos()) == furnace
                && playerIn.getDistanceSq(furnace.getPos().getX() + 0.5, furnace.getPos().getY() + 0.5, furnace.getPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();
            if (index < 3) {
                if (!this.mergeItemStack(stackInSlot, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.mergeItemStack(stackInSlot, 0, 3, false)) {
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
            return new FurnaceContainer(player.inventory, (TileEntityFurnaceController) tile);
        }

        @SideOnly(Side.CLIENT)
        @Override
        public Object constructGui(EntityPlayer player, TileEntity tile) {
            FurnaceContainer container = new FurnaceContainer(player.inventory, (TileEntityFurnaceController) tile);
            return new GuiFurnaceController(container, player.inventory);
        }

        @Override
        public String getName() {
            return "furnace_controller";
        }
    }
}
