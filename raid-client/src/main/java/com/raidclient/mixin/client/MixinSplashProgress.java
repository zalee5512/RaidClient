package com.raidclient.mixin.client;

import com.raidclient.gui.RaidSplashScreen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SplashProgress.class, remap = false)
public abstract class MixinSplashProgress {

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private static void onStart(CallbackInfo ci) {
        ci.cancel();
        RaidSplashScreen.start();
    }

    @Inject(method = "pause", at = @At("HEAD"), cancellable = true)
    private static void onPause(CallbackInfo ci) {
        ci.cancel();
        RaidSplashScreen.pause();
    }

    @Inject(method = "resume", at = @At("HEAD"), cancellable = true)
    private static void onResume(CallbackInfo ci) {
        ci.cancel();
        RaidSplashScreen.resume();
    }

    @Inject(method = "finish", at = @At("HEAD"), cancellable = true)
    private static void onFinish(CallbackInfo ci) {
        ci.cancel();
        RaidSplashScreen.finish();
    }

    @Inject(method = "drawVanillaScreen", at = @At("HEAD"), cancellable = true)
    private static void onDrawVanillaScreen(TextureManager tm, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "clearVanillaResources", at = @At("HEAD"), cancellable = true)
    private static void onClearVanillaResources(TextureManager tm, ResourceLocation rl, CallbackInfo ci) {
        ci.cancel();
    }
}
