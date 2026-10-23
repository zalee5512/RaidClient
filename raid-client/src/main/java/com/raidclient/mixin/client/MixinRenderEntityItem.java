package com.raidclient.mixin.client;

import com.raidclient.RaidClient;
import com.raidclient.mod.impl.TwoDItemsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderEntityItem.class)
public abstract class MixinRenderEntityItem {

    @Inject(method = "func_177077_a", at = @At("HEAD"), cancellable = true)
    private void onTransformItem(EntityItem itemIn, double x, double y, double z, float partialTicks, IBakedModel model, CallbackInfoReturnable<Integer> cir) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            TwoDItemsMod mod = RaidClient.INSTANCE.getModManager().getMod(TwoDItemsMod.class);
            if (mod != null && mod.isEnabled()) {
                GlStateManager.translate((float) x, (float) y + 0.15F, (float) z);

                RenderManager rm = Minecraft.getMinecraft().getRenderManager();
                if (mod.billboard.isEnabled() && rm != null) {
                    GlStateManager.rotate(180.0F - rm.playerViewY, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(-rm.playerViewX, 1.0F, 0.0F, 0.0F);
                }

                // Scale flat in 2D sprite fashion
                GlStateManager.scale(0.5F, 0.5F, 0.001F);

                cir.setReturnValue(1); // 1 layer only, no duplicate models
            }
        }
    }
}
