package com.raidclient.mixin.client;

import com.raidclient.RaidClient;
import com.raidclient.mod.impl.FreelookMod;
import com.raidclient.mod.impl.NoFogMod;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V"))
    private void onUpdateCameraAngles(EntityPlayerSP entityPlayerSP, float yaw, float pitch) {
        if (FreelookMod.isActive()) {
            FreelookMod.onMouseTurn(yaw, pitch);
        } else {
            entityPlayerSP.setAngles(yaw, pitch);
        }
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    private float redirectYaw(Entity entity) {
        if (FreelookMod.isActive()) {
            return FreelookMod.cameraYaw;
        }
        return entity.rotationYaw;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F"))
    private float redirectPrevYaw(Entity entity) {
        if (FreelookMod.isActive()) {
            return FreelookMod.cameraYaw;
        }
        return entity.prevRotationYaw;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
    private float redirectPitch(Entity entity) {
        if (FreelookMod.isActive()) {
            return FreelookMod.cameraPitch;
        }
        return entity.rotationPitch;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F"))
    private float redirectPrevPitch(Entity entity) {
        if (FreelookMod.isActive()) {
            return FreelookMod.cameraPitch;
        }
        return entity.prevRotationPitch;
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void onSetupFog(int startCoords, float partialTicks, CallbackInfo ci) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            NoFogMod mod = RaidClient.INSTANCE.getModManager().getMod(NoFogMod.class);
            if (mod != null && mod.isEnabled()) {
                GlStateManager.disableFog();
                GlStateManager.setFogStart(999999.0F);
                GlStateManager.setFogEnd(9999999.0F);
            }
        }
    }
}
