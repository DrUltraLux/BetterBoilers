package com.drultralux.betterboilers.client.framework;

import net.minecraft.client.gui.inventory.GuiContainer;

public interface IGuiElement {
    void draw(GuiContainer g, float partialTicks, int mouseX, int mouseY);
    void drawTooltip(GuiContainer g, int mouseX, int mouseY);
    void onClick(GuiContainer g, int mouseX, int mouseY);
}
