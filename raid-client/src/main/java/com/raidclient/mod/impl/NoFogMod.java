package com.raidclient.mod.impl;

import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;

public class NoFogMod extends Mod {

    public final BooleanSetting waterFog = new BooleanSetting("Quitar en Agua/Lava", true);
    public final BooleanSetting distanceFog = new BooleanSetting("Quitar Niebla Distante", true);

    public NoFogMod() {
        super("No Fog (Anti-Niebla)", "Elimina por completo la niebla del mundo para una visibilidad total en PvP", Category.RENDER);
        addSettings(waterFog, distanceFog);
        setEnabled(true);
    }
}
