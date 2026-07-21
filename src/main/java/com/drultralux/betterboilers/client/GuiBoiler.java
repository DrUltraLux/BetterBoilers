package com.drultralux.betterboilers.client;

import com.drultralux.betterboilers.BetterBoilers;
import com.drultralux.betterboilers.client.elements.ElementBar;
import com.drultralux.betterboilers.client.elements.ElementFluidTank;
import com.drultralux.betterboilers.client.framework.GuiModular;
import com.drultralux.betterboilers.container.BoilerContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiBoiler extends GuiModular {

    private static final ResourceLocation WATER_BG = loc("water_bg.png");
    private static final ResourceLocation WATER_FG = loc("water_fg.png");
    private static final ResourceLocation STEAM_BG = loc("steam_bg.png");
    private static final ResourceLocation STEAM_FG = loc("steam_fg.png");
    private static final ResourceLocation FIRE_BG = loc("fire_bg.png");
    private static final ResourceLocation FIRE_FG = loc("fire_fg.png");
    private static final ResourceLocation ARROW_BG = loc("arrow_bg.png");
    private static final ResourceLocation ARROW_FG = loc("arrow_fg.png");
    private static final ResourceLocation WATER_BAR = loc("water_bar.png");
    private static final ResourceLocation STEAM_BAR = loc("steam_bar.png");
    private static final ResourceLocation[] PUMP_ANIM = new ResourceLocation[13];
    static {
        for (int i = 0; i <= 12; i++) PUMP_ANIM[i] = loc("pump/" + i + ".png");
    }

    private static ResourceLocation loc(String path) {
        return new ResourceLocation(BetterBoilers.MODID, "textures/gui/" + path);
    }

    private final BoilerContainer boilerContainer;
    private int pumpFrame = 0;
    private long lastFrameTime = 0;

    public GuiBoiler(BoilerContainer container, InventoryPlayer playerInv) {
        super(container, 176, 166);
        this.boilerContainer = container;

        addElement(new ElementFluidTank(11, 15, 50, 24, () -> boilerContainer.boiler.tankWater, WATER_BG, WATER_FG, "water"));
        addElement(new ElementFluidTank(112, 27, 18, 40, () -> boilerContainer.boiler.tankSteam, STEAM_BG, STEAM_FG, "steam"));

        addElement(new ElementBar(11, 46, 14, 14, FIRE_BG, FIRE_FG,
                () -> boilerContainer.boiler.getField(2), () -> boilerContainer.boiler.getField(3), ElementBar.Direction.UP, null));
        addElement(new ElementBar(29, 46, 14, 14, FIRE_BG, FIRE_FG,
                () -> boilerContainer.boiler.getField(4), () -> boilerContainer.boiler.getField(5), ElementBar.Direction.UP, null));
        addElement(new ElementBar(47, 46, 14, 14, FIRE_BG, FIRE_FG,
                () -> boilerContainer.boiler.getField(6), () -> boilerContainer.boiler.getField(7), ElementBar.Direction.UP, null));
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

        BBGuiUtils.rect(WATER_BAR, guiLeft + 11, guiTop + 11, 50, 4);
        BBGuiUtils.rect(WATER_BAR, guiLeft + 11, guiTop + 39, 50, 4);
        BBGuiUtils.rect(STEAM_BAR, guiLeft + 112, guiTop + 23, 18, 4);
        BBGuiUtils.rect(STEAM_BAR, guiLeft + 112, guiTop + 67, 18, 4);

        if (boilerContainer.boiler.pumpCount == 0) {
            BBGuiUtils.rect(ARROW_BG, guiLeft + 75, guiTop + 37, 24, 17);
            BBGuiUtils.drawBarRight(ARROW_FG, guiLeft + 75, guiTop + 37, 24, 17,
                    boilerContainer.boiler.getField(0), boilerContainer.boiler.getField(1));
        } else {
            drawPumpAnimation(guiLeft + 75, guiTop + 37);
        }

        fontRenderer.drawString(I18n.format("tile.betterboilers.boiler_controller.name"), guiLeft + 8, guiTop + 2, 0xFF404040);
    }

    private void drawPumpAnimation(int x, int y) {
        long now = System.nanoTime() / 1_000_000L;
        if (lastFrameTime == 0) lastFrameTime = now;
        long elapsed = now - lastFrameTime;
        if (elapsed >= 150) {
            pumpFrame = (pumpFrame + 1) % (PUMP_ANIM.length - 1);
            lastFrameTime = now;
        }
        BBGuiUtils.rect(PUMP_ANIM[pumpFrame], x, y, 24, 17);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        String statusKey = boilerContainer.boiler.getClientStatusKey();
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