package com.raidclient.mod.impl.autotext;

import org.lwjgl.input.Keyboard;

import java.util.UUID;

public class AutoTextMacro {

    private String id;
    private int key; // Tecla del teclado (> 0) o Botón del ratón (< 0, ej. -97 = Mouse 4, -96 = Mouse 5)
    private String message;
    private boolean enabled;

    public AutoTextMacro() {
        this.id = UUID.randomUUID().toString();
        this.key = Keyboard.KEY_NONE;
        this.message = "";
        this.enabled = true;
    }

    public AutoTextMacro(int key, String message, boolean enabled) {
        this.id = UUID.randomUUID().toString();
        this.key = key;
        this.message = message;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isMouseButton() {
        return key < 0;
    }

    public int getMouseButton() {
        return key < 0 ? key + 100 : -1;
    }

    public String getKeyName() {
        if (key == Keyboard.KEY_NONE || key == 0) return "NINGUNA";
        if (key < 0) {
            int mouseBtn = key + 100;
            switch (mouseBtn) {
                case 0: return "MOUSE 1";
                case 1: return "MOUSE 2";
                case 2: return "MOUSE 3";
                case 3: return "MOUSE 4"; // Botón lateral 1 (Atrás)
                case 4: return "MOUSE 5"; // Botón lateral 2 (Adelante)
                case 5: return "MOUSE 6";
                case 6: return "MOUSE 7";
                default: return "MOUSE " + (mouseBtn + 1);
            }
        }
        String name = Keyboard.getKeyName(key);
        return name != null ? name.toUpperCase() : "TECLA " + key;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}
