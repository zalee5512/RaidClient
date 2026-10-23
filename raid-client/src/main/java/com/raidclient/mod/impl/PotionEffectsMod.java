package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.Collection;

public class PotionEffectsMod extends Mod {

    public final BooleanSetting background = new BooleanSetting("Fondo Translúcido", true);
    public final BooleanSetting showDuration = new BooleanSetting("Mostrar Duración", true);
    public final BooleanSetting potionColor = new BooleanSetting("Color de Poción", true);
    public final ColorSetting defaultColor = new ColorSetting("Color Texto", 255, 255, 255, 255);

    public PotionEffectsMod() {
        super("Potion Effects", "Muestra las pociones activas con su nivel y tiempo restante en pantalla", Category.HUD);
        setX(10);
        setY(80);
        setWidth(110);
        setHeight(50);
        addSettings(background, showDuration, potionColor, defaultColor);
        setEnabled(true);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!isEnabled()) return;
        renderHUD();
    }

    @Override
    public void renderHUD() {
        if (mc.thePlayer == null) return;

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects.isEmpty()) return;

        int lineHeight = 16;
        int totalHeight = effects.size() * lineHeight + 4;
        int maxWidth = 90;

        // Calculate maximum width
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;

            String name = I18n.format(potion.getName());
            if (effect.getAmplifier() > 0) {
                name += " " + getRomanNumeral(effect.getAmplifier() + 1);
            }
            if (showDuration.isEnabled()) {
                name += " (" + Potion.getDurationString(effect) + ")";
            }
            int strW = mc.fontRendererObj.getStringWidth(name);
            if (strW > maxWidth) maxWidth = strW;
        }

        setWidth(maxWidth + 12);
        setHeight(totalHeight);

        float drawX = getX();
        float drawY = getY();

        if (background.isEnabled()) {
            RenderUtils.drawRoundedRect(drawX, drawY, getWidth(), getHeight(), 4, 0x880D1117);
            RenderUtils.drawOutline(drawX, drawY, getWidth(), getHeight(), 1.0F, 0x3300E5FF);
        }

        int offset = 4;
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;

            String name = I18n.format(potion.getName());
            if (effect.getAmplifier() > 0) {
                name += " " + getRomanNumeral(effect.getAmplifier() + 1);
            }

            int color = potionColor.isEnabled() ? (0xFF000000 | potion.getLiquidColor()) : defaultColor.getRGB();

            mc.fontRendererObj.drawStringWithShadow(name, drawX + 6, drawY + offset, color);

            if (showDuration.isEnabled()) {
                String duration = Potion.getDurationString(effect);
                int durW = mc.fontRendererObj.getStringWidth(duration);
                mc.fontRendererObj.drawStringWithShadow(duration, drawX + getWidth() - durW - 6, drawY + offset, 0xCCAAAAAA);
            }

            offset += lineHeight;
        }
    }

    @Override
    public void renderDummy(int mouseX, int mouseY) {
        setWidth(110);
        setHeight(36);

        if (background.isEnabled()) {
            RenderUtils.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), 4, 0x880D1117);
            RenderUtils.drawOutline(getX(), getY(), getWidth(), getHeight(), 1.0F, 0x8800E5FF);
        }

        mc.fontRendererObj.drawStringWithShadow("Velocidad II", getX() + 6, getY() + 4, 0xFF3498DB);
        mc.fontRendererObj.drawStringWithShadow("01:25", getX() + getWidth() - 32, getY() + 4, 0xCCAAAAAA);

        mc.fontRendererObj.drawStringWithShadow("Fuerza II", getX() + 6, getY() + 20, 0xFFE74C3C);
        mc.fontRendererObj.drawStringWithShadow("01:25", getX() + getWidth() - 32, getY() + 20, 0xCCAAAAAA);
    }

    private String getRomanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(number);
        }
    }
}
