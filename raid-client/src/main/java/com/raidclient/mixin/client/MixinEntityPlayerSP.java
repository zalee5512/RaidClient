package com.raidclient.mixin.client;

import com.raidclient.RaidClient;
import com.raidclient.mod.impl.ToggleSprintMod;
import com.raidclient.mod.impl.waypoints.WaypointsMod;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    public void onLivingUpdate(CallbackInfo ci) {
        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
            ToggleSprintMod sprintMod = RaidClient.INSTANCE.getModManager().getMod(ToggleSprintMod.class);
            if (sprintMod != null && sprintMod.isEnabled()) {
                EntityPlayerSP player = (EntityPlayerSP) (Object) this;
                if (player.movementInput.moveForward > 0.0F && !player.isSneaking() && !player.isCollidedHorizontally) {
                    player.setSprinting(true);
                }
            }
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    public void onSendChatMessage(String message, CallbackInfo ci) {
        if (message == null) return;
        String clean = message.trim().toLowerCase();

        // Detectar si el usuario envía /f rally o /f r
        if (clean.equals("/f rally") || clean.equals("/f r") || clean.equals("/faction rally") || clean.equals("/f setrally") || clean.equals("/f rally here")) {
            if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
                WaypointsMod waypointsMod = RaidClient.INSTANCE.getModManager().getMod(WaypointsMod.class);
                if (waypointsMod != null && waypointsMod.isEnabled()) {
                    EntityPlayerSP player = (EntityPlayerSP) (Object) this;
                    waypointsMod.setRally(Math.floor(player.posX), Math.floor(player.posY), Math.floor(player.posZ));
                }
            }
        } else if (clean.startsWith("/f rally ") || clean.startsWith("/f r ")) {
            String sub = clean.substring(clean.indexOf(' ') + 1).trim();
            if (sub.equals("unmark") || sub.equals("remove") || sub.equals("clear")) {
                if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
                    WaypointsMod waypointsMod = RaidClient.INSTANCE.getModManager().getMod(WaypointsMod.class);
                    if (waypointsMod != null) {
                        waypointsMod.removeRally();
                    }
                }
            } else {
                // Coordenadas manuales si escribe por ejemplo: /f rally 100 70 -200
                String[] parts = sub.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        double x = Double.parseDouble(parts[0]);
                        double y = Double.parseDouble(parts[1]);
                        double z = Double.parseDouble(parts[2]);
                        if (RaidClient.INSTANCE != null && RaidClient.INSTANCE.getModManager() != null) {
                            WaypointsMod waypointsMod = RaidClient.INSTANCE.getModManager().getMod(WaypointsMod.class);
                            if (waypointsMod != null && waypointsMod.isEnabled()) {
                                waypointsMod.setRally(x, y, z);
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}
