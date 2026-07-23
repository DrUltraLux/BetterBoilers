package com.drultralux.betterboilers.util;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.item.ModItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

public class BBRecipes {

    @SubscribeEvent
    public static void onRegisterRecipes(RegistryEvent.Register<IRecipe> event) {

        IForgeRegistry<IRecipe> r = event.getRegistry();

        // Crafting bench recipes

        String material = null;
        if(!OreDictionary.getOres("plateBrass").isEmpty()) {
            material = "plateBrass";
        } else if (!OreDictionary.getOres("ingotBrass").isEmpty()) {
            BBLog.warn("Oops, couldn't find brass plates! Falling back to brass ingot boiler recipe.");
            material = "ingotBrass";
        } else if (!OreDictionary.getOres("plateConstantan").isEmpty()) {
            BBLog.warn("Oh dear, couldn't find any brass. Falling back to constantan plate boiler recipe.");
            material = "plateConstantan";
        }
        if (material != null) {
            recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.BOILER, 16),
                    "bbb", "bib", "bbb",
                    'b', material,
                    'i', new ItemStack(Items.IRON_INGOT)
            ));
        } else {
            BBLog.warn("What are you even using this mod for? Falling back to iron/gold boiler recipe.");
            recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.BOILER, 16),
                    "bib", "iii", "bib",
                    'b', new ItemStack(Items.GOLD_INGOT),
                    'i', new ItemStack(Items.IRON_INGOT)
            ));
        }

        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.VALVE, 1),
                " u ", "ibi", " u ",
                'b', new ItemStack(ModBlocks.BOILER),
                'i', new ItemStack(Items.IRON_INGOT),
                'u', new ItemStack(Items.BUCKET)
        ));
        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.VENT, 1),
                " u ", "ibi", " u ",
                'b', new ItemStack(ModBlocks.BOILER),
                'i', new ItemStack(Items.IRON_INGOT),
                'u', new ItemStack(Blocks.IRON_BARS)
        ));
        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.FIREBOX, 8),
                " b ", "bib", " b ",
                'b', new ItemStack(Items.BRICK),
                'i', new ItemStack(Items.NETHERBRICK)
        ));
        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.HATCH, 1),
                " u ", "ibi", " u ",
                'b', new ItemStack(ModBlocks.FIREBOX),
                'i', new ItemStack(Items.IRON_INGOT),
                'u', new ItemStack(Blocks.IRON_BARS)
        ));
        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.BOILER_CONTROLLER, 1),
                "ibi", "bub", "ibi",
                'b', new ItemStack(ModBlocks.FIREBOX),
                'i', new ItemStack(Items.IRON_INGOT),
                'u', new ItemStack(Items.BLAZE_POWDER)
        ));
        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:items"), new ItemStack(ModItems.INSPECTOR, 1),
                "i ", "ui", "ib",
                'b', new ItemStack(Items.BOOK),
                'i', new ItemStack(Items.IRON_INGOT),
                'u', new ItemStack(Items.FLINT)
        ));

        // Firebox-only multiblock controller - takes over the recipe role the old combined
        // BOILER_CONTROLLER used to have, since it's the same physical position/purpose.
        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.FURNACE_CONTROLLER, 1),
                "ibi", "bub", "ibi",
                'b', new ItemStack(ModBlocks.FIREBOX),
                'i', new ItemStack(Items.IRON_INGOT),
                'u', new ItemStack(Items.BLAZE_POWDER)
        ));

        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.HEAT_SINK, 2),
                "iii", "ubu", "iii",
                'i', new ItemStack(Items.IRON_INGOT),
                'u', new ItemStack(Items.BLAZE_POWDER),
                'b', new ItemStack(Items.NETHERBRICK)
        ));

        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.FLUID_PIPE, 4),
                "iii",
                'i', new ItemStack(Items.IRON_INGOT)
        ));

        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.ENERGY_PIPE, 4),
                "iii",
                'i', new ItemStack(Items.GOLD_INGOT)
        ));

        recipe(r, new ShapedOreRecipe(new ResourceLocation("betterboilers:blocks"), new ItemStack(ModBlocks.HEAT_PIPE, 4),
                "ibi",
                'i', new ItemStack(Items.IRON_INGOT),
                'b', new ItemStack(Items.BLAZE_POWDER)
        ));
    }

    public static <T extends IRecipe> T recipe(IForgeRegistry<IRecipe> registry, T t) {
        t.setRegistryName(new ResourceLocation(t.getRecipeOutput().getItem().getRegistryName()+"_"+t.getRecipeOutput().getItemDamage()));
        registry.register(t);
        return t;
    }

}