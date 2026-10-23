package com.raidclient.gui;

import com.raidclient.RaidClient;
import com.raidclient.mod.impl.AutoTextMod;
import com.raidclient.mod.impl.autotext.AutoTextMacro;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

public class AutoTextScreen extends GuiScreen {

    private GuiTextField commandField;
    private int newMacroKey = Keyboard.KEY_NONE;
    private boolean listeningForNewKey = false;
    private AutoTextMacro listeningMacro = null;

    private int scrollOffset = 0;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        float panelX = width / 2.0F - 280;
        float panelY = height / 2.0F - 180;

        commandField = new GuiTextField(0, fontRendererObj, (int) panelX + 25, (int) panelY + 130, 220, 20);
        commandField.setText("/f home");
        commandField.setMaxStringLength(100);
        commandField.setFocused(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        listeningForNewKey = false;
        listeningMacro = null;
        if (AutoTextMod.INSTANCE != null) {
            AutoTextMod.INSTANCE.saveMacros();
        }
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getConfigManager() != null) {
            RaidClient.INSTANCE.getConfigManager().saveConfig();
        }
    }

    @Override
    public void updateScreen() {
        if (commandField != null) {
            commandField.updateCursorCounter();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                if (AutoTextMod.INSTANCE != null) {
                    int maxScroll = Math.max(0, AutoTextMod.INSTANCE.getMacros().size() - 5);
                    scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark translucent overlay
        RenderUtils.drawRect(0, 0, width, height, 0xB00B0E14);

        float panelX = width / 2.0F - 280;
        float panelY = height / 2.0F - 180;
        float panelW = 560;
        float panelH = 360;

        // Main background panel
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 6, 0xFA141722);
        RenderUtils.drawOutline(panelX, panelY, panelW, panelH, 1.0F, 0x4400E5FF);

        // Header
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, 40, 6, 0xFF191E2C);
        RenderUtils.drawRect(panelX, panelY + 39, panelW, 1, 0x22FFFFFF);

        fontRendererObj.drawStringWithShadow("RAID CLIENT • CONFIGURADOR DE AUTOTEXT", panelX + 15, panelY + 12, 0xFF00E5FF);
        fontRendererObj.drawStringWithShadow("Compatible con teclas y botones laterales del ratón (Mouse 4, Mouse 5...)", panelX + 15, panelY + 25, 0x88AAAAAA);

        // Close button (X)
        float closeX = panelX + panelW - 25;
        float closeY = panelY + 12;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 15 && mouseY >= closeY && mouseY <= closeY + 15;
        fontRendererObj.drawStringWithShadow("✕", closeX, closeY, closeHovered ? 0xFFFF4444 : 0xFF888888);

        // ═════════════════════════════════════════════════════════════════
        // LEFT COLUMN: FORMULARIO AGREGAR MACRO
        // ═════════════════════════════════════════════════════════════════
        fontRendererObj.drawStringWithShadow("CREAR NUEVO MACRO", panelX + 25, panelY + 52, 0xFFFFFFFF);

        // Key Selection Button
        fontRendererObj.drawString("1. Tecla o Botón de ratón:", (int) panelX + 25, (int) panelY + 70, 0xFF9E9E9E);
        float keyBtnX = panelX + 25;
        float keyBtnY = panelY + 84;
        float keyBtnW = 220;
        float keyBtnH = 24;

        boolean keyBtnHovered = mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH;
        int keyBtnBg = listeningForNewKey ? 0xFF00E5FF : (keyBtnHovered ? 0xFF1F2636 : 0xFF181C28);
        int keyBtnBorder = listeningForNewKey ? 0xFFFFFFFF : 0x5500E5FF;
        RenderUtils.drawRoundedRect(keyBtnX, keyBtnY, keyBtnW, keyBtnH, 4, keyBtnBg);
        RenderUtils.drawOutline(keyBtnX, keyBtnY, keyBtnW, keyBtnH, 1.0F, keyBtnBorder);

        String keyText;
        if (listeningForNewKey) {
            keyText = "» PULSA TECLA O BOTÓN LATERAL «";
        } else if (newMacroKey == Keyboard.KEY_NONE) {
            keyText = "[ Click para elegir tecla / botón ]";
        } else {
            AutoTextMacro dummy = new AutoTextMacro(newMacroKey, "", true);
            keyText = "Asignado: [" + dummy.getKeyName() + "]";
        }
        int keyStrW = fontRendererObj.getStringWidth(keyText);
        int keyTextColor = listeningForNewKey ? 0xFF0E1118 : 0xFF00E5FF;
        fontRendererObj.drawStringWithShadow(keyText, keyBtnX + (keyBtnW - keyStrW) / 2.0F, keyBtnY + 8, keyTextColor);

        // Command Input Field
        fontRendererObj.drawString("2. Comando o mensaje a enviar:", (int) panelX + 25, (int) panelY + 116, 0xFF9E9E9E);
        commandField.drawTextBox();

        // Examples info
        fontRendererObj.drawString("Ejemplos: /f home, /kit pvp, /pot, gg, /feed", (int) panelX + 25, (int) panelY + 156, 0x667E879B);

        // Save Button
        float addBtnY = panelY + 185;
        boolean addHovered = mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= addBtnY && mouseY <= addBtnY + 34;
        RenderUtils.drawRoundedRect(panelX + 25, addBtnY, 220, 34, 5, addHovered ? 0xFF33ECFF : 0xFF00E5FF);
        String addLabel = "+ AGREGAR MACRO";
        int addStrW = fontRendererObj.getStringWidth(addLabel);
        fontRendererObj.drawStringWithShadow(addLabel, panelX + 25 + (220 - addStrW) / 2.0F, addBtnY + 12, 0xFF0E1118);

        // Help hint
        fontRendererObj.drawString("• Detecta botones laterales del mouse", (int) panelX + 25, (int) panelY + 235, 0xFF00E5FF);
        fontRendererObj.drawString("  (Mouse 4, Mouse 5, Middle click, etc.)", (int) panelX + 25, (int) panelY + 248, 0x88AAAAAA);
        fontRendererObj.drawString("• Puedes cambiar la tecla de cualquier", (int) panelX + 25, (int) panelY + 263, 0x88AAAAAA);
        fontRendererObj.drawString("  macro haciendo clic sobre su recuadro", (int) panelX + 25, (int) panelY + 276, 0x88AAAAAA);

        // ═════════════════════════════════════════════════════════════════
        // RIGHT COLUMN: LISTA DE MACROS EXISTENTES
        // ═════════════════════════════════════════════════════════════════
        List<AutoTextMacro> macros = AutoTextMod.INSTANCE != null ? AutoTextMod.INSTANCE.getMacros() : null;
        int count = macros != null ? macros.size() : 0;
        fontRendererObj.drawStringWithShadow("MACROS CONFIGURADOS (" + count + ")", panelX + 285, panelY + 52, 0xFFFFFFFF);

        float itemY = panelY + 70;
        float itemW = 250;
        float itemH = 42;

        if (macros == null || macros.isEmpty()) {
            fontRendererObj.drawString("No tienes ningún macro configurado.", (int) panelX + 285, (int) panelY + 80, 0x66AAAAAA);
            fontRendererObj.drawString("Agrega uno usando el formulario de la izquierda.", (int) panelX + 285, (int) panelY + 95, 0x44AAAAAA);
        } else {
            int startIndex = Math.min(scrollOffset, Math.max(0, macros.size() - 5));
            int endIndex = Math.min(startIndex + 5, macros.size());

            for (int i = startIndex; i < endIndex; i++) {
                AutoTextMacro macro = macros.get(i);
                boolean itemHovered = mouseX >= panelX + 285 && mouseX <= panelX + 285 + itemW && mouseY >= itemY && mouseY <= itemY + itemH;

                RenderUtils.drawRoundedRect(panelX + 285, itemY, itemW, itemH, 4, itemHovered ? 0xFF1C2232 : 0xFF161A26);
                RenderUtils.drawOutline(panelX + 285, itemY, itemW, itemH, 1.0F, macro.isEnabled() ? 0x4400E5FF : 0x22FFFFFF);

                // Key Badge Button (Clickable to change key!)
                float badgeX = panelX + 293;
                float badgeY = itemY + 8;
                float badgeW = 66;
                float badgeH = 26;

                boolean badgeHovered = mouseX >= badgeX && mouseX <= badgeX + badgeW && mouseY >= badgeY && mouseY <= badgeY + badgeH;
                boolean isThisListening = listeningMacro == macro;

                int badgeBg = isThisListening ? 0xFF00E5FF : (badgeHovered ? 0xFF283248 : 0xFF1F2636);
                RenderUtils.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 4, badgeBg);
                RenderUtils.drawOutline(badgeX, badgeY, badgeW, badgeH, 1.0F, isThisListening ? 0xFFFFFFFF : 0x6600E5FF);

                String badgeText = isThisListening ? "..." : "[" + macro.getKeyName() + "]";
                int bTextW = fontRendererObj.getStringWidth(badgeText);
                int bTextColor = isThisListening ? 0xFF0E1118 : 0xFF00E5FF;
                fontRendererObj.drawStringWithShadow(badgeText, badgeX + (badgeW - bTextW) / 2.0F, badgeY + 9, bTextColor);

                // Command message text
                String cmdText = macro.getMessage();
                if (cmdText == null) cmdText = "";
                if (cmdText.length() > 18) {
                    cmdText = cmdText.substring(0, 16) + "...";
                }
                fontRendererObj.drawStringWithShadow(cmdText, panelX + 366, itemY + 10, macro.isEnabled() ? 0xFFFFFFFF : 0x66AAAAAA);
                fontRendererObj.drawString("Clic en tecla para cambiar", (int) panelX + 366, (int) itemY + 23, 0x557E879B);

                // Mini Switch (Toggle Active)
                float switchX = panelX + 285 + itemW - 48;
                float switchY = itemY + 15;
                drawMiniSwitch(switchX, switchY, macro.isEnabled());

                // Delete Button (Red ✕)
                float delX = panelX + 285 + itemW - 20;
                float delY = itemY + 15;
                boolean delHovered = mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY <= delY + 14;
                fontRendererObj.drawStringWithShadow("✕", delX, delY + 2, delHovered ? 0xFFFF3333 : 0x66888888);

                itemY += itemH + 6;
            }

            if (macros.size() > 5) {
                fontRendererObj.drawString("Usa la rueda del ratón para desplazarte (" + (startIndex + 1) + "-" + endIndex + " de " + macros.size() + ")", (int) (panelX + 285), (int) (panelY + panelH - 20), 0x55AAAAAA);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawMiniSwitch(float x, float y, boolean enabled) {
        RenderUtils.drawRoundedRect(x, y, 22, 13, 6, enabled ? 0xFF00E5FF : 0xFF252B3B);
        float knobX = enabled ? (x + 11) : (x + 2);
        RenderUtils.drawRoundedRect(knobX, y + 2, 9, 9, 4, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float panelX = width / 2.0F - 280;
        float panelY = height / 2.0F - 180;
        float panelW = 560;
        float panelH = 360;

        // If listening for a key, and user clicks with side buttons / middle click (mouseButton != 0):
        if (listeningForNewKey && mouseButton != 0) {
            newMacroKey = -100 + mouseButton;
            listeningForNewKey = false;
            return;
        }

        if (listeningMacro != null && mouseButton != 0) {
            listeningMacro.setKey(-100 + mouseButton);
            if (AutoTextMod.INSTANCE != null) {
                AutoTextMod.INSTANCE.saveMacros();
            }
            listeningMacro = null;
            return;
        }

        // Close button
        float closeX = panelX + panelW - 25;
        float closeY = panelY + 12;
        if (mouseX >= closeX && mouseX <= closeX + 15 && mouseY >= closeY && mouseY <= closeY + 15) {
            mc.displayGuiScreen(null);
            return;
        }

        commandField.mouseClicked(mouseX, mouseY, mouseButton);

        // Click on "Choose Key" button for new macro
        float keyBtnX = panelX + 25;
        float keyBtnY = panelY + 84;
        float keyBtnW = 220;
        float keyBtnH = 24;
        if (mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH) {
            if (mouseButton == 0) {
                listeningForNewKey = true;
                listeningMacro = null;
            } else {
                newMacroKey = -100 + mouseButton;
                listeningForNewKey = false;
            }
            return;
        }

        // Click on "+ AGREGAR MACRO" button
        float addBtnY = panelY + 185;
        if (mouseX >= panelX + 25 && mouseX <= panelX + 245 && mouseY >= addBtnY && mouseY <= addBtnY + 34) {
            String msg = commandField.getText().trim();
            if (!msg.isEmpty() && newMacroKey != Keyboard.KEY_NONE) {
                if (AutoTextMod.INSTANCE != null) {
                    AutoTextMod.INSTANCE.addMacro(newMacroKey, msg);
                }
                newMacroKey = Keyboard.KEY_NONE;
                commandField.setText("");
            }
            return;
        }

        // Check clicks on existing macros list
        List<AutoTextMacro> macros = AutoTextMod.INSTANCE != null ? AutoTextMod.INSTANCE.getMacros() : null;
        if (macros != null && !macros.isEmpty()) {
            float itemY = panelY + 70;
            float itemW = 250;
            float itemH = 42;

            int startIndex = Math.min(scrollOffset, Math.max(0, macros.size() - 5));
            int endIndex = Math.min(startIndex + 5, macros.size());

            for (int i = startIndex; i < endIndex; i++) {
                AutoTextMacro macro = macros.get(i);

                // Click on key badge to rebind
                float badgeX = panelX + 293;
                float badgeY = itemY + 8;
                float badgeW = 66;
                float badgeH = 26;
                if (mouseX >= badgeX && mouseX <= badgeX + badgeW && mouseY >= badgeY && mouseY <= badgeY + badgeH) {
                    if (mouseButton == 0) {
                        listeningMacro = macro;
                        listeningForNewKey = false;
                    } else {
                        macro.setKey(-100 + mouseButton);
                        if (AutoTextMod.INSTANCE != null) AutoTextMod.INSTANCE.saveMacros();
                    }
                    return;
                }

                // Click on toggle switch
                float switchX = panelX + 285 + itemW - 48;
                float switchY = itemY + 15;
                if (mouseX >= switchX && mouseX <= switchX + 22 && mouseY >= switchY && mouseY <= switchY + 13) {
                    macro.toggle();
                    if (AutoTextMod.INSTANCE != null) AutoTextMod.INSTANCE.saveMacros();
                    return;
                }

                // Click on delete button (✕)
                float delX = panelX + 285 + itemW - 20;
                float delY = itemY + 15;
                if (mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY <= delY + 14) {
                    if (AutoTextMod.INSTANCE != null) {
                        AutoTextMod.INSTANCE.removeMacro(macro);
                    }
                    return;
                }

                itemY += itemH + 6;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningForNewKey) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                newMacroKey = Keyboard.KEY_NONE;
            } else {
                newMacroKey = keyCode;
            }
            listeningForNewKey = false;
            return;
        }

        if (listeningMacro != null) {
            if (keyCode != Keyboard.KEY_ESCAPE) {
                listeningMacro.setKey(keyCode);
                if (AutoTextMod.INSTANCE != null) {
                    AutoTextMod.INSTANCE.saveMacros();
                }
            }
            listeningMacro = null;
            return;
        }

        if (commandField != null && commandField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                commandField.setFocused(false);
                return;
            }
            commandField.textboxKeyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
        }
    }
}
