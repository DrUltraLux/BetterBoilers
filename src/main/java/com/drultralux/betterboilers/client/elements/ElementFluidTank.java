package com.drultralux.betterboilers.client.elements;

import com.drultralux.betterboilers.client.BBGuiUtils;
import com.drultralux.betterboilers.client.framework.IGuiElement;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidTank;

import java.util.function.Supplier;

public class ElementFluidTank implements IGuiElement {
    private final int x, y, width, height;
    private final Supplier<FluidTank> tank;
    private final ResourceLocation bg, fg;
    private final String label;

    public ElementFluidTank(int x, int y, int width, int height, Supplier<FluidTank> tank,
                            ResourceLocation bg, ResourceLocation fg, String label) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.tank = tank; this.bg = bg; this.fg = fg; this.label = label;
    }

    @Override
    public void draw(GuiContainer g, float partialTicks, int mouseX, int mouseY) {
        int left = g.getGuiLeft() + x, top = g.getGuiTop() + y;
        BBGuiUtils.rect(bg, left, top, width, height);
        FluidTank t = tank.get();
        if (t.getFluid() != null) {
            BBGuiUtils.drawFluidBarUp(t.getFluid().getFluid(), left, top, width, height, t.getFluidAmount(), t.getCapacity());
        }
        BBGuiUtils.rect(fg, left, top, width, height);
    }

    @Override
    public void drawTooltip(GuiContainer g, int mouseX, int mouseY) {
        int left = g.getGuiLeft() + x, top = g.getGuiTop() + y;
        if (mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height) {
            FluidTank t = tank.get();
            g.drawHoveringText(t.getFluidAmount() + "/" + t.getCapacity() + " mB " + label,
                    mouseX - g.getGuiLeft(), mouseY - g.getGuiTop());
        }
    }

    @Override
    public void onClick(GuiContainer g, int mouseX, int mouseY) {}
}