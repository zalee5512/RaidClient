package com.raidclient.mod;

import com.raidclient.mod.impl.*;
import com.raidclient.mod.impl.waypoints.WaypointsMod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModManager {
    private final List<Mod> mods = new ArrayList<>();

    public void init() {
        // PvP
        register(new HitboxMod());
        register(new TeamViewMod());

        // Render
        register(new CrosshairMod());
        register(new FullbrightMod());
        register(new FPSBoostMod());
        register(new WaypointsMod());
        register(new TwoDItemsMod());
        register(new FreelookMod());
        register(new OldVisualsMod());
        register(new BlockOverlayMod());
        register(new NoFogMod());
        register(new NametagMod());
        register(new ZoomMod());

        // HUD
        register(new FPSMod());
        register(new CPSMod());
        register(new KeystrokesMod());
        register(new PotionEffectsMod());
        register(new ScoreboardMod());
        register(new ToggleSprintMod());

        // Utility
        register(new AutoTextMod());
    }

    private void register(Mod mod) {
        mods.add(mod);
    }

    public List<Mod> getMods() {
        return mods;
    }

    public List<Mod> getModsByCategory(Category category) {
        return mods.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<Mod> getHUDMods() {
        return mods.stream()
                .filter(Mod::isHUDMod)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Mod> T getMod(Class<T> clazz) {
        return (T) mods.stream()
                .filter(m -> m.getClass() == clazz)
                .findFirst()
                .orElse(null);
    }

    public Mod getModByName(String name) {
        return mods.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
