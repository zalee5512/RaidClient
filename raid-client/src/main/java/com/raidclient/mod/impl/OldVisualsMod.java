package com.raidclient.mod.impl;

import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;

public class OldVisualsMod extends Mod {

    public final BooleanSetting blockhit = new BooleanSetting("1.7 Blockhit", true);
    public final BooleanSetting bow = new BooleanSetting("1.7 Posición de Arco", true);
    public final BooleanSetting rod = new BooleanSetting("1.7 Posición de Caña", true);
    public final BooleanSetting itemHeldFix = new BooleanSetting("Sin Salto de Ítem", true);
    public final BooleanSetting armorRed = new BooleanSetting("1.7 Daño Rojo Armadura", true);

    public OldVisualsMod() {
        super("1.7 Visuals", "Restaura las animaciones clásicas de Minecraft 1.7 (Blockhit, arco, caña, etc.)", Category.RENDER);
        addSettings(blockhit, bow, rod, itemHeldFix, armorRed);
        setEnabled(true);
    }
}
