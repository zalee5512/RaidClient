package com.raidclient.mod.impl;

import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.setting.NumberSetting;
import com.raidclient.util.ColorUtils;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import org.lwjgl.opengl.GL11;

public class BlockOverlayMod extends Mod {

    public final BooleanSetting outline = new BooleanSetting("Borde", true);
    public final BooleanSetting fill = new BooleanSetting("Relleno Translúcido", true);
    public final BooleanSetting chroma = new BooleanSetting("Chroma RGB", false);
    public final NumberSetting lineWidth = new NumberSetting("Grosor Borde", 2.0, 1.0, 5.0, 0.5);
    public final ColorSetting outlineColor = new ColorSetting("Color Borde", 0, 229, 255, 255);
    public final ColorSetting fillColor = new ColorSetting("Color Relleno", 0, 229, 255, 45);

    public BlockOverlayMod() {
        super("Block Overlay", "Resalta el bloque apuntado con borde y relleno de color personalizado", Category.RENDER);
        addSettings(outline, fill, chroma, lineWidth, outlineColor, fillColor);
        setEnabled(true);
    }

    public void renderOverlay(DrawBlockHighlightEvent event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) return;
        if (event.target == null || event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        BlockPos pos = event.target.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null) return;

        block.setBlockBoundsBasedOnState(mc.theWorld, pos);

        double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
        double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;

        AxisAlignedBB box = block.getSelectedBoundingBox(mc.theWorld, pos).expand(0.002D, 0.002D, 0.002D).offset(-x, -y, -z);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        int r, g, b;
        if (chroma.isEnabled()) {
            java.awt.Color rainbow = ColorUtils.getRainbow(4.0F, 0.8F, 1.0F);
            r = rainbow.getRed();
            g = rainbow.getGreen();
            b = rainbow.getBlue();
        } else {
            r = outlineColor.getRed();
            g = outlineColor.getGreen();
            b = outlineColor.getBlue();
        }

        // Fill Box
        if (fill.isEnabled()) {
            int fa = fillColor.getAlpha();
            drawFilledBox(box, r / 255.0F, g / 255.0F, b / 255.0F, fa / 255.0F);
        }

        // Outline Box
        if (outline.isEnabled()) {
            GL11.glLineWidth(lineWidth.getValueFloat());
            int oa = outlineColor.getAlpha();
            RenderGlobal.drawOutlinedBoundingBox(box, r, g, b, oa);
            GL11.glLineWidth(1.0F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawFilledBox(AxisAlignedBB bb, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        // Bottom
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();

        // Top
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();

        // North
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();

        // South
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();

        // West
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();

        // East
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();

        tessellator.draw();
    }
}
