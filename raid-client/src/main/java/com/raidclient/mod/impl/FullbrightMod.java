package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventTick;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.ModeSetting;
import com.raidclient.setting.NumberSetting;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class FullbrightMod extends Mod {

    public final ModeSetting mode = new ModeSetting("Modo", "Gamma", "Gamma", "Efecto");
    public final NumberSetting brightness = new NumberSetting("Brillo Gamma", 100.0, 10.0, 1000.0, 10.0);

    private float previousGamma = 1.0f;

    public FullbrightMod() {
        super("Fullbright", "Ilumina el mundo al máximo sin necesidad de antorchas ni oscuridad", Category.RENDER);
        addSettings(mode, brightness);
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings != null) {
            previousGamma = mc.gameSettings.gammaSetting;
            if (mode.is("Gamma")) {
                mc.gameSettings.gammaSetting = brightness.getValueFloat();
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = previousGamma;
        }
        if (mc.thePlayer != null && mc.thePlayer.isPotionActive(Potion.nightVision)) {
            mc.thePlayer.removePotionEffect(Potion.nightVision.id);
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (!isEnabled()) return;

        if (mode.is("Gamma")) {
            if (mc.gameSettings != null && mc.gameSettings.gammaSetting < brightness.getValueFloat()) {
                mc.gameSettings.gammaSetting = brightness.getValueFloat();
            }
        } else if (mode.is("Efecto")) {
            if (mc.thePlayer != null) {
                // Apply subtle night vision effect without annoying particle effects
                mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 5200, 0, false, false));
            }
        }
    }
}
