package com.drultralux.betterboilers.client.framework;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class GuiModular extends GuiContainer {
    protected final List<IGuiElement> elements = new ArrayList<>();

    public GuiModular(Container container, int width, int height) {
        super(container);
        this.xSize = width;
        this.ySize = height;
    }

    protected GuiModular addElement(IGuiElement e) {
        elements.add(e);
        return this;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawBackground(partialTicks, mouseX, mouseY);
        for (IGuiElement e : elements) {
            e.draw(this, partialTicks, mouseX, mouseY);
        }
    }

    protected abstract void drawBackground(float partialTicks, int mouseX, int mouseY);

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        for (IGuiElement e : elements) {
            e.drawTooltip(this, mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (IGuiElement e : elements) {
            e.onClick(this, mouseX, mouseY);
        }
    }
}
