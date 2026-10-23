package com.raidclient.setting;

import java.awt.Color;

public class ColorSetting extends Setting {
    private int red;
    private int green;
    private int blue;
    private int alpha;

    public ColorSetting(String name, int red, int green, int blue, int alpha) {
        super(name);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public ColorSetting(String name, int rgb) {
        super(name);
        this.red = (rgb >> 16) & 0xFF;
        this.green = (rgb >> 8) & 0xFF;
        this.blue = rgb & 0xFF;
        this.alpha = (rgb >> 24) & 0xFF;
        if (this.alpha == 0) this.alpha = 255;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = Math.max(0, Math.min(255, red));
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = Math.max(0, Math.min(255, green));
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = Math.max(0, Math.min(255, blue));
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
    }

    public int getRGB() {
        return new Color(red, green, blue, alpha).getRGB();
    }

    public void setRGB(int rgb) {
        this.red = (rgb >> 16) & 0xFF;
        this.green = (rgb >> 8) & 0xFF;
        this.blue = rgb & 0xFF;
        this.alpha = (rgb >> 24) & 0xFF;
        if (this.alpha == 0) this.alpha = 255;
    }

    public float getRedFloat() {
        return red / 255.0f;
    }

    public float getGreenFloat() {
        return green / 255.0f;
    }

    public float getBlueFloat() {
        return blue / 255.0f;
    }

    public float getAlphaFloat() {
        return alpha / 255.0f;
    }
}
