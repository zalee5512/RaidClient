package com.raidclient.mod.impl;

import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.setting.NumberSetting;

public class HitboxMod extends Mod {

    public final BooleanSetting showAlways = new BooleanSetting("Siempre Visible", true);
    public final BooleanSetting onlyPlayers = new BooleanSetting("Solo Jugadores", true);
    public final ColorSetting outlineColor = new ColorSetting("Color Borde", 255, 255, 255, 255);
    public final NumberSetting lineWidth = new NumberSetting("Grosor Línea", 2.0, 1.0, 5.0, 0.5);
    public final BooleanSetting fill = new BooleanSetting("Rellenar", true);
    public final ColorSetting fillColor = new ColorSetting("Color Relleno", 255, 0, 0, 40);
    public final BooleanSetting eyeVector = new BooleanSetting("Línea de Mirada", true);
    public final ColorSetting eyeColor = new ColorSetting("Color Mirada", 0, 255, 255, 255);

    public HitboxMod() {
        super("Hitboxes", "Muestra y personaliza las cajas de colisión y línea de mirada", Category.PVP);
        addSettings(showAlways, onlyPlayers, outlineColor, lineWidth, fill, fillColor, eyeVector, eyeColor);
        setEnabled(false);
    }
}
