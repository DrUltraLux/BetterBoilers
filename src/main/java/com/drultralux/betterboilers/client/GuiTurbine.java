package com.drultralux.betterboilers.client;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.client.elements.ElementBar;
import com.drultralux.betterboilers.client.elements.ElementFluidTank;
import com.drultralux.betterboilers.client.framework.GuiModular;
import com.drultralux.betterboilers.container.TurbineContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiTurbine extends GuiModular {

    private static final ResourceLocation STEAM_BG = loc("steam_bg.png");
    private static final ResourceLocation STEAM_FG = loc("steam_fg.png");
    private static final ResourceLocation ENERGY_BG = loc("energy_bg.png");
    private static final ResourceLocation ENERGY_FG = loc("energy_fg.png");
    private static final ResourceLocation STEAM_BAR = loc("steam_bar.png");

    private static ResourceLocation loc(String path) {
        return new ResourceLocation(BetterBoilers.MODID, "textures/gui/" + path);
    }

    private final TurbineContainer turbineContainer;

    public GuiTurbine(TurbineContainer container, InventoryPlayer playerInv) {
        super(container, 176, 166);
        this.turbineContainer = container;

        addElement(new ElementFluidTank(36, 26, 18, 40, () -> turbineContainer.turbine.tankSteam, STEAM_BG, STEAM_FG, "steam"));
        addElement(new ElementBar(108, 22, 18, 48, ENERGY_BG, ENERGY_FG,
                () -> turbineContainer.turbine.getField(0), () -> turbineContainer.turbine.getField(1), ElementBar.Direction.UP, "RF"));
    }

    @Override
    protected void drawBackground(float partialTicks, int mouseX, int mouseY) {
        BBGuiUtils.drawGuiPanel(guiLeft - 8, guiTop - 8, xSize + 14, ySize + 14);

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                BBGuiUtils.drawSlotBevel(guiLeft + 8 + col * 18, guiTop + 87 + row * 18);
        for (int col = 0; col < 9; col++)
            BBGuiUtils.drawSlotBevel(guiLeft + 8 + col * 18, guiTop + 145);

        BBGuiUtils.rect(STEAM_BAR, guiLeft + 36, guiTop + 22, 18, 4);
        BBGuiUtils.rect(STEAM_BAR, guiLeft + 36, guiTop + 66, 18, 4);

        String rfPerTick = turbineContainer.turbine.getField(2) + " RF/t";
        fontRenderer.drawString(rfPerTick, guiLeft, guiTop + 9, 0xFF404040);

        fontRenderer.drawString(I18n.format("tile.betterboilers.turbine_controller.name"), guiLeft + 8, guiTop + 2, 0xFF404040);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        String statusKey = turbineContainer.turbine.getClientStatusKey();
        boolean pending = "msg.bb.notYetScanned".equals(statusKey);
        boolean ok = "msg.bb.noIssue".equals(statusKey);
        String label = pending ? "Status: Scanning..." : (ok ? "Status: OK" : "Status: Error");
        int color = pending ? 0xFF808080 : (ok ? 0xFF2E8B57 : 0xFFB22222);
        fontRenderer.drawString(label, 8, 62, color);
        if (!ok) {
            int left = guiLeft + 124, top = guiTop + 2;
            if (mouseX >= left && mouseX < left + 46 && mouseY >= top && mouseY < top + 10) {
                drawHoveringText(I18n.format(statusKey), mouseX - guiLeft, mouseY - guiTop);
            }
        }
    }
}