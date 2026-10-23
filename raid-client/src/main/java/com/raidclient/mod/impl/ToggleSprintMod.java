package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.event.impl.EventTick;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.util.RenderUtils;

public class ToggleSprintMod extends Mod {

    public final BooleanSetting showHUD = new BooleanSetting("Mostrar HUD", true);
    public final BooleanSetting background = new BooleanSetting("Fondo HUD", true);
    public final ColorSetting textColor = new ColorSetting("Color Texto", 255, 255, 255, 255);

    public ToggleSprintMod() {
        super("ToggleSprint", "Mantiene presionado correr automáticamente y muestra indicador HUD", Category.HUD);
        setX(5);
        setY(130);
        setWidth(95);
        setHeight(14);
        addSettings(showHUD, background, textColor);
        setEnabled(true);
    }

    @Override
    public boolean isHUDMod() {
        return true;
    }

    @Override
    public void renderDummy(int mouseX, int mouseY) {
        renderHUD();
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (!isEnabled()) return;
        if (mc.thePlayer != null && mc.gameSettings.keyBindForward.isKeyDown()) {
            mc.thePlayer.setSprinting(true);
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!isEnabled() || !showHUD.isEnabled()) return;
        renderHUD();
    }

    @Override
    public void renderHUD() {
        if (!showHUD.isEnabled()) return;

        String text = "[Sprinting (Toggled)]";
        int strWidth = mc.fontRendererObj.getStringWidth(text);
        setWidth(strWidth + 8);
        setHeight(14);

        if (background.isEnabled()) {
            RenderUtils.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), 3, 0x88000000);
            RenderUtils.drawOutline(getX(), getY(), getWidth(), getHeight(), 1.0f, 0x33FFFFFF);
        }

        mc.fontRendererObj.drawStringWithShadow(text, getX() + 4, getY() + 3, textColor.getRGB());
    }
}
