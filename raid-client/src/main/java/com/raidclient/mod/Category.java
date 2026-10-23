package com.raidclient.mod;

public enum Category {
    HUD("HUD", "Elementos visuales en pantalla"),
    PVP("PvP", "Mejoras para combate y PvP"),
    RENDER("Render", "Personalización visual y del juego"),
    UTILITY("Utilidades", "Optimizaciones y comodidades");

    private final String displayName;
    private final String description;

    Category(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
