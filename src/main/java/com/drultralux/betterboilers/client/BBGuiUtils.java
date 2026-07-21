package com.drultralux.betterboilers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import org.lwjgl.opengl.GL11;

public final class BBGuiUtils {
    private BBGuiUtils() {}

    public static void rect(ResourceLocation texture, int left, int top, int width, int height) {
        rect(texture, left, top, width, height, 0f, 0f, 1f, 1f);
    }

    public static void rect(ResourceLocation texture, int left, int top, int width, int height, float u1, float v1, float u2, float v2) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(left, top + height, 0.0D).tex(u1, v2).endVertex();
        buffer.pos(left + width, top + height, 0.0D).tex(u2, v2).endVertex();
        buffer.pos(left + width, top, 0.0D).tex(u2, v1).endVertex();
        buffer.pos(left, top, 0.0D).tex(u1, v1).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    public static void solidRect(int left, int top, int width, int height, int argb) {
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        float a = (argb >> 24 & 255) / 255.0F;
        float r = (argb >> 16 & 255) / 255.0F;
        float g = (argb >> 8 & 255) / 255.0F;
        float b = (argb & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.color(r, g, b, a);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(left, top + height, 0.0D).endVertex();
        buffer.pos(left + width, top + height, 0.0D).endVertex();
        buffer.pos(left + width, top, 0.0D).endVertex();
        buffer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Vanilla-style beveled outer window panel (matches Concrete's drawGuiPanel exactly). */
    public static void drawGuiPanel(int x, int y, int width, int height) {
        int panel = 0xFFC6C6C6;
        int shadow = multiplyColor(panel, 0.50f);
        int hilight = multiplyColor(panel, 1.25f);
        int outline = 0xFF000000;

        solidRect(x + 3, y + 3, width - 6, height - 6, panel);
        solidRect(x + 2, y + 1, width - 4, 2, hilight);
        solidRect(x + 2, y + height - 3, width - 4, 2, shadow);
        solidRect(x + 1, y + 2, 2, height - 4, hilight);
        solidRect(x + width - 3, y + 2, 2, height - 4, shadow);
        solidRect(x + width - 3, y + 2, 1, 1, panel);
        solidRect(x + 2, y + height - 3, 1, 1, panel);
        solidRect(x + 3, y + 3, 1, 1, hilight);
        solidRect(x + width - 4, y + height - 4, 1, 1, shadow);
        solidRect(x + 2, y, width - 4, 1, outline);
        solidRect(x, y + 2, 1, height - 4, outline);
        solidRect(x + width - 1, y + 2, 1, height - 4, outline);
        solidRect(x + 2, y + height - 1, width - 4, 1, outline);
        solidRect(x + 1, y + 1, 1, 1, outline);
        solidRect(x + 1, y + height - 2, 1, 1, outline);
        solidRect(x + width - 2, y + 1, 1, 1, outline);
        solidRect(x + width - 2, y + height - 2, 1, 1, outline);
    }

    /** Recessed 18x18 item-slot bevel (matches Concrete's per-slot drawBeveledPanel). */
    public static void drawSlotBevel(int slotX, int slotY) {
        int lo = colorAtOpacity(0x000000, 0.72f);
        int bg = colorAtOpacity(0x000000, 0.29f);
        int hi = colorAtOpacity(0xFFFFFF, 1.0f);
        int x = slotX - 1;
        int y = slotY - 1;
        int width = 18;
        int height = 18;
        solidRect(x, y, width, height, bg);
        solidRect(x, y, width - 1, 1, lo);
        solidRect(x, y + 1, 1, height - 2, lo);
        solidRect(x + width - 1, y + 1, 1, height - 1, hi);
        solidRect(x + 1, y + height - 1, width - 1, 1, hi);
    }

    /** Vertical (bottom-up fill) fluid bar, tiled in 16x16 segments like the original. */
    public static void drawFluidBarUp(Fluid fluid, int x, int y, int width, int height, int fluidAmount, int capacity) {
        if (fluid == null || capacity <= 0 || fluidAmount <= 0) return;
        float percent = Math.max(0f, Math.min(1f, fluidAmount / (float) capacity));
        int barSize = (int) (height * percent);
        if (barSize <= 0) return;

        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluid.getStill().toString());
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        int bottom = y + height;
        int verticalSegments = barSize / 16;
        int horizontalSegments = width / 16;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        for (int dY = 0; dY < verticalSegments; dY++) {
            for (int dX = 0; dX < horizontalSegments; dX++) {
                quad(buffer, sprite, x + dX * 16, bottom - (dY + 1) * 16, 16, 16);
            }
            int rem = width % 16;
            if (rem > 0) quad(buffer, sprite, x + horizontalSegments * 16, bottom - (dY + 1) * 16, rem, 16);
        }
        int remH = barSize % 16;
        if (remH > 0) {
            for (int dX = 0; dX < horizontalSegments; dX++) {
                quad(buffer, sprite, x + dX * 16, bottom - verticalSegments * 16 - remH, 16, remH);
            }
            int rem = width % 16;
            if (rem > 0) quad(buffer, sprite, x + horizontalSegments * 16, bottom - verticalSegments * 16 - remH, rem, remH);
        }
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    private static void quad(BufferBuilder buffer, TextureAtlasSprite sprite, int x, int y, int w, int h) {
        float u1 = sprite.getInterpolatedU(0);
        float u2 = sprite.getInterpolatedU(16.0 * w / 16.0);
        float v1 = sprite.getInterpolatedV(0);
        float v2 = sprite.getInterpolatedV(16.0 * h / 16.0);
        buffer.pos(x, y + h, 0.0D).tex(u1, v2).endVertex();
        buffer.pos(x + w, y + h, 0.0D).tex(u2, v2).endVertex();
        buffer.pos(x + w, y, 0.0D).tex(u2, v1).endVertex();
        buffer.pos(x, y, 0.0D).tex(u1, v1).endVertex();
    }

    /** Horizontal (left-to-right) progress bar overlay, e.g. the boil-progress arrow. */
    public static void drawBarRight(ResourceLocation bar, int x, int y, int width, int height, int value, int max) {
        if (max <= 0) return;
        float percent = Math.max(0f, Math.min(1f, value / (float) max));
        int barSize = (int) (width * percent);
        if (barSize <= 0) return;
        rect(bar, x, y, barSize, height, 0f, 0f, percent, 1f);
    }

    /** Vertical bottom-up progress bar overlay, e.g. fuel-burn indicators. */
    public static void drawBarUp(ResourceLocation bar, int x, int y, int width, int height, int value, int max) {
        if (max <= 0) return;
        float percent = Math.max(0f, Math.min(1f, value / (float) max));
        int barSize = (int) (height * percent);
        if (barSize <= 0) return;
        int top = y + height - barSize;
        rect(bar, x, top, width, barSize, 0f, 1f - percent, 1f, 1f);
    }

    public static int colorAtOpacity(int rgb, float opacity) {
        if (opacity < 0f) opacity = 0f;
        if (opacity > 1f) opacity = 1f;
        int a = (int) (opacity * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int multiplyColor(int color, float amount) {
        int a = color & 0xFF000000;
        float r = Math.min(((color >> 16) & 255) / 255.0F * amount, 1.0f);
        float g = Math.min(((color >> 8) & 255) / 255.0F * amount, 1.0f);
        float b = Math.min((color & 255) / 255.0F * amount, 1.0f);
        return a | ((int)(r*255) << 16) | ((int)(g*255) << 8) | (int)(b*255);
    }
}