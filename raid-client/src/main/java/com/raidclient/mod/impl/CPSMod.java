package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventMouse;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.util.ColorUtils;
import com.raidclient.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class CPSMod extends Mod {

    public final BooleanSetting showRightClick = new BooleanSetting("Mostrar Click Derecho", true);
    public final BooleanSetting background = new BooleanSetting("Fondo", true);
    public final BooleanSetting chroma = new BooleanSetting("Chroma RGB", false);
    public final ColorSetting textColor = new ColorSetting("Color Texto", 255, 255, 255, 255);

    private final List<Long> leftClicks = new ArrayList<>();
    private final List<Long> rightClicks = new ArrayList<>();
    private long lastLeftClick = 0;
    private long lastRightClick = 0;

    public CPSMod() {
        super("CPS Counter", "Muestra los clicks por segundo izquierdo y derecho", Category.HUD);
        setX(5);
        setY(23);
        setWidth(60);
        setHeight(14);
        addSettings(showRightClick, background, chroma, textColor);
        setEnabled(true);
    }

    @EventTarget
    public void onMouse(EventMouse event) {
        if (!event.isState()) return; // Solo cuando se presiona el botón hacia abajo
        if (mc.currentScreen != null) return; // Ignorar clics mientras se navega en menús o inventarios

        long now = System.currentTimeMillis();
        if (event.getButton() == 0) {
            if (now - lastLeftClick >= 12) { // 12ms debounce anti-rebote de switch
                leftClicks.add(now);
                lastLeftClick = now;
            }
        } else if (event.getButton() == 1) {
            if (now - lastRightClick >= 12) {
                rightClicks.add(now);
                lastRightClick = now;
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!isEnabled()) return;
        renderHUD();
    }

    @Override
    public void renderHUD() {
        cleanOldClicks();

        int lCps = leftClicks.size();
        int rCps = rightClicks.size();

        String text = showRightClick.isEnabled() ? (lCps + " | " + rCps + " CPS") : (lCps + " CPS");

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

    private void cleanOldClicks() {
        long time = System.currentTimeMillis() - 1000L;
        leftClicks.removeIf(click -> click < time);
        rightClicks.removeIf(click -> click < time);
    }
}
