package com.raidclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raidclient.RaidClient;
import com.raidclient.mod.Mod;
import com.raidclient.mod.impl.AutoTextMod;
import com.raidclient.mod.impl.waypoints.WaypointsMod;
import com.raidclient.setting.*;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {

    private final File configDir;
    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ConfigManager() {
        Minecraft mc = Minecraft.getMinecraft();
        this.configDir = new File(mc.mcDataDir, "raidclient");
        this.configFile = new File(configDir, "config.json");

        // Registrar shutdown hook para guardar todo si se cierra la ventana directamente
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveConfig, "RaidClient-Save-Hook"));
    }

    public void saveConfig() {
        try {
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            if (RaidClient.INSTANCE == null || RaidClient.INSTANCE.getModManager() == null) {
                return;
            }

            JsonObject root = new JsonObject();
            JsonObject modsObj = new JsonObject();

            for (Mod mod : RaidClient.INSTANCE.getModManager().getMods()) {
                JsonObject modJson = new JsonObject();
                modJson.addProperty("enabled", mod.isEnabled());
                modJson.addProperty("key", mod.getKey());
                modJson.addProperty("x", mod.getX());
                modJson.addProperty("y", mod.getY());

                JsonObject settingsJson = new JsonObject();
                for (Setting s : mod.getSettings()) {
                    if (s instanceof BooleanSetting) {
                        settingsJson.addProperty(s.getName(), ((BooleanSetting) s).isEnabled());
                    } else if (s instanceof NumberSetting) {
                        settingsJson.addProperty(s.getName(), ((NumberSetting) s).getValue());
                    } else if (s instanceof ColorSetting) {
                        settingsJson.addProperty(s.getName(), ((ColorSetting) s).getRGB());
                    } else if (s instanceof ModeSetting) {
                        settingsJson.addProperty(s.getName(), ((ModeSetting) s).getMode());
                    }
                }
                modJson.add("settings", settingsJson);
                modsObj.add(mod.getName(), modJson);
            }

            root.add("mods", modsObj);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }

            // Guardar también macros y waypoints
            if (AutoTextMod.INSTANCE != null) {
                AutoTextMod.INSTANCE.saveMacros();
            }
            if (WaypointsMod.INSTANCE != null) {
                WaypointsMod.INSTANCE.saveWaypoints();
            }

        } catch (Throwable t) {
            System.err.println("[RaidClient] Error al guardar config: " + t.getMessage());
        }
    }

    public void loadConfig() {
        try {
            if (!configFile.exists()) {
                return;
            }

            if (RaidClient.INSTANCE == null || RaidClient.INSTANCE.getModManager() == null) {
                return;
            }

            JsonObject root;
            try (FileReader reader = new FileReader(configFile)) {
                root = new JsonParser().parse(reader).getAsJsonObject();
            }

            if (!root.has("mods")) return;
            JsonObject modsObj = root.getAsJsonObject("mods");

            for (Mod mod : RaidClient.INSTANCE.getModManager().getMods()) {
                if (!modsObj.has(mod.getName())) continue;
                JsonObject modJson = modsObj.getAsJsonObject(mod.getName());

                if (modJson.has("enabled")) {
                    mod.setEnabled(modJson.get("enabled").getAsBoolean());
                }
                if (modJson.has("key")) {
                    mod.setKey(modJson.get("key").getAsInt());
                }
                if (modJson.has("x")) {
                    mod.setX(modJson.get("x").getAsFloat());
                }
                if (modJson.has("y")) {
                    mod.setY(modJson.get("y").getAsFloat());
                }

                if (modJson.has("settings")) {
                    JsonObject settingsJson = modJson.getAsJsonObject("settings");
                    for (Setting s : mod.getSettings()) {
                        if (!settingsJson.has(s.getName())) continue;

                        try {
                            if (s instanceof BooleanSetting) {
                                ((BooleanSetting) s).setEnabled(settingsJson.get(s.getName()).getAsBoolean());
                            } else if (s instanceof NumberSetting) {
                                ((NumberSetting) s).setValue(settingsJson.get(s.getName()).getAsDouble());
                            } else if (s instanceof ColorSetting) {
                                ((ColorSetting) s).setRGB(settingsJson.get(s.getName()).getAsInt());
                            } else if (s instanceof ModeSetting) {
                                ((ModeSetting) s).setMode(settingsJson.get(s.getName()).getAsString());
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }

            System.out.println("[RaidClient] Configuración cargada correctamente desde " + configFile.getAbsolutePath());

        } catch (Throwable t) {
            System.err.println("[RaidClient] Error al cargar config: " + t.getMessage());
        }
    }
}
