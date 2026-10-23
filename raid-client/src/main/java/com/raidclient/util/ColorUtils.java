package com.raidclient.util;

import java.awt.Color;

public class ColorUtils {

    public static Color getRainbow(float seconds, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() % (int)(seconds * 1000)) / (seconds * 1000f));
        return Color.getHSBColor(hue, saturation, brightness);
    }

    public static int toRGBA(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                (b & 0xFF);
    }

    public static float[] getRGBAFloats(int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        return new float[]{red, green, blue, alpha};
    }
}
