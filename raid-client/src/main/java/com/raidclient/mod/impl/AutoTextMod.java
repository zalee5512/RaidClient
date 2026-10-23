package com.raidclient.mod.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventKey;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.mod.impl.autotext.AutoTextMacro;
import com.raidclient.setting.BooleanSetting;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AutoTextMod extends Mod {

    public static AutoTextMod INSTANCE;

    public final BooleanSetting showChatNotice = new BooleanSetting("Aviso al Ejecutar", false);

    private final List<AutoTextMacro> macros = new ArrayList<>();
    private final File storageFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AutoTextMod() {
        super("AutoText", "Ejecuta comandos o mensajes de chat totalmente personalizables con cualquier tecla", Category.UTILITY);
        INSTANCE = this;
        addSettings(showChatNotice);
        setEnabled(true);

        File dir = new File(mc.mcDataDir, "raidclient");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.storageFile = new File(dir, "autotext.json");
        loadMacros();
    }

    public List<AutoTextMacro> getMacros() {
        return macros;
    }

    public void addMacro(int key, String message) {
        macros.add(new AutoTextMacro(key, message, true));
        saveMacros();
    }

    public void removeMacro(AutoTextMacro macro) {
        macros.remove(macro);
        saveMacros();
    }

    @EventTarget
    public void onKey(EventKey event) {
        onRawKey(event.getKey());
    }

    public void onRawKey(int key) {
        if (!isEnabled() || mc.thePlayer == null || mc.currentScreen != null) return;
        if (key == Keyboard.KEY_NONE || key == 0) return;

        for (AutoTextMacro macro : macros) {
            if (macro.isEnabled() && macro.getKey() == key) {
                String msg = macro.getMessage();
                if (msg != null && !msg.trim().isEmpty()) {
                    mc.thePlayer.sendChatMessage(msg);
                }
            }
        }
    }

    public void loadMacros() {
        macros.clear();
        if (storageFile != null && storageFile.exists()) {
            try (FileReader reader = new FileReader(storageFile)) {
                Type type = new TypeToken<List<AutoTextMacro>>() {}.getType();
                List<AutoTextMacro> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    macros.addAll(loaded);
                    return; // Cargado exitosamente del archivo guardado
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Si es la primera vez que se inicia el juego y no hay archivo de guardado, añadir por defecto
        macros.add(new AutoTextMacro(Keyboard.KEY_H, "/f home", true));
        macros.add(new AutoTextMacro(Keyboard.KEY_K, "/kit pvp", true));
        macros.add(new AutoTextMacro(Keyboard.KEY_P, "/pot", true));
        macros.add(new AutoTextMacro(Keyboard.KEY_G, "gg", true));
        macros.add(new AutoTextMacro(Keyboard.KEY_B, "/f c f", false));
        saveMacros();
    }

    public void saveMacros() {
        try {
            File dir = storageFile.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(storageFile)) {
                gson.toJson(macros, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
