package com.raidclient.mod.impl;

import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;

public class TwoDItemsMod extends Mod {

    public final BooleanSetting billboard = new BooleanSetting("Mirar a Cámara", true);
    public final BooleanSetting singleLayer = new BooleanSetting("Capa Única (Max FPS)", true);

    public TwoDItemsMod() {
        super("2D Items", "Renderiza ítems caídos en 2D plano optimizando drásticamente los FPS en peleas", Category.RENDER);
        addSettings(billboard, singleLayer);
        setEnabled(true);
    }
}
