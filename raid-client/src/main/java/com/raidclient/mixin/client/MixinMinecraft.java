package com.raidclient.mixin.client;

import com.raidclient.RaidClient;
import com.raidclient.event.impl.EventKey;
import com.raidclient.event.impl.EventMouse;
import com.raidclient.event.impl.EventTick;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "drawSplashScreen", at = @At("HEAD"), cancellable = true)
    public void onDrawSplashScreen(TextureManager textureManager, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "createDisplay", at = @At("RETURN"))
    public void onCreateDisplay(CallbackInfo ci) {
        Display.setTitle("Raid Client 1.8.9 | PvP & Performance");
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getEventManager() != null) {
            RaidClient.INSTANCE.getEventManager().call(new EventTick());
        }
    }

    @Inject(method = "dispatchKeypresses", at = @At("HEAD"))
    public void onKeyPress(CallbackInfo ci) {
        int key = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        if (Keyboard.getEventKeyState() && RaidClient.INSTANCE != null && RaidClient.INSTANCE.getEventManager() != null) {
            RaidClient.INSTANCE.getEventManager().call(new EventKey(key));
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void onShutdown(CallbackInfo ci) {
        if (RaidClient.INSTANCE != null) {
            RaidClient.INSTANCE.stop();
        }
    }
}
