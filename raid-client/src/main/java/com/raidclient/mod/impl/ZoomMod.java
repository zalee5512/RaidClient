package com.raidclient.mod.impl;

import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ModeSetting;
import com.raidclient.setting.NumberSetting;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ZoomMod extends Mod {

    public final ModeSetting zoomKey = new ModeSetting("Tecla", "C", "C", "V", "F", "Z", "X", "LCONTROL", "LMENU");
    public final NumberSetting zoomFactor = new NumberSetting("Nivel de Zoom", 4.0, 2.0, 10.0, 0.5);
    public final BooleanSetting smoothZoom = new BooleanSetting("Zoom Suave", true);
    public final BooleanSetting smoothCamera = new BooleanSetting("Cámara Cinemática", true);
    public final BooleanSetting scrollZoom = new BooleanSetting("Rueda del Ratón", true);

    private boolean zooming = false;
    private boolean prevSmoothCamera = false;
    private double currentFactor = 1.0;
    private double dynamicScrollMultiplier = 1.0;

    public ZoomMod() {
        super("Zoom", "Acerca la vista manteniendo presionada la tecla configurada (Estilo OptiFine)", Category.RENDER);
        setKey(Keyboard.KEY_C);
        addSettings(zoomKey, zoomFactor, smoothZoom, smoothCamera, scrollZoom);
        setEnabled(true);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean isZooming() {
        return isEnabled() && zooming;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !isEnabled()) return;

        int targetKey = Keyboard.getKeyIndex(zoomKey.getMode());
        if (targetKey == Keyboard.KEY_NONE) targetKey = Keyboard.KEY_C;

        boolean keyDown = Keyboard.isKeyDown(targetKey) && mc.currentScreen == null;

        if (keyDown && !zooming) {
            zooming = true;
            prevSmoothCamera = mc.gameSettings.smoothCamera;
            if (smoothCamera.isEnabled()) {
                mc.gameSettings.smoothCamera = true;
            }
            dynamicScrollMultiplier = 1.0;
        } else if (!keyDown && zooming) {
            zooming = false;
            mc.gameSettings.smoothCamera = prevSmoothCamera;
            dynamicScrollMultiplier = 1.0;
        }

        // Modificar zoom con la rueda del ratón mientras se mantiene presionado el zoom
        if (zooming && scrollZoom.isEnabled()) {
            int dWheel = Mouse.getDWheel();
            if (dWheel > 0) {
                dynamicScrollMultiplier = Math.min(3.0, dynamicScrollMultiplier + 0.2);
            } else if (dWheel < 0) {
                dynamicScrollMultiplier = Math.max(0.5, dynamicScrollMultiplier - 0.2);
            }
        }

        // Interpolación suave del multiplicador de FOV
        double target = zooming ? (1.0 / (zoomFactor.getValue() * dynamicScrollMultiplier)) : 1.0;
        if (smoothZoom.isEnabled()) {
            currentFactor += (target - currentFactor) * 0.45;
        } else {
            currentFactor = target;
        }
    }

    @SubscribeEvent
    public void onFOV(EntityViewRenderEvent.FOVModifier event) {
        if (!isEnabled()) return;
        if (zooming || Math.abs(currentFactor - 1.0) > 0.005) {
            event.setFOV(event.getFOV() * (float) currentFactor);
        }
    }
}
