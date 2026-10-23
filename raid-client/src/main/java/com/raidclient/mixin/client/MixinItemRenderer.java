package com.raidclient.mixin.client;

import com.raidclient.RaidClient;
import com.raidclient.mod.impl.OldVisualsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Shadow private ItemStack itemToRender;
    @Shadow protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    private float currentPartialTicks;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void onRenderItemHead(float partialTicks, CallbackInfo ci) {
        this.currentPartialTicks = partialTicks;
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V"))
    private void onTransformFirstPersonItem(ItemRenderer renderer, float equipProgress, float swingProgress) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            OldVisualsMod mod = RaidClient.INSTANCE.getModManager().getMod(OldVisualsMod.class);
            if (mod != null && mod.isEnabled() && mod.blockhit.isEnabled()) {
                EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
                if (player != null && player.getItemInUseCount() > 0 && player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem().getItemUseAction() == EnumAction.BLOCK) {
                    float realSwing = player.getSwingProgress(this.currentPartialTicks);
                    if (realSwing > 0.0F) {
                        this.transformFirstPersonItem(equipProgress, realSwing);
                        return;
                    }
                }
            }
        }
        this.transformFirstPersonItem(equipProgress, swingProgress);
    }

    @Inject(method = "doBowTransformations", at = @At("TAIL"))
    private void onBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer, CallbackInfo ci) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            OldVisualsMod mod = RaidClient.INSTANCE.getModManager().getMod(OldVisualsMod.class);
            if (mod != null && mod.isEnabled() && mod.bow.isEnabled()) {
                GlStateManager.translate(0.0F, 0.05F, 0.04F);
                GlStateManager.scale(0.93F, 1.0F, 1.0F);
            }
        }
    }

    @Inject(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;doItemUsedTransformations(F)V", shift = At.Shift.AFTER))
    private void onRodTransform(float partialTicks, CallbackInfo ci) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            OldVisualsMod mod = RaidClient.INSTANCE.getModManager().getMod(OldVisualsMod.class);
            if (mod != null && mod.isEnabled() && mod.rod.isEnabled() && this.itemToRender != null && this.itemToRender.getItem() instanceof ItemFishingRod) {
                GlStateManager.translate(0.08F, -0.027F, -0.15F);
                GlStateManager.scale(0.8F, 0.8F, 0.8F);
            }
        }
    }

    @Redirect(method = "updateEquippedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getIsItemStackEqual(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean onCheckItemEqual(ItemStack prev, ItemStack current) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            OldVisualsMod mod = RaidClient.INSTANCE.getModManager().getMod(OldVisualsMod.class);
            if (mod != null && mod.isEnabled() && mod.itemHeldFix.isEnabled()) {
                if (prev != null && current != null && prev.getItem() == current.getItem()) {
                    return true;
                }
            }
        }
        return prev != null && prev.getIsItemStackEqual(current);
    }
}
