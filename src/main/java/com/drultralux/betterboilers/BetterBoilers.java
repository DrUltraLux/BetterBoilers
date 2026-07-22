package com.drultralux.betterboilers;

import com.drultralux.betterboilers.client.framework.BBGuiHandler;
import com.drultralux.betterboilers.container.BoilerContainer;
import com.drultralux.betterboilers.container.TurbineContainer;
import com.drultralux.betterboilers.network.BBNetwork;
import com.drultralux.betterboilers.pipe.PipeNetworkManager;
import com.drultralux.betterboilers.pipe.heat.HeatCapability;
import com.drultralux.betterboilers.util.BBConfig;
import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.client.BBTab;
import com.drultralux.betterboilers.item.ModItems;
import com.drultralux.betterboilers.proxy.CommonProxy;
import com.drultralux.betterboilers.log.BBLogTickHandler;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.oredict.OreDictionary;

@Mod(modid = BetterBoilers.MODID, name = BetterBoilers.NAME, version = BetterBoilers.VERSION)
public class BetterBoilers {
    public static final String MODID = "betterboilers";
    public static final String NAME  = "Better Boilers";
    public static final String VERSION = "1.0.0";
    public static BBConfig config;
    public static boolean hasBrass;

    @Mod.Instance(MODID)
    public static BetterBoilers instance;

    public static final BBTab creativeTab = new BBTab();

    static {
        FluidRegistry.enableUniversalBucket();
    }

    @SidedProxy(serverSide = "com.drultralux.betterboilers.proxy.CommonProxy", clientSide = "com.drultralux.betterboilers.proxy.ClientProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BBNetwork.init();
        BBLog.info("oooh, steamy! " + NAME + " is loading!");
        MinecraftForge.EVENT_BUS.register(new BBLogTickHandler());
        MinecraftForge.EVENT_BUS.register(proxy);
        MinecraftForge.EVENT_BUS.register(PipeNetworkManager.INSTANCE);
        HeatCapability.register();
        config = BBConfig.createConfig(event);

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new BBGuiHandler());
        BBGuiHandler.registerGui(new BoilerContainer.Factory());
        BBGuiHandler.registerGui(new TurbineContainer.Factory());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModItems.registerOreDict();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        hasBrass = !OreDictionary.getOres("plateBrass").isEmpty() || !OreDictionary.getOres("ingotBrass").isEmpty();
        if (hasBrass) {
            BBLog.info("Using advanced Brass recipes.");
        } else {
            BBLog.info("Reverting to default vanilla mc recipes.");
        }
    }

    @Mod.EventBusSubscriber
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            ModItems.register(event.getRegistry());
            ModBlocks.registerItemBlocks(event.getRegistry());
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            ModItems.registerModels();
            ModBlocks.registerModels();
        }

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            ModBlocks.register(event.getRegistry());
        }
    }
}