package com.raidclient.gui;

import com.raidclient.RaidClient;
import com.raidclient.mod.Mod;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class HUDConfigScreen extends GuiScreen {

    private Mod selectedMod = null;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Subtle dark translucent background
        drawDefaultBackground();

        // Top info bar
        RenderUtils.drawRoundedRect(width / 2.0f - 160, 10, 320, 24, 4, 0xDD1E222D);
        RenderUtils.drawOutline(width / 2.0f - 160, 10, 320, 24, 1.0f, 0x4400E5FF);
        String tip = "Arrastra los modulos para moverlos. Presiona ESC para salir.";
        int strW = fontRendererObj.getStringWidth(tip);
        fontRendererObj.drawStringWithShadow(tip, (width - strW) / 2.0f, 18, 0xFF00E5FF);

        // Render each enabled HUD mod with border
        for (Mod mod : RaidClient.INSTANCE.getModManager().getHUDMods()) {
            if (!mod.isEnabled()) continue;

            if (mod.isDragging()) {
                mod.setX(mouseX - mod.getDragX());
                mod.setY(mouseY - mod.getDragY());
            }

            // Draw bounding box
            boolean hovered = mouseX >= mod.getX() && mouseX <= mod.getX() + mod.getWidth() &&
                    mouseY >= mod.getY() && mouseY <= mod.getY() + mod.getHeight();

            int borderColor = (mod == selectedMod || hovered) ? 0xFF00E5FF : 0x88FFFFFF;
            RenderUtils.drawRect(mod.getX(), mod.getY(), mod.getWidth(), mod.getHeight(), 0x3300E5FF);
            RenderUtils.drawOutline(mod.getX(), mod.getY(), mod.getWidth(), mod.getHeight(), 1.0f, borderColor);

            mod.renderDummy(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            for (Mod mod : RaidClient.INSTANCE.getModManager().getHUDMods()) {
                if (!mod.isEnabled()) continue;

                if (mouseX >= mod.getX() && mouseX <= mod.getX() + mod.getWidth() &&
                        mouseY >= mod.getY() && mouseY <= mod.getY() + mod.getHeight()) {
                    mod.setDragging(true);
                    mod.setDragX(mouseX - mod.getX());
                    mod.setDragY(mouseY - mod.getY());
                    selectedMod = mod;
                    break;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            for (Mod mod : RaidClient.INSTANCE.getModManager().getHUDMods()) {
                mod.setDragging(false);
            }
            if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getConfigManager() != null) {
                RaidClient.INSTANCE.getConfigManager().saveConfig();
            }
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getConfigManager() != null) {
            RaidClient.INSTANCE.getConfigManager().saveConfig();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
