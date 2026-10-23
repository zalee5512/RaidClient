package com.raidclient.util;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class RenderUtils {

    public static void drawRect(float x, float y, float width, float height, int color) {
        float[] c = ColorUtils.getRGBAFloats(color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(c[0], c[1], c[2], c[3]);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawBorderedRect(float x, float y, float width, float height, float lineWidth, int insideColor, int borderColor) {
        drawRect(x, y, width, height, insideColor);
        drawOutline(x, y, width, height, lineWidth, borderColor);
    }

    public static void drawOutline(float x, float y, float width, float height, float lineWidth, int color) {
        float[] c = ColorUtils.getRGBAFloats(color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        tessellator.draw();

        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        float[] c = ColorUtils.getRGBAFloats(color);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(c[0], c[1], c[2], c[3]);

        // Center body
        drawRectInternal(x + radius, y, width - radius * 2, height);
        // Left & right sides
        drawRectInternal(x, y + radius, radius, height - radius * 2);
        drawRectInternal(x + width - radius, y + radius, radius, height - radius * 2);

        // Rounded corners
        drawQuarterCircle(x + radius, y + radius, radius, 180, 270);
        drawQuarterCircle(x + width - radius, y + radius, radius, 270, 360);
        drawQuarterCircle(x + width - radius, y + height - radius, radius, 0, 90);
        drawQuarterCircle(x + radius, y + height - radius, radius, 90, 180);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void drawRectInternal(float x, float y, float width, float height) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();
    }

    private static void drawQuarterCircle(float x, float y, float radius, int startAngle, int endAngle) {
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(x, y);
        for (int i = startAngle; i <= endAngle; i += 6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(rad) * radius, y + Math.cos(rad) * radius);
        }
        GL11.glEnd();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        float[] c = ColorUtils.getRGBAFloats(color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(thickness);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();

        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
