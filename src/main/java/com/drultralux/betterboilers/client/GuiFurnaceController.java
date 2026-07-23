package com.drultralux.betterboilers.client;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.client.elements.ElementBar;
import com.drultralux.betterboilers.client.framework.GuiModular;
import com.drultralux.betterboilers.container.FurnaceContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiFurnaceController extends GuiModular {

    private static final ResourceLocation FIRE_BG = loc("fire_bg.png");
    private static final ResourceLocation FIRE_FG = loc("fire_fg.png");
    private static final ResourceLocation HEAT_BG = loc("energy_bg.png");
    private static final ResourceLocation HEAT_FG = loc("energy_fg.png");

    private static ResourceLocation loc(String path) {
        return new ResourceLocation(BetterBoilers.MODID, "textures/gui/" + path);
    }

    private final FurnaceContainer furnaceContainer;

    public GuiFurnaceController(FurnaceContainer container, InventoryPlayer playerInv) {
        super(container, 176, 166);
        this.furnaceContainer = container;

        addElement(new ElementBar(11, 46, 14, 14, FIRE_BG, FIRE_FG,
                () -> furnaceContainer.furnace.getField(2), () -> furnaceContainer.furnace.getField(3), ElementBar.Direction.UP, null));
        addElement(new ElementBar(29, 46, 14, 14, FIRE_BG, FIRE_FG,
                () -> furnaceContainer.furnace.getField(4), () -> furnaceContainer.furnace.getField(5), ElementBar.Direction.UP, null));
        addElement(new ElementBar(47, 46, 14, 14, FIRE_BG, FIRE_FG,
                () -> furnaceContainer.furnace.getField(6), () -> furnaceContainer.furnace.getField(7), ElementBar.Direction.UP, null));

        addElement(new ElementBar(108, 22, 18, 48, HEAT_BG, HEAT_FG,
                () -> furnaceContainer.furnace.getField(0), () -> furnaceContainer.furnace.getField(1), ElementBar.Direction.UP, "Heat"));
    }

    @Override
    protected void drawBackground(float partialTicks, int mouseX, int mouseY) {
        BBGuiUtils.drawGuiPanel(guiLeft - 8, guiTop - 8, xSize + 14, ySize + 14);

        for (int i = 0; i < 3; i++) BBGuiUtils.drawSlotBevel(guiLeft + 9 + i * 18, guiTop + 64);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                BBGuiUtils.drawSlotBevel(guiLeft + 8 + col * 18, guiTop + 87 + row * 18);
        for (int col = 0; col < 9; col++)
            BBGuiUtils.drawSlotBevel(guiLeft + 8 + col * 18, guiTop + 145);

        String heatPerTick = furnaceContainer.furnace.getField(8) + " Heat/t";
        fontRenderer.drawString(heatPerTick, guiLeft, guiTop + 9, 0xFF404040);
        fontRenderer.drawString(I18n.format("tile.betterboilers.furnace_controller.name"), guiLeft + 8, guiTop + 2, 0xFF404040);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        String statusKey = furnaceContainer.furnace.getClientStatusKey();
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