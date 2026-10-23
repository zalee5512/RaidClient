package com.raidclient.mixin.client;

import com.raidclient.RaidClient;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.mod.impl.CrosshairMod;
import com.raidclient.mod.impl.ScoreboardMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {

    // EventRender2D is rendered via Forge's RenderGameOverlayEvent.Post in RaidClient to avoid duplicate rendering

    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    public void onShowCrosshair(CallbackInfoReturnable<Boolean> cir) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            CrosshairMod crosshairMod = RaidClient.INSTANCE.getModManager().getMod(CrosshairMod.class);
            if (crosshairMod != null && crosshairMod.isEnabled()) {
                cir.setReturnValue(false); // Suppress default crosshair when Custom Crosshair is enabled
            }
        }
    }

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    public void onRenderScoreboard(ScoreObjective objective, ScaledResolution sr, CallbackInfo ci) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            ScoreboardMod scoreboardMod = RaidClient.INSTANCE.getModManager().getMod(ScoreboardMod.class);
            if (scoreboardMod != null && scoreboardMod.isEnabled()) {
                ci.cancel();
                scoreboardMod.renderScoreboard(sr);
            }
        }
    }
}
