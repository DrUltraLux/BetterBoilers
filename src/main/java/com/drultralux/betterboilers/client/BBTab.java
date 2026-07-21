package com.drultralux.betterboilers.client;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

public class BBTab extends CreativeTabs {
    public BBTab() {
        super(BetterBoilers.MODID);
        //setBackgroundImageName("betterboilers.png");
    }

    @Override
    public ItemStack getTabIconItem() {
        return new ItemStack(ModBlocks.BOILER_CONTROLLER);
    }
}
