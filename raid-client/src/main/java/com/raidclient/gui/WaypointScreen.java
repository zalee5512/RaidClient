package com.raidclient.gui;

import com.raidclient.RaidClient;
import com.raidclient.mod.impl.waypoints.Waypoint;
import com.raidclient.mod.impl.waypoints.WaypointsMod;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

public class WaypointScreen extends GuiScreen {

    private GuiTextField nameField;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;

    private int selectedColor = 0xFF00E5FF; // Default Cyan
    private boolean isKoth = false;

    private static final int[] PALETTE = {
            0xFF00E5FF, // Cyan
            0xFFFFD700, // Gold / KoTH
            0xFFFF8C00, // Orange
            0xFFFF3333, // Red
            0xFF00FF7F, // Spring Green
            0xFFBA55D3  // Purple
    };

    private int scrollOffset = 0;
    private long openTime = 0;

    @Override
    public void initGui() {
        openTime = System.currentTimeMillis();
        Keyboard.enableRepeatEvents(true);

        float panelX = width / 2.0F - 280;
        float panelY = height / 2.0F - 180;

        int playerX = mc.thePlayer != null ? (int) mc.thePlayer.posX : 0;
        int playerY = mc.thePlayer != null ? (int) mc.thePlayer.posY : 64;
        int playerZ = mc.thePlayer != null ? (int) mc.thePlayer.posZ : 0;

        nameField = new GuiTextField(0, fontRendererObj, (int) panelX + 25, (int) panelY + 68, 220, 18);
        nameField.setText("Nuevo Waypoint");
        nameField.setMaxStringLength(24);

        xField = new GuiTextField(1, fontRendererObj, (int) panelX + 25, (int) panelY + 110, 68, 18);
        xField.setText(String.valueOf(playerX));
        xField.setMaxStringLength(10);

        yField = new GuiTextField(2, fontRendererObj, (int) panelX + 101, (int) panelY + 110, 68, 18);
        yField.setText(String.valueOf(playerY));
        yField.setMaxStringLength(10);

        zField = new GuiTextField(3, fontRendererObj, (int) panelX + 177, (int) panelY + 110, 68, 18);
        zField.setText(String.valueOf(playerZ));
        zField.setMaxStringLength(10);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (WaypointsMod.INSTANCE != null) {
            WaypointsMod.INSTANCE.saveWaypoints();
        }
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getConfigManager() != null) {
            RaidClient.INSTANCE.getConfigManager().saveConfig();
        }
    }

    @Override
    public void updateScreen() {
        if (nameField != null) nameField.updateCursorCounter();
        if (xField != null) xField.updateCursorCounter();
        if (yField != null) yField.updateCursorCounter();
        if (zField != null) zField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark overlay
        RenderUtils.drawRect(0, 0, width, height, 0xB00B0E14);

        float panelX = width / 2.0F - 280;
        float panelY = height / 2.0F - 180;
        float panelW = 560;
        float panelH = 360;

        // Main background panel
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 6, 0xFA141722);
        RenderUtils.drawOutline(panelX, panelY, panelW, panelH, 1.0F, 0x4400E5FF);

        // Header bar
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, 40, 6, 0xFF191E2C);
        RenderUtils.drawRect(panelX, panelY + 39, panelW, 1, 0x22FFFFFF);

        fontRendererObj.drawStringWithShadow("RAID CLIENT • WAYPOINTS & KOTHS", panelX + 15, panelY + 12, 0xFF00E5FF);
        String server = WaypointsMod.INSTANCE != null ? WaypointsMod.INSTANCE.getCurrentServerIp() : "Servidor";
        fontRendererObj.drawStringWithShadow("Servidor: " + server, panelX + 245, panelY + 13, 0x88AAAAAA);

        // Close button (X)
        float closeX = panelX + panelW - 25;
        float closeY = panelY + 12;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 15 && mouseY >= closeY && mouseY <= closeY + 15;
        fontRendererObj.drawStringWithShadow("✕", closeX, closeY, closeHovered ? 0xFFFF4444 : 0xFFAAAAAA);

        // Divider line between columns
        float divX = panelX + 270;
        RenderUtils.drawRect(divX, panelY + 45, 1, panelH - 55, 0x1AFFFFFF);

        // ═════════════════════════════════════════════════════════════════
        // LEFT COLUMN: FORMULARIO AGREGAR WAYPOINT
        // ═════════════════════════════════════════════════════════════════
        fontRendererObj.drawStringWithShadow("CREAR NUEVO WAYPOINT", panelX + 25, panelY + 48, 0xFFFFFFFF);

        // Name label & field
        fontRendererObj.drawString("Nombre:", (int) panelX + 25, (int) panelY + 58, 0xFFA0A5B5);
        nameField.drawTextBox();

        // Coordinates label & fields
        fontRendererObj.drawString("Coordenadas (X / Y / Z):", (int) panelX + 25, (int) panelY + 98, 0xFFA0A5B5);
        xField.drawTextBox();
        yField.drawTextBox();
        zField.drawTextBox();

        // "Usar Mi Posición" button
        float posBtnY = panelY + 136;
        boolean posHovered = mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= posBtnY && mouseY <= posBtnY + 20;
        RenderUtils.drawRoundedRect(panelX + 25, posBtnY, 220, 20, 3, posHovered ? 0xFF242C3F : 0xFF1C2232);
        RenderUtils.drawOutline(panelX + 25, posBtnY, 220, 20, 1.0F, 0x3300E5FF);
        fontRendererObj.drawStringWithShadow("📍 Usar Mi Posición Actual", panelX + 72, posBtnY + 6, 0xFF00E5FF);

        // Color selection
        fontRendererObj.drawString("Color del Marcador:", (int) panelX + 25, (int) panelY + 168, 0xFFA0A5B5);
        for (int i = 0; i < PALETTE.length; i++) {
            float cx = panelX + 25 + i * 36;
            float cy = panelY + 182;
            int c = PALETTE[i];
            boolean isSel = (c == selectedColor);
            RenderUtils.drawRoundedRect(cx, cy, 28, 20, 3, c);
            if (isSel) {
                RenderUtils.drawOutline(cx - 2, cy - 2, 32, 24, 1.5F, 0xFFFFFFFF);
            }
        }

        // KoTH toggle checkbox
        float kothY = panelY + 220;
        boolean kothBoxHovered = mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= kothY && mouseY <= kothY + 24;
        RenderUtils.drawRoundedRect(panelX + 25, kothY, 220, 24, 4, kothBoxHovered ? 0xFF1E2436 : 0xFF171B28);
        drawToggleCheckbox(panelX + 32, kothY + 5, isKoth);
        fontRendererObj.drawStringWithShadow("⚔ Es un KoTH (Ícono dorado)", panelX + 54, kothY + 8, isKoth ? 0xFFFFD700 : 0xFFFFFFFF);

        // Save Waypoint Button
        float saveY = panelY + 265;
        boolean saveHovered = mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= saveY && mouseY <= saveY + 36;
        RenderUtils.drawRoundedRect(panelX + 25, saveY, 220, 36, 5, saveHovered ? 0xFF33ECFF : 0xFF00E5FF);
        fontRendererObj.drawStringWithShadow("+ GUARDAR WAYPOINT", panelX + 66, saveY + 13, 0xFF0E1118);

        // Hint at bottom
        fontRendererObj.drawString("Presiona ESC o M para cerrar", (int) panelX + 60, (int) panelY + 335, 0x66AAAAAA);

        // ═════════════════════════════════════════════════════════════════
        // RIGHT COLUMN: LISTA DE WAYPOINTS EXISTENTES
        // ═════════════════════════════════════════════════════════════════
        List<Waypoint> list = WaypointsMod.INSTANCE != null ? WaypointsMod.INSTANCE.getWaypointsForCurrentServer() : null;
        int count = list != null ? list.size() : 0;
        fontRendererObj.drawStringWithShadow("WAYPOINTS GUARDADOS (" + count + ")", panelX + 285, panelY + 48, 0xFFFFFFFF);

        float itemY = panelY + 70;
        float itemW = 250;
        float itemH = 38;

        if (list == null || list.isEmpty()) {
            fontRendererObj.drawString("No hay waypoints para este servidor.", (int) panelX + 285, (int) panelY + 80, 0x66AAAAAA);
            fontRendererObj.drawString("Crea uno usando el formulario de la izquierda.", (int) panelX + 285, (int) panelY + 95, 0x44AAAAAA);
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (itemY + itemH > panelY + panelH - 15) break;

                Waypoint wp = list.get(i);
                boolean itemHovered = mouseX >= panelX + 285 && mouseX <= panelX + 285 + itemW && mouseY >= itemY && mouseY <= itemY + itemH;

                RenderUtils.drawRoundedRect(panelX + 285, itemY, itemW, itemH, 4, itemHovered ? 0xFF1C2232 : 0xFF161A26);
                RenderUtils.drawOutline(panelX + 285, itemY, itemW, itemH, 1.0F, wp.isEnabled() ? (wp.getColor() | 0x66000000) : 0x22FFFFFF);

                // Color badge
                RenderUtils.drawRoundedRect(panelX + 293, itemY + 8, 4, 22, 2, wp.getColor());

                // Title & KoTH icon
                String title = (wp.isKoth() ? "⚔ " : "📍 ") + wp.getName();
                fontRendererObj.drawStringWithShadow(title, panelX + 305, itemY + 8, wp.isEnabled() ? wp.getColor() : 0x88888888);

                // Coordinates string
                String coords = String.format("(X: %.0f, Y: %.0f, Z: %.0f)", wp.getX(), wp.getY(), wp.getZ());
                fontRendererObj.drawString(coords, (int) panelX + 305, (int) itemY + 22, 0xFF7E879B);

                // Toggle visibility switch
                float switchX = panelX + 285 + itemW - 40;
                float switchY = itemY + 12;
                drawMiniSwitch(switchX, switchY, wp.isEnabled());

                // Delete button (Red X)
                float delX = panelX + 285 + itemW - 16;
                float delY = itemY + 12;
                boolean delHovered = mouseX >= delX && mouseX <= delX + 12 && mouseY >= delY && mouseY <= delY + 14;
                fontRendererObj.drawStringWithShadow("✕", delX, delY + 2, delHovered ? 0xFFFF3333 : 0x66AAAAAA);

                itemY += itemH + 6;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawToggleCheckbox(float x, float y, boolean checked) {
        RenderUtils.drawRoundedRect(x, y, 14, 14, 3, checked ? 0xFFFFD700 : 0xFF22283A);
        if (checked) {
            fontRendererObj.drawString("✓", (int) x + 3, (int) y + 3, 0xFF000000);
        }
    }

    private void drawMiniSwitch(float x, float y, boolean enabled) {
        RenderUtils.drawRoundedRect(x, y, 20, 12, 6, enabled ? 0xFF00E5FF : 0xFF252B3B);
        float knobX = enabled ? (x + 10) : (x + 2);
        RenderUtils.drawRoundedRect(knobX, y + 2, 8, 8, 4, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float panelX = width / 2.0F - 280;
        float panelY = height / 2.0F - 180;
        float panelW = 560;
        float panelH = 360;

        // Check close button
        float closeX = panelX + panelW - 25;
        float closeY = panelY + 12;
        if (mouseX >= closeX && mouseX <= closeX + 15 && mouseY >= closeY && mouseY <= closeY + 15) {
            mc.displayGuiScreen(null);
            return;
        }

        // Focus text fields
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        xField.mouseClicked(mouseX, mouseY, mouseButton);
        yField.mouseClicked(mouseX, mouseY, mouseButton);
        zField.mouseClicked(mouseX, mouseY, mouseButton);

        // "Usar Mi Posición Actual" button click
        float posBtnY = panelY + 136;
        if (mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= posBtnY && mouseY <= posBtnY + 20) {
            if (mc.thePlayer != null) {
                xField.setText(String.valueOf((int) mc.thePlayer.posX));
                yField.setText(String.valueOf((int) mc.thePlayer.posY));
                zField.setText(String.valueOf((int) mc.thePlayer.posZ));
            }
            return;
        }

        // Color palette clicks
        for (int i = 0; i < PALETTE.length; i++) {
            float cx = panelX + 25 + i * 36;
            float cy = panelY + 182;
            if (mouseX >= cx && mouseX <= cx + 28 && mouseY >= cy && mouseY <= cy + 20) {
                selectedColor = PALETTE[i];
                if (selectedColor == 0xFFFFD700) {
                    isKoth = true; // Auto check KoTH if gold selected
                }
                return;
            }
        }

        // KoTH checkbox click
        float kothY = panelY + 220;
        if (mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= kothY && mouseY <= kothY + 24) {
            isKoth = !isKoth;
            if (isKoth && selectedColor == 0xFF00E5FF) {
                selectedColor = 0xFFFFD700; // auto switch to gold
            }
            return;
        }

        // Save Waypoint button click
        float saveY = panelY + 265;
        if (mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= saveY && mouseY <= saveY + 36) {
            saveCurrentWaypoint();
            return;
        }

        // Existing waypoints list clicks (toggle switch or delete)
        List<Waypoint> list = WaypointsMod.INSTANCE != null ? WaypointsMod.INSTANCE.getWaypointsForCurrentServer() : null;
        if (list != null) {
            float itemY = panelY + 70;
            float itemW = 250;
            float itemH = 38;

            for (int i = 0; i < list.size(); i++) {
                if (itemY + itemH > panelY + panelH - 15) break;

                Waypoint wp = list.get(i);

                // Check delete button
                float delX = panelX + 285 + itemW - 16;
                float delY = itemY + 12;
                if (mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY <= delY + 16) {
                    WaypointsMod.INSTANCE.removeWaypoint(wp.getName());
                    return;
                }

                // Check toggle switch
                float switchX = panelX + 285 + itemW - 40;
                float switchY = itemY + 12;
                if (mouseX >= switchX && mouseX <= switchX + 22 && mouseY >= switchY && mouseY <= switchY + 14) {
                    wp.setEnabled(!wp.isEnabled());
                    WaypointsMod.INSTANCE.saveWaypoints();
                    return;
                }

                itemY += itemH + 6;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void saveCurrentWaypoint() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;

        double x, y, z;
        try {
            x = Double.parseDouble(xField.getText().trim());
            y = Double.parseDouble(yField.getText().trim());
            z = Double.parseDouble(zField.getText().trim());
        } catch (NumberFormatException e) {
            return;
        }

        if (WaypointsMod.INSTANCE != null) {
            WaypointsMod.INSTANCE.addWaypoint(name, x, y, z, selectedColor, isKoth);
        }

        nameField.setText("Nuevo Waypoint");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || (keyCode == Keyboard.KEY_M && !isAnyFieldFocused() && (System.currentTimeMillis() - openTime > 200))) {
            mc.displayGuiScreen(null);
            return;
        }

        if (nameField.isFocused()) {
            nameField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (xField.isFocused()) {
            xField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (yField.isFocused()) {
            yField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (zField.isFocused()) {
            zField.textboxKeyTyped(typedChar, keyCode);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private boolean isAnyFieldFocused() {
        return (nameField != null && nameField.isFocused()) ||
                (xField != null && xField.isFocused()) ||
                (yField != null && yField.isFocused()) ||
                (zField != null && zField.isFocused());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
