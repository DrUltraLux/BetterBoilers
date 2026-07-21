package com.drultralux.betterboilers.client.elements;

import com.drultralux.betterboilers.client.BBGuiUtils;
import com.drultralux.betterboilers.client.framework.IGuiElement;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;

import java.util.function.IntSupplier;

public class ElementBar implements IGuiElement {
    public enum Direction { UP, RIGHT }

    private final int x, y, width, height;
    private final ResourceLocation bg, fg;
    private final IntSupplier value, max;
    private final Direction direction;
    private final String tooltipLabel;

    public ElementBar(int x, int y, int width, int height, ResourceLocation bg, ResourceLocation fg,
                      IntSupplier value, IntSupplier max, Direction direction, String tooltipLabel) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.bg = bg; this.fg = fg; this.value = value; this.max = max;
        this.direction = direction; this.tooltipLabel = tooltipLabel;
    }

    @Override
    public void draw(GuiContainer g, float partialTicks, int mouseX, int mouseY) {
        int left = g.getGuiLeft() + x, top = g.getGuiTop() + y;
        if (bg != null) BBGuiUtils.rect(bg, left, top, width, height);
        if (direction == Direction.UP) {
            BBGuiUtils.drawBarUp(fg, left, top, width, height, value.getAsInt(), max.getAsInt());
        } else {
            BBGuiUtils.drawBarRight(fg, left, top, width, height, value.getAsInt(), max.getAsInt());
        }
    }

    @Override
    public void drawTooltip(GuiContainer g, int mouseX, int mouseY) {
        if (tooltipLabel == null) return;
        int left = g.getGuiLeft() + x, top = g.getGuiTop() + y;
        if (mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height) {
            g.drawHoveringText(value.getAsInt() + "/" + max.getAsInt() + " " + tooltipLabel,
                    mouseX - g.getGuiLeft(), mouseY - g.getGuiTop());
        }
    }

    @Override
    public void onClick(GuiContainer g, int mouseX, int mouseY) {}
}
