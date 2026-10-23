package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventTick;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import org.lwjgl.opengl.Display;

public class FPSBoostMod extends Mod {

    public final BooleanSetting unlockFps = new BooleanSetting("Desbloquear FPS", true);
    public final BooleanSetting fastVBO = new BooleanSetting("Usar VBOs (GPU)", true);
    public final BooleanSetting noEntityShadows = new BooleanSetting("Quitar Sombras Entidades", true);
    public final BooleanSetting noClouds = new BooleanSetting("Quitar Nubes", true);

    public FPSBoostMod() {
        super("FPS Boost", "Optimiza el rendimiento del cliente y desbloquea los 60 FPS", Category.RENDER);
        addSettings(unlockFps, fastVBO, noEntityShadows, noClouds);
        setEnabled(true);
        applyOptimizations();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        applyOptimizations();
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (!isEnabled()) return;

        if (unlockFps.isEnabled()) {
            if (mc.gameSettings != null) {
                if (mc.gameSettings.enableVsync) {
                    mc.gameSettings.enableVsync = false;
                    try { Display.setVSyncEnabled(false); } catch (Throwable ignored) {}
                }
                if (mc.gameSettings.limitFramerate < 260) {
                    mc.gameSettings.limitFramerate = 260;
                }
            }
        }
    }

    public void applyOptimizations() {
        try {
            if (mc.gameSettings != null) {
                if (unlockFps.isEnabled()) {
                    mc.gameSettings.enableVsync = false;
                    mc.gameSettings.limitFramerate = 260; // 260 = Ilimitado
                    try { Display.setVSyncEnabled(false); } catch (Throwable ignored) {}
                }
                if (fastVBO.isEnabled()) {
                    mc.gameSettings.useVbo = true;
                }
                if (noEntityShadows.isEnabled()) {
                    mc.gameSettings.entityShadows = false;
                }
                if (noClouds.isEnabled()) {
                    mc.gameSettings.clouds = 0;
                }
                mc.gameSettings.mipmapLevels = 0;
                mc.gameSettings.ambientOcclusion = 0;
            }
        } catch (Throwable ignored) {}
    }
}
