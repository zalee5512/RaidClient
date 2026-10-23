package com.raidclient.mod.impl;

import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.setting.ModeSetting;
import com.raidclient.setting.NumberSetting;
import com.raidclient.util.ColorUtils;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class CrosshairMod extends Mod {

    public final ModeSetting shape = new ModeSetting("Forma", "Cruz", "Cruz", "Círculo", "Cuadrado", "Punto", "Cruz en T", "Flecha");
    public final ColorSetting color = new ColorSetting("Color Principal", 0, 255, 200, 255);
    public final NumberSetting size = new NumberSetting("Longitud", 5.0, 1.0, 20.0, 0.5);
    public final NumberSetting thickness = new NumberSetting("Grosor", 1.5, 0.5, 6.0, 0.5);
    public final NumberSetting gap = new NumberSetting("Separación (Gap)", 3.0, 0.0, 15.0, 0.5);
    public final BooleanSetting dot = new BooleanSetting("Punto Central", false);
    public final NumberSetting dotSize = new NumberSetting("Tamaño Punto", 2.0, 1.0, 6.0, 0.5);
    public final BooleanSetting outline = new BooleanSetting("Borde Exterior", true);
    public final ColorSetting outlineColor = new ColorSetting("Color Borde", 0, 0, 0, 180);
    public final BooleanSetting chroma = new BooleanSetting("Chroma RGB", false);
    public final BooleanSetting dynamic = new BooleanSetting("Dinámico", false);
    public final BooleanSetting highlightHostile = new BooleanSetting("Alerta de Enemigo", true);

    public CrosshairMod() {
        super("Custom Crosshair", "Crea tu propia mira personalizada: formas, colores, bordes y punto", Category.RENDER);
        addSettings(shape, color, size, thickness, gap, dot, dotSize, outline, outlineColor, chroma, dynamic, highlightHostile);
        setEnabled(true);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!isEnabled()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float cx = sr.getScaledWidth() / 2.0f;
        float cy = sr.getScaledHeight() / 2.0f;

        renderAt(cx, cy);
    }

    public void renderAt(float cx, float cy) {
        float currentGap = gap.getValueFloat();
        if (dynamic.isEnabled() && mc.thePlayer != null) {
            if (mc.thePlayer.isSprinting() || mc.thePlayer.motionY != 0) {
                currentGap += 2.5f;
            }
        }

        float s = size.getValueFloat();
        float t = thickness.getValueFloat();

        int renderColor;
        if (highlightHostile.isEnabled() && mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
            renderColor = 0xFFFF1744; // Rojo brillante al apuntar a un jugador
        } else if (chroma.isEnabled()) {
            renderColor = ColorUtils.getRainbow(4.0f, 0.8f, 1.0f).getRGB();
        } else {
            renderColor = color.getRGB();
        }

        int outC = outlineColor.getRGB();
        boolean hasOutline = outline.isEnabled();
        String currentShape = shape.getMode();

        switch (currentShape) {
            case "Cruz":
                drawCross(cx, cy, s, t, currentGap, renderColor, outC, hasOutline, true);
                break;
            case "Cruz en T":
                drawCross(cx, cy, s, t, currentGap, renderColor, outC, hasOutline, false);
                break;
            case "Círculo":
                drawCircleCrosshair(cx, cy, s, t, currentGap, renderColor, outC, hasOutline);
                break;
            case "Cuadrado":
                drawSquareCrosshair(cx, cy, s, t, currentGap, renderColor, outC, hasOutline);
                break;
            case "Punto":
                drawCenterDot(cx, cy, dotSize.getValueFloat(), renderColor, outC, hasOutline);
                break;
            case "Flecha":
                drawChevronCrosshair(cx, cy, s, t, currentGap, renderColor, outC, hasOutline);
                break;
            default:
                drawCross(cx, cy, s, t, currentGap, renderColor, outC, hasOutline, true);
                break;
        }

        // Punto central (si está activo y no es el modo exclusivo 'Punto')
        if (dot.isEnabled() && !currentShape.equals("Punto")) {
            drawCenterDot(cx, cy, dotSize.getValueFloat(), renderColor, outC, hasOutline);
        }
    }

    private void drawCross(float cx, float cy, float s, float t, float g, int col, int outC, boolean hasOut, boolean hasTop) {
        float ot = t + 1.5f;

        // Borde exterior
        if (hasOut) {
            if (hasTop) {
                RenderUtils.drawRect(cx - ot / 2.0f, cy - g - s - 0.75f, ot, s + 1.5f, outC);
            }
            // Abajo
            RenderUtils.drawRect(cx - ot / 2.0f, cy + g - 0.75f, ot, s + 1.5f, outC);
            // Izquierda
            RenderUtils.drawRect(cx - g - s - 0.75f, cy - ot / 2.0f, s + 1.5f, ot, outC);
            // Derecha
            RenderUtils.drawRect(cx + g - 0.75f, cy - ot / 2.0f, s + 1.5f, ot, outC);
        }

        // Líneas principales
        if (hasTop) {
            RenderUtils.drawRect(cx - t / 2.0f, cy - g - s, t, s, col);
        }
        RenderUtils.drawRect(cx - t / 2.0f, cy + g, t, s, col);
        RenderUtils.drawRect(cx - g - s, cy - t / 2.0f, s, t, col);
        RenderUtils.drawRect(cx + g, cy - t / 2.0f, s, t, col);
    }

    private void drawCircleCrosshair(float cx, float cy, float s, float t, float g, int col, int outC, boolean hasOut) {
        float radius = g + s;
        if (hasOut) {
            drawCircle(cx, cy, radius, t + 1.5f, outC);
        }
        drawCircle(cx, cy, radius, t, col);
    }

    private void drawCircle(float cx, float cy, float radius, float lineWidth, int col) {
        float a = (float) (col >> 24 & 255) / 255.0F;
        float r = (float) (col >> 16 & 255) / 255.0F;
        float g = (float) (col >> 8 & 255) / 255.0F;
        float b = (float) (col & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(Math.max(1.0F, lineWidth));
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 36; i++) {
            double angle = i * 2 * Math.PI / 36;
            GL11.glVertex2d(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawSquareCrosshair(float cx, float cy, float s, float t, float g, int col, int outC, boolean hasOut) {
        float r = g + s;
        float fullW = r * 2.0f;

        if (hasOut) {
            float ot = t + 1.5f;
            float off = ot / 2.0f;
            // Top
            RenderUtils.drawRect(cx - r - off, cy - r - off, fullW + ot, ot, outC);
            // Bottom
            RenderUtils.drawRect(cx - r - off, cy + r - off, fullW + ot, ot, outC);
            // Left
            RenderUtils.drawRect(cx - r - off, cy - r - off, ot, fullW + ot, outC);
            // Right
            RenderUtils.drawRect(cx + r - off, cy - r - off, ot, fullW + ot, outC);
        }

        float off = t / 2.0f;
        // Top
        RenderUtils.drawRect(cx - r, cy - r - off, fullW, t, col);
        // Bottom
        RenderUtils.drawRect(cx - r, cy + r - off, fullW, t, col);
        // Left
        RenderUtils.drawRect(cx - r - off, cy - r, t, fullW, col);
        // Right
        RenderUtils.drawRect(cx + r - off, cy - r, t, fullW, col);
    }

    private void drawChevronCrosshair(float cx, float cy, float s, float t, float g, int col, int outC, boolean hasOut) {
        if (hasOut) {
            drawChevronLines(cx, cy, s, t + 1.5f, g, outC);
        }
        drawChevronLines(cx, cy, s, t, g, col);
    }

    private void drawChevronLines(float cx, float cy, float s, float thickness, float g, int col) {
        float a = (float) (col >> 24 & 255) / 255.0F;
        float r = (float) (col >> 16 & 255) / 255.0F;
        float gr = (float) (col >> 8 & 255) / 255.0F;
        float b = (float) (col & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, gr, b, a);
        GL11.glLineWidth(Math.max(1.0F, thickness));
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        GL11.glBegin(GL11.GL_LINES);
        // Pata izquierda: de (cx - s, cy + g + s) a (cx, cy + g)
        GL11.glVertex2f(cx - s, cy + g + s);
        GL11.glVertex2f(cx, cy + g);
        // Pata derecha: de (cx, cy + g) a (cx + s, cy + g + s)
        GL11.glVertex2f(cx, cy + g);
        GL11.glVertex2f(cx + s, cy + g + s);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawCenterDot(float cx, float cy, float ds, int col, int outC, boolean hasOut) {
        if (hasOut) {
            RenderUtils.drawRect(cx - (ds + 1.5f) / 2.0f, cy - (ds + 1.5f) / 2.0f, ds + 1.5f, ds + 1.5f, outC);
        }
        RenderUtils.drawRect(cx - ds / 2.0f, cy - ds / 2.0f, ds, ds, col);
    }
}
