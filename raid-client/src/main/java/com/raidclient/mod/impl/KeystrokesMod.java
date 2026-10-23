package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.util.ColorUtils;
import com.raidclient.util.RenderUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeystrokesMod extends Mod {

    public final BooleanSetting showMouse = new BooleanSetting("Mostrar Ratón", true);
    public final BooleanSetting showSpace = new BooleanSetting("Mostrar Barra Espaciadora", true);
    public final BooleanSetting chroma = new BooleanSetting("Chroma RGB", false);
    public final ColorSetting keyColor = new ColorSetting("Color Teclas", 255, 255, 255, 255);
    public final ColorSetting pressedColor = new ColorSetting("Color Pulsado", 0, 200, 255, 120);

    public KeystrokesMod() {
        super("Keystrokes", "Muestra las teclas WASD, espacio y clicks en pantalla", Category.HUD);
        setX(5);
        setY(42);
        setWidth(60);
        setHeight(82);
        addSettings(showMouse, showSpace, chroma, keyColor, pressedColor);
        setEnabled(true);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!isEnabled()) return;
        renderHUD();
    }

    @Override
    public void renderHUD() {
        float startX = getX();
        float startY = getY();

        boolean wPressed = mc.gameSettings.keyBindForward.isKeyDown();
        boolean aPressed = mc.gameSettings.keyBindLeft.isKeyDown();
        boolean sPressed = mc.gameSettings.keyBindBack.isKeyDown();
        boolean dPressed = mc.gameSettings.keyBindRight.isKeyDown();
        boolean lmbPressed = Mouse.isButtonDown(0);
        boolean rmbPressed = Mouse.isButtonDown(1);
        boolean spacePressed = Keyboard.isKeyDown(Keyboard.KEY_SPACE);

        // Row 1: W
        renderKey("W", startX + 21, startY, 18, 18, wPressed);

        // Row 2: A, S, D
        renderKey("A", startX, startY + 21, 18, 18, aPressed);
        renderKey("S", startX + 21, startY + 21, 18, 18, sPressed);
        renderKey("D", startX + 42, startY + 21, 18, 18, dPressed);

        float currentY = startY + 42;

        // Row 3: LMB & RMB
        if (showMouse.isEnabled()) {
            renderKey("LMB", startX, currentY, 29, 18, lmbPressed);
            renderKey("RMB", startX + 31, currentY, 29, 18, rmbPressed);
            currentY += 21;
        }

        // Row 4: Space
        if (showSpace.isEnabled()) {
            renderKey("-----", startX, currentY, 60, 14, spacePressed);
            currentY += 17;
        }

        setWidth(60);
        setHeight(currentY - startY);
    }

    private void renderKey(String label, float x, float y, float w, float h, boolean pressed) {
        int bg = pressed ? pressedColor.getRGB() : 0x77000000;
        RenderUtils.drawRoundedRect(x, y, w, h, 2, bg);
        RenderUtils.drawOutline(x, y, w, h, 1.0f, pressed ? 0xAAFFFFFF : 0x33FFFFFF);

        int textCol = chroma.isEnabled() ? ColorUtils.getRainbow(4.0f, 0.8f, 1.0f).getRGB() : keyColor.getRGB();
        int strWidth = mc.fontRendererObj.getStringWidth(label);
        mc.fontRendererObj.drawStringWithShadow(label, x + (w - strWidth) / 2.0f, y + (h - 8) / 2.0f, textCol);
    }
}
