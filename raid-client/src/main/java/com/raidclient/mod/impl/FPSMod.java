package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.util.ColorUtils;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.Minecraft;

public class FPSMod extends Mod {

    public final BooleanSetting background = new BooleanSetting("Fondo", true);
    public final BooleanSetting brackets = new BooleanSetting("Corchetes [ ]", false);
    public final BooleanSetting chroma = new BooleanSetting("Chroma RGB", false);
    public final ColorSetting textColor = new ColorSetting("Color Texto", 255, 255, 255, 255);

    public FPSMod() {
        super("FPS Counter", "Muestra los cuadros por segundo actuales", Category.HUD);
        setX(5);
        setY(5);
        setWidth(55);
        setHeight(16);
        addSettings(background, brackets, chroma, textColor);
        setEnabled(true);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!isEnabled()) return;
        renderHUD();
    }

    @Override
    public void renderHUD() {
        int fps = Minecraft.getDebugFPS();
        String text = (brackets.isEnabled() ? "[ " : "") + fps + " FPS" + (brackets.isEnabled() ? " ]" : "");

        int stringWidth = mc.fontRendererObj.getStringWidth(text);
        setWidth(stringWidth + 8);
        setHeight(14);

        if (background.isEnabled()) {
            RenderUtils.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), 3, 0x88000000);
            RenderUtils.drawOutline(getX(), getY(), getWidth(), getHeight(), 1.0f, 0x33FFFFFF);
        }

        int color = chroma.isEnabled() ? ColorUtils.getRainbow(4.0f, 0.8f, 1.0f).getRGB() : textColor.getRGB();
        mc.fontRendererObj.drawStringWithShadow(text, getX() + 4, getY() + 3, color);
    }
}
