package com.raidclient.mixin.client;

import com.raidclient.RaidClient;
import com.raidclient.mod.impl.HitboxMod;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public abstract class MixinRenderManager {

    @Shadow private boolean debugBoundingBox;
    @Shadow protected abstract void renderDebugBoundingBox(Entity entityIn, double x, double y, double z, float entityYaw, float partialTicks);

    @Inject(method = "doRenderEntity", at = @At("RETURN"))
    public void onDoRenderEntity(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, boolean p_147939_10_, CallbackInfoReturnable<Boolean> cir) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            HitboxMod hitboxMod = RaidClient.INSTANCE.getModManager().getMod(HitboxMod.class);
            if (hitboxMod != null && hitboxMod.isEnabled() && hitboxMod.showAlways.isEnabled() && !this.debugBoundingBox) {
                if (hitboxMod.onlyPlayers.isEnabled() && !(entity instanceof EntityPlayer)) {
                    return;
                }
                this.renderDebugBoundingBox(entity, x, y, z, entityYaw, partialTicks);
            }
        }
    }

    @Inject(method = "renderDebugBoundingBox", at = @At("HEAD"), cancellable = true)
    public void onRenderDebugBoundingBox(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            HitboxMod hitboxMod = RaidClient.INSTANCE.getModManager().getMod(HitboxMod.class);
            if (hitboxMod != null && hitboxMod.isEnabled()) {
                ci.cancel();

                GlStateManager.depthMask(false);
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.disableCull();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

                float w = entity.width / 2.0F;
                AxisAlignedBB bb = new AxisAlignedBB(
                        x - (double)w, y, z - (double)w,
                        x + (double)w, y + (double)entity.height, z + (double)w
                );

                // Custom line width & outline color
                GL11.glLineWidth(hitboxMod.lineWidth.getValueFloat());
                int r = hitboxMod.outlineColor.getRed();
                int g = hitboxMod.outlineColor.getGreen();
                int b = hitboxMod.outlineColor.getBlue();
                int a = hitboxMod.outlineColor.getAlpha();
                RenderGlobal.drawOutlinedBoundingBox(bb, r, g, b, a);

                // Eye line vector
                if (hitboxMod.eyeVector.isEnabled()) {
                    Vec3 look = entity.getLook(partialTicks);
                    int er = hitboxMod.eyeColor.getRed();
                    int eg = hitboxMod.eyeColor.getGreen();
                    int eb = hitboxMod.eyeColor.getBlue();
                    int ea = hitboxMod.eyeColor.getAlpha();

                    GL11.glColor4ub((byte) er, (byte) eg, (byte) eb, (byte) ea);
                    GL11.glBegin(GL11.GL_LINES);
                    GL11.glVertex3d(x, y + entity.getEyeHeight(), z);
                    GL11.glVertex3d(x + look.xCoord * 2.0D, y + entity.getEyeHeight() + look.yCoord * 2.0D, z + look.zCoord * 2.0D);
                    GL11.glEnd();
                }

                GL11.glLineWidth(1.0F);
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.enableCull();
                GlStateManager.disableBlend();
                GlStateManager.depthMask(true);
            }
        }
    }
}
