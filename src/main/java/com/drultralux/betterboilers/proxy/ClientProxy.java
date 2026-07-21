package com.drultralux.betterboilers.proxy;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.network.BBNetwork;
import com.drultralux.betterboilers.tile.TileEntityMultiblockController;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        //ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDistiller.class, new RenderDistiller());

        BBNetwork.registerClientHandler("controller_status", (payload) -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.world == null) return;
            BlockPos pos = new BlockPos(
                    payload.getInteger("x"), payload.getInteger("y"), payload.getInteger("z"));
            net.minecraft.tileentity.TileEntity te = mc.world.getTileEntity(pos);
            if (te instanceof com.drultralux.betterboilers.tile.TileEntityMultiblockController) {
                ((TileEntityMultiblockController) te).setClientStatusKeyFromNetwork(payload.getString("key"));
            }
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerItemRenderer(Item item, int meta, String id) {
        ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(new ResourceLocation(BetterBoilers.MODID, id), "inventory"));
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(com.drultralux.betterboilers.block.ModBlocks.FLUID_STEAM.getStill());
        event.getMap().registerSprite(com.drultralux.betterboilers.block.ModBlocks.FLUID_STEAM.getFlowing());
    }
}
