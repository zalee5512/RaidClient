package com.raidclient.mod.impl;

import com.raidclient.RaidClient;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ModeSetting;
import org.lwjgl.input.Keyboard;

public class FreelookMod extends Mod {

    private static boolean active = false;
    public static float cameraYaw = 0.0F;
    public static float cameraPitch = 0.0F;

    private int prevPerspective = 0;
    private boolean wasDown = false;

    public final ModeSetting mode = new ModeSetting("Modo", "Mantener", "Mantener", "Alternar");
    public final BooleanSetting invertX = new BooleanSetting("Invertir X", false);
    public final BooleanSetting invertY = new BooleanSetting("Invertir Y", false);

    public FreelookMod() {
        super("Freelook (ALT)", "Permite rotar la cámara 360° en tercera persona con la tecla ALT", Category.RENDER);
        addSettings(mode, invertX, invertY);
        setEnabled(true);
    }

    public static boolean isActive() {
        return active;
    }

    public static void onMouseTurn(float yaw, float pitch) {
        if (!active) return;

        FreelookMod mod = null;
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            mod = RaidClient.INSTANCE.getModManager().getMod(FreelookMod.class);
        }

        float multX = (mod != null && mod.invertX.isEnabled()) ? -1.0F : 1.0F;
        float multY = (mod != null && mod.invertY.isEnabled()) ? -1.0F : 1.0F;

        cameraYaw += yaw * 0.15F * multX;
        cameraPitch -= pitch * 0.15F * multY;

        if (cameraPitch > 90.0F) cameraPitch = 90.0F;
        if (cameraPitch < -90.0F) cameraPitch = -90.0F;
    }

    public void handleKey(boolean isDown) {
        if (mc.thePlayer == null || mc.currentScreen != null) {
            if (active) disableFreelook();
            wasDown = isDown;
            return;
        }

        if (mode.is("Mantener")) {
            if (isDown && !active) {
                enableFreelook();
            } else if (!isDown && active) {
                disableFreelook();
            }
        } else { // Alternar
            if (isDown && !wasDown) {
                if (active) {
                    disableFreelook();
                } else {
                    enableFreelook();
                }
            }
        }
        wasDown = isDown;
    }

    public void enableFreelook() {
        if (mc.thePlayer == null || mc.gameSettings == null) return;
        active = true;
        cameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = mc.thePlayer.rotationPitch;
        prevPerspective = mc.gameSettings.thirdPersonView;
        mc.gameSettings.thirdPersonView = 1;
    }

    public void disableFreelook() {
        active = false;
        if (mc.gameSettings != null) {
            mc.gameSettings.thirdPersonView = prevPerspective;
        }
    }

    @Override
    public void onDisable() {
        disableFreelook();
    }
}
