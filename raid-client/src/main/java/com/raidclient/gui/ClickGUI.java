package com.raidclient.gui;

import com.raidclient.RaidClient;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.mod.impl.AutoTextMod;
import com.raidclient.mod.impl.CrosshairMod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.setting.ModeSetting;
import com.raidclient.setting.NumberSetting;
import com.raidclient.setting.Setting;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ClickGUI extends GuiScreen {

    private Category selectedCategory = null; // null = TODOS
    private String searchQuery = "";
    private Mod selectedModForConfig = null;
    private NumberSetting draggingSlider = null;

    private int scrollRow = 0;
    private static final int VISIBLE_ROWS = 5;
    private long openTime = 0;

    // Compact, elegant dimensions
    private static final float PANEL_W = 460;
    private static final float PANEL_H = 280;

    @Override
    public void initGui() {
        scrollRow = 0;
        openTime = System.currentTimeMillis();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getConfigManager() != null) {
            RaidClient.INSTANCE.getConfigManager().saveConfig();
        }
    }

    private List<Mod> getFilteredMods() {
        return RaidClient.INSTANCE.getModManager().getMods().stream()
                .filter(m -> selectedCategory == null || m.getCategory() == selectedCategory)
                .filter(m -> searchQuery.isEmpty() || m.getName().toLowerCase().contains(searchQuery.toLowerCase()) || m.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (selectedModForConfig == null) {
            int dWheel = Mouse.getEventDWheel();
            if (dWheel != 0) {
                int totalRows = (int) Math.ceil(getFilteredMods().size() / 2.0);
                int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
                if (dWheel > 0) {
                    scrollRow = Math.max(0, scrollRow - 1);
                } else {
                    scrollRow = Math.min(maxScroll, scrollRow + 1);
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Translucent background overlay (25% oscurecido para ver claramente el entorno de Minecraft)
        RenderUtils.drawRect(0, 0, width, height, 0x40000000);

        float panelW = PANEL_W;
        float panelH = PANEL_H;
        float panelX = (width - panelW) / 2.0F;
        float panelY = (height - panelH) / 2.0F;

        // Main Panel background (Glassmorphism ultra-limpio)
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 6, 0x880C1018);
        RenderUtils.drawOutline(panelX, panelY, panelW, panelH, 1.0F, 0x3300E5FF);

        // ══════════════════════════════════════════════════════════════════════════
        // FILA 1: HEADER COMPACTO
        // ══════════════════════════════════════════════════════════════════════════
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, 28, 6, 0xAA121722);
        RenderUtils.drawRect(panelX, panelY + 27, panelW, 1, 0x18FFFFFF);

        // Logo
        fontRendererObj.drawStringWithShadow("RAID CLIENT", panelX + 10, panelY + 9, 0xFF00E5FF);
        fontRendererObj.drawStringWithShadow("v1.8.9", panelX + 78, panelY + 10, 0x77AAAAAA);

        // 1. "⚡ Macros"
        float atBtnX = panelX + panelW - 222;
        float atBtnW = 66;
        boolean atHovered = mouseX >= atBtnX && mouseX <= atBtnX + atBtnW && mouseY >= panelY + 5 && mouseY <= panelY + 22;
        RenderUtils.drawRoundedRect(atBtnX, panelY + 5, atBtnW, 17, 3, atHovered ? 0xFF00E5FF : 0x33192538);
        RenderUtils.drawOutline(atBtnX, panelY + 5, atBtnW, 17, 1.0F, atHovered ? 0xFFFFFFFF : 0x4400E5FF);
        fontRendererObj.drawStringWithShadow("⚡ Macros", atBtnX + 6, panelY + 9, atHovered ? 0xFF0E1118 : 0xFF00E5FF);

        // 2. "⚔ Puntos"
        float wpBtnX = panelX + panelW - 152;
        float wpBtnW = 68;
        boolean wpHovered = mouseX >= wpBtnX && mouseX <= wpBtnX + wpBtnW && mouseY >= panelY + 5 && mouseY <= panelY + 22;
        RenderUtils.drawRoundedRect(wpBtnX, panelY + 5, wpBtnW, 17, 3, wpHovered ? 0xFF00E5FF : 0x33192538);
        RenderUtils.drawOutline(wpBtnX, panelY + 5, wpBtnW, 17, 1.0F, wpHovered ? 0xFFFFFFFF : 0x4400E5FF);
        fontRendererObj.drawStringWithShadow("⚔ Puntos", wpBtnX + 6, panelY + 9, wpHovered ? 0xFF0E1118 : 0xFF00E5FF);

        // 3. "HUD"
        float editHudX = panelX + panelW - 80;
        float editHudW = 54;
        boolean editHovered = mouseX >= editHudX && mouseX <= editHudX + editHudW && mouseY >= panelY + 5 && mouseY <= panelY + 22;
        RenderUtils.drawRoundedRect(editHudX, panelY + 5, editHudW, 17, 3, editHovered ? 0xFF00C8FF : 0xCC0084FF);
        RenderUtils.drawOutline(editHudX, panelY + 5, editHudW, 17, 1.0F, 0x44FFFFFF);
        fontRendererObj.drawStringWithShadow("HUD", editHudX + 16, panelY + 9, 0xFFFFFFFF);

        // 4. Close (✕)
        float closeX = panelX + panelW - 20;
        float closeY = panelY + 7;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14;
        fontRendererObj.drawStringWithShadow("✕", closeX + 1, closeY + 1, closeHovered ? 0xFFFF4444 : 0xFF888888);

        // ══════════════════════════════════════════════════════════════════════════
        // FILA 2: NAVEGACIÓN Y BÚSQUEDA
        // ══════════════════════════════════════════════════════════════════════════
        float navY = panelY + 31;
        float tabX = panelX + 10;

        tabX = drawCategoryTab("TODOS", null, tabX, navY, mouseX, mouseY);
        for (Category cat : Category.values()) {
            tabX = drawCategoryTab(cat.getDisplayName().toUpperCase(), cat, tabX, navY, mouseX, mouseY);
        }

        // Search Bar
        float searchW = 105;
        float searchH = 16;
        float searchX = panelX + panelW - searchW - 10;
        RenderUtils.drawRoundedRect(searchX, navY + 1, searchW, searchH, 3, 0x55101420);
        RenderUtils.drawOutline(searchX, navY + 1, searchW, searchH, 1.0F, 0x22FFFFFF);
        String searchDisplay = searchQuery.isEmpty() ? "🔍 Buscar..." : searchQuery;
        fontRendererObj.drawStringWithShadow(searchDisplay, searchX + 5, navY + 5, searchQuery.isEmpty() ? 0x55AAAAAA : 0xFFFFFFFF);

        // Divisor inferior
        RenderUtils.drawRect(panelX + 10, panelY + 52, panelW - 20, 1, 0x14FFFFFF);

        // ══════════════════════════════════════════════════════════════════════════
        // FILA 3: CUADRÍCULA DE MODS (LIMPIA, SIN BORDES FEOS)
        // ══════════════════════════════════════════════════════════════════════════
        List<Mod> mods = getFilteredMods();
        float startGridX = panelX + 10;
        float startGridY = panelY + 56;
        float cardW = (panelW - 26) / 2.0F;
        float cardH = 38;
        float gapY = 4;

        int totalRows = (int) Math.ceil(mods.size() / 2.0);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (scrollRow > maxScroll) scrollRow = maxScroll;

        int startIndex = scrollRow * 2;
        int endIndex = Math.min(startIndex + (VISIBLE_ROWS * 2), mods.size());

        for (int i = startIndex; i < endIndex; i++) {
            Mod mod = mods.get(i);
            int relIndex = i - startIndex;
            int col = relIndex % 2;
            int row = relIndex / 2;

            float cx = startGridX + col * (cardW + 6);
            float cy = startGridY + row * (cardH + gapY);

            boolean cardHovered = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;

            // Fondo y borde de tarjeta claros y diferenciados
            int cardBg;
            int cardBorder;
            if (mod.isEnabled()) {
                cardBg = cardHovered ? 0x44004055 : 0x280B2736;
                cardBorder = cardHovered ? 0xCC00E5FF : 0x6600E5FF;
            } else {
                cardBg = cardHovered ? 0x441C2436 : 0x25121622;
                cardBorder = cardHovered ? 0x33FFFFFF : 0x14FFFFFF;
            }

            RenderUtils.drawRoundedRect(cx, cy, cardW, cardH, 4, cardBg);
            RenderUtils.drawOutline(cx, cy, cardW, cardH, 1.0F, cardBorder);

            // Indicador de acento elegante a la izquierda cuando está activo
            if (mod.isEnabled()) {
                RenderUtils.drawRoundedRect(cx + 2, cy + 5, 3.0F, cardH - 10, 1.5F, 0xFF00E5FF);
            }

            // Nombre del mod
            int titleColor = mod.isEnabled() ? 0xFF00E5FF : 0xFFAAAAAA;
            fontRendererObj.drawStringWithShadow(mod.getName(), cx + 9, cy + 6, titleColor);

            // Descripción breve
            String desc = mod.getDescription();
            if (desc.length() > 24) desc = desc.substring(0, 22) + "...";
            fontRendererObj.drawString(desc, (int) (cx + 9), (int) (cy + 19), mod.isEnabled() ? 0xFF8AE8F5 : 0xFF6C7588);

            // ══════════════════════════════════════════════════════════════════
            // Elementos de la derecha alineados horizontalmente al centro
            // ══════════════════════════════════════════════════════════════════
            float midY = cy + (cardH - 13) / 2.0F;

            // Toggle switch (compacto)
            float switchX = cx + cardW - 32;
            drawToggleSwitch(switchX, midY, mod.isEnabled());

            // Settings button (Icono de engranaje ⚙ estilizado a la izquierda del switch)
            if (!mod.getSettings().isEmpty() || mod instanceof AutoTextMod) {
                float gearX = cx + cardW - 51;
                float gearY = midY - 1;
                float gearW = 15;
                float gearH = 15;
                boolean gearHovered = mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH;
                RenderUtils.drawRoundedRect(gearX, gearY, gearW, gearH, 3, gearHovered ? 0x6600E5FF : 0x331C2434);
                RenderUtils.drawOutline(gearX, gearY, gearW, gearH, 1.0F, gearHovered ? 0x8800E5FF : 0x1AFFFFFF);
                fontRendererObj.drawString("⚙", (int) (gearX + 3), (int) (gearY + 3), gearHovered ? 0xFF00E5FF : 0xFF888E9B);
            }
        }

        // Scrollbar Indicator
        if (totalRows > VISIBLE_ROWS) {
            float sbX = panelX + panelW - 4;
            float sbY = startGridY;
            float sbH = VISIBLE_ROWS * (cardH + gapY) - gapY;
            RenderUtils.drawRoundedRect(sbX, sbY, 2, sbH, 1, 0x22FFFFFF);

            float thumbH = Math.max(14, sbH * (float) VISIBLE_ROWS / (float) totalRows);
            float thumbY = sbY + (sbH - thumbH) * ((float) scrollRow / (float) maxScroll);
            RenderUtils.drawRoundedRect(sbX, thumbY, 2, thumbH, 1, 0xFF00E5FF);
        }

        // Settings modal dialog
        if (selectedModForConfig != null) {
            drawModSettingsModal(mouseX, mouseY, panelX, panelY, panelW, panelH);
        }

        // Slider dragging
        if (draggingSlider != null && selectedModForConfig != null) {
            boolean isCrosshair = selectedModForConfig instanceof CrosshairMod;
            float mW = isCrosshair ? 440 : 340;
            float mX = panelX + (panelW - mW) / 2.0F;
            float rightMargin = isCrosshair ? 104 : 0;
            float sliderX = mX + 120;
            float sliderW = mW - 136 - rightMargin;
            double diff = Math.min(sliderW, Math.max(0, mouseX - sliderX));
            double val = draggingSlider.getMinimum() + (diff / sliderW) * (draggingSlider.getMaximum() - draggingSlider.getMinimum());
            draggingSlider.setValue(val);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private float drawCategoryTab(String title, Category cat, float x, float y, int mx, int my) {
        int strW = fontRendererObj.getStringWidth(title);
        float w = strW + 8;
        float h = 16;

        boolean active = (selectedCategory == cat);
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;

        if (active) {
            RenderUtils.drawRoundedRect(x, y, w, h, 3, 0x3300E5FF);
            RenderUtils.drawOutline(x, y, w, h, 1.0F, 0xFF00E5FF);
        } else if (hovered) {
            RenderUtils.drawRoundedRect(x, y, w, h, 3, 0x1AFFFFFF);
        }

        fontRendererObj.drawStringWithShadow(title, x + 4, y + 4, active ? 0xFF00E5FF : (hovered ? 0xFFFFFFFF : 0xFF8E95A5));
        return x + w + 3;
    }

    private void drawToggleSwitch(float x, float y, boolean enabled) {
        float w = 26;
        float h = 13;
        int bgColor = enabled ? 0xFF00E5FF : 0xFF1C2230;
        RenderUtils.drawRoundedRect(x, y, w, h, 6, bgColor);
        RenderUtils.drawOutline(x, y, w, h, 1.0F, enabled ? 0x88FFFFFF : 0x22FFFFFF);

        float knobX = enabled ? (x + w - 11) : (x + 2);
        RenderUtils.drawRoundedRect(knobX, y + 1.5F, 9, 10, 4.5F, enabled ? 0xFFFFFFFF : 0xFF7A8294);
    }

    private void drawModSettingsModal(int mx, int my, float px, float py, float pw, float ph) {
        RenderUtils.drawRect(px, py, pw, ph, 0x88000000);

        boolean isCrosshair = selectedModForConfig instanceof CrosshairMod;
        float mW = isCrosshair ? 440 : 340;
        float mH = Math.min(300, 50 + (selectedModForConfig.getSettings().size() * 21) + 20);
        float mX = px + (pw - mW) / 2.0F;
        float mY = py + (ph - mH) / 2.0F;
        float rightMargin = isCrosshair ? 104 : 0;

        RenderUtils.drawRoundedRect(mX, mY, mW, mH, 6, 0xEE111520);
        RenderUtils.drawOutline(mX, mY, mW, mH, 1.0F, 0xFF00E5FF);

        fontRendererObj.drawStringWithShadow("Ajustes: " + selectedModForConfig.getName(), mX + 12, mY + 11, 0xFF00E5FF);

        float closeX = mX + mW - 20;
        float closeY = mY + 9;
        boolean closeHovered = mx >= closeX && mx <= closeX + 12 && my >= closeY && my <= closeY + 12;
        fontRendererObj.drawStringWithShadow("✕", closeX, closeY, closeHovered ? 0xFFFF4444 : 0xFFAAAAAA);

        // Previsualización en vivo si es Custom Crosshair
        if (isCrosshair) {
            float prevW = 80;
            float prevH = 80;
            float prevX = mX + mW - prevW - 14;
            float prevY = mY + 35;

            RenderUtils.drawRoundedRect(prevX, prevY, prevW, prevH, 4, 0xEE0B0E14);
            RenderUtils.drawOutline(prevX, prevY, prevW, prevH, 1.0F, 0x4400E5FF);
            fontRendererObj.drawStringWithShadow("Vista Previa", prevX + 8, prevY + 6, 0xFF708090);

            ((CrosshairMod) selectedModForConfig).renderAt(prevX + prevW / 2.0F, prevY + prevH / 2.0F + 5.0F);
        }

        float settingY = mY + 30;
        for (Setting s : selectedModForConfig.getSettings()) {
            if (s instanceof BooleanSetting) {
                BooleanSetting bs = (BooleanSetting) s;
                fontRendererObj.drawStringWithShadow(bs.getName(), mX + 12, settingY + 2, 0xFFFFFFFF);
                drawToggleSwitch(mX + mW - rightMargin - 38, settingY, bs.isEnabled());
                settingY += 21;
            } else if (s instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) s;
                fontRendererObj.drawStringWithShadow(ns.getName() + ": " + String.format("%.1f", ns.getValue()), mX + 12, settingY + 2, 0xFFFFFFFF);

                float sliderX = mX + 120;
                float sliderW = mW - 136 - rightMargin;
                RenderUtils.drawRoundedRect(sliderX, settingY + 2, sliderW, 7, 3, 0xFF10121A);

                double percent = (ns.getValue() - ns.getMinimum()) / (ns.getMaximum() - ns.getMinimum());
                RenderUtils.drawRoundedRect(sliderX, settingY + 2, (float) (sliderW * percent), 7, 3, 0xFF00E5FF);
                settingY += 21;
            } else if (s instanceof ColorSetting) {
                ColorSetting cs = (ColorSetting) s;
                fontRendererObj.drawStringWithShadow(cs.getName(), mX + 12, settingY + 2, 0xFFFFFFFF);
                RenderUtils.drawRoundedRect(mX + mW - rightMargin - 42, settingY, 26, 13, 3, cs.getRGB());
                RenderUtils.drawOutline(mX + mW - rightMargin - 42, settingY, 26, 13, 1.0F, 0xFFFFFFFF);
                settingY += 21;
            } else if (s instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) s;
                fontRendererObj.drawStringWithShadow(ms.getName(), mX + 12, settingY + 2, 0xFFFFFFFF);
                String modeText = "[ " + ms.getMode() + " ]";
                int modeW = fontRendererObj.getStringWidth(modeText) + 8;
                float btnX = mX + mW - rightMargin - modeW - 12;
                RenderUtils.drawRoundedRect(btnX, settingY, modeW, 13, 3, 0xFF192538);
                RenderUtils.drawOutline(btnX, settingY, modeW, 13, 1.0F, 0x5500E5FF);
                fontRendererObj.drawStringWithShadow(modeText, btnX + 4, settingY + 3, 0xFF00E5FF);
                settingY += 21;
            }
        }

        if (selectedModForConfig instanceof AutoTextMod) {
            float btnX = mX + 12;
            float btnY = settingY + 5;
            float btnW = mW - 24;
            float btnH = 20;
            boolean btnHover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
            RenderUtils.drawRoundedRect(btnX, btnY, btnW, btnH, 4, btnHover ? 0xFF00E5FF : 0xFF192538);
            RenderUtils.drawOutline(btnX, btnY, btnW, btnH, 1.0F, 0x5500E5FF);
            String label = "⚡ CONFIGURAR MACROS";
            int labelW = fontRendererObj.getStringWidth(label);
            fontRendererObj.drawStringWithShadow(label, btnX + (btnW - labelW) / 2.0F, btnY + 6, btnHover ? 0xFF0E1118 : 0xFF00E5FF);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float panelW = PANEL_W;
        float panelH = PANEL_H;
        float panelX = (width - panelW) / 2.0F;
        float panelY = (height - panelH) / 2.0F;

        // Modal de ajustes
        if (selectedModForConfig != null) {
            boolean isCrosshair = selectedModForConfig instanceof CrosshairMod;
            float mW = isCrosshair ? 440 : 340;
            float mH = Math.min(300, 50 + (selectedModForConfig.getSettings().size() * 21) + 20);
            float mX = panelX + (panelW - mW) / 2.0F;
            float mY = panelY + (panelH - mH) / 2.0F;
            float rightMargin = isCrosshair ? 104 : 0;

            float closeX = mX + mW - 20;
            float closeY = mY + 9;
            if (mouseX >= closeX && mouseX <= closeX + 12 && mouseY >= closeY && mouseY <= closeY + 12) {
                selectedModForConfig = null;
                return;
            }

            float settingY = mY + 30;
            for (Setting s : selectedModForConfig.getSettings()) {
                if (s instanceof BooleanSetting) {
                    float switchX = mX + mW - rightMargin - 38;
                    if (mouseX >= switchX && mouseX <= switchX + 26 && mouseY >= settingY && mouseY <= settingY + 13) {
                        ((BooleanSetting) s).toggle();
                        return;
                    }
                    settingY += 21;
                } else if (s instanceof NumberSetting) {
                    NumberSetting ns = (NumberSetting) s;
                    float sliderX = mX + 120;
                    float sliderW = mW - 136 - rightMargin;
                    if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= settingY && mouseY <= settingY + 11) {
                        draggingSlider = ns;
                        double percent = (mouseX - sliderX) / sliderW;
                        double val = ns.getMinimum() + percent * (ns.getMaximum() - ns.getMinimum());
                        ns.setValue(val);
                        return;
                    }
                    settingY += 21;
                } else if (s instanceof ColorSetting) {
                    settingY += 21;
                } else if (s instanceof ModeSetting) {
                    ModeSetting ms = (ModeSetting) s;
                    String modeText = "[ " + ms.getMode() + " ]";
                    int modeW = fontRendererObj.getStringWidth(modeText) + 8;
                    float btnX = mX + mW - rightMargin - modeW - 12;
                    if (mouseX >= btnX && mouseX <= btnX + modeW && mouseY >= settingY && mouseY <= settingY + 13) {
                        ms.cycle();
                        return;
                    }
                    settingY += 21;
                }
            }

            if (selectedModForConfig instanceof AutoTextMod) {
                float btnX = mX + 12;
                float btnY = settingY + 5;
                float btnW = mW - 24;
                float btnH = 20;
                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    mc.displayGuiScreen(new AutoTextScreen());
                    return;
                }
            }
            return;
        }

        // Close button (✕)
        float closeX = panelX + panelW - 20;
        float closeY = panelY + 7;
        if (mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14) {
            mc.displayGuiScreen(null);
            return;
        }

        // Quick Actions
        // 1. "AutoText"
        float atBtnX = panelX + panelW - 222;
        if (mouseX >= atBtnX && mouseX <= atBtnX + 66 && mouseY >= panelY + 5 && mouseY <= panelY + 22) {
            mc.displayGuiScreen(new AutoTextScreen());
            return;
        }

        // 2. "Waypoints"
        float wpBtnX = panelX + panelW - 152;
        if (mouseX >= wpBtnX && mouseX <= wpBtnX + 68 && mouseY >= panelY + 5 && mouseY <= panelY + 22) {
            mc.displayGuiScreen(new WaypointScreen());
            return;
        }

        // 3. "HUD"
        float editHudX = panelX + panelW - 80;
        if (mouseX >= editHudX && mouseX <= editHudX + 54 && mouseY >= panelY + 5 && mouseY <= panelY + 22) {
            mc.displayGuiScreen(new HUDConfigScreen());
            return;
        }

        // Category Tabs
        float navY = panelY + 31;
        float tabX = panelX + 10;

        float tabWAll = fontRendererObj.getStringWidth("TODOS") + 8;
        if (mouseX >= tabX && mouseX <= tabX + tabWAll && mouseY >= navY && mouseY <= navY + 16) {
            selectedCategory = null;
            scrollRow = 0;
            return;
        }
        tabX += tabWAll + 3;

        for (Category cat : Category.values()) {
            float tabW = fontRendererObj.getStringWidth(cat.getDisplayName().toUpperCase()) + 8;
            if (mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= navY && mouseY <= navY + 16) {
                selectedCategory = cat;
                scrollRow = 0;
                return;
            }
            tabX += tabW + 3;
        }

        // Mod Cards Click
        List<Mod> mods = getFilteredMods();
        float startGridX = panelX + 10;
        float startGridY = panelY + 56;
        float cardW = (panelW - 26) / 2.0F;
        float cardH = 38;
        float gapY = 4;

        int startIndex = scrollRow * 2;
        int endIndex = Math.min(startIndex + (VISIBLE_ROWS * 2), mods.size());

        for (int i = startIndex; i < endIndex; i++) {
            Mod mod = mods.get(i);
            int relIndex = i - startIndex;
            int col = relIndex % 2;
            int row = relIndex / 2;

            float cx = startGridX + col * (cardW + 6);
            float cy = startGridY + row * (cardH + gapY);
            float midY = cy + (cardH - 13) / 2.0F;

            // Settings button click (⚙)
            if (!mod.getSettings().isEmpty() || mod instanceof AutoTextMod) {
                float gearX = cx + cardW - 51;
                float gearY = midY - 1;
                float gearW = 15;
                float gearH = 15;
                if (mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH) {
                    selectedModForConfig = mod;
                    return;
                }
            }

            // Toggle switch or card click
            if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                mod.toggle();
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || (keyCode == Keyboard.KEY_RSHIFT && (System.currentTimeMillis() - openTime > 200))) {
            if (selectedModForConfig != null) {
                selectedModForConfig = null;
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollRow = 0;
            }
        } else if (Character.isLetterOrDigit(typedChar) || typedChar == ' ') {
            if (searchQuery.length() < 20) {
                searchQuery += typedChar;
                scrollRow = 0;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
