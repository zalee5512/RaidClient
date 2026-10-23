package com.raidclient.mod;

import com.raidclient.RaidClient;
import com.raidclient.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Mod {
    protected final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int key;

    // HUD position & dimensions
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean dragging;
    private float dragX;
    private float dragY;

    private final List<Setting> settings = new ArrayList<>();

    public Mod(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
        this.key = 0;
        this.x = 10;
        this.y = 10;
        this.width = 50;
        this.height = 15;
    }

    public void addSettings(Setting... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
            RaidClient.INSTANCE.getEventManager().register(this);
        } else {
            onDisable();
            RaidClient.INSTANCE.getEventManager().unregister(this);
        }
    }

    public void onEnable() {}
    public void onDisable() {}

    public boolean isHUDMod() {
        return this.category == Category.HUD;
    }

    public void renderDummy(int mouseX, int mouseY) {
        renderHUD();
    }

    public void renderHUD() {}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public float getDragX() {
        return dragX;
    }

    public void setDragX(float dragX) {
        this.dragX = dragX;
    }

    public float getDragY() {
        return dragY;
    }

    public void setDragY(float dragY) {
        this.dragY = dragY;
    }

    public List<Setting> getSettings() {
        return settings;
    }
}
