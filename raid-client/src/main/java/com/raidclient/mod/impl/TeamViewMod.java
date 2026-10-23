package com.raidclient.mod.impl;

import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;

public class TeamViewMod extends Mod {

    public final BooleanSetting detectScoreboard = new BooleanSetting("Mismo Scoreboard Team", true);
    public final BooleanSetting detectColorPrefix = new BooleanSetting("Mismo Color de Nombre", true);
    public final BooleanSetting showHealth = new BooleanSetting("Mostrar Vida (HP)", true);
    public final BooleanSetting showDistance = new BooleanSetting("Mostrar Distancia", true);
    public final BooleanSetting outline = new BooleanSetting("Caja 3D de Compañero", true);
    public final ColorSetting teamColor = new ColorSetting("Color de Equipo", 0, 229, 255, 200);

    private final Set<String> friends = new HashSet<>();

    public TeamViewMod() {
        super("Team View", "Resalta compañeros de equipo, facción y amigos con indicadores 3D y vida", Category.PVP);
        addSettings(detectScoreboard, detectColorPrefix, showHealth, showDistance, outline, teamColor);
        setEnabled(true);
    }

    public boolean isTeammate(EntityPlayer player) {
        if (player == null || mc.thePlayer == null || player == mc.thePlayer) return false;

        String name = player.getName().toLowerCase();
        if (friends.contains(name)) return true;

        if (detectScoreboard.isEnabled() && mc.thePlayer.isOnSameTeam(player)) {
            return true;
        }

        if (detectColorPrefix.isEnabled()) {
            String myPrefix = mc.thePlayer.getDisplayName().getFormattedText();
            String otherPrefix = player.getDisplayName().getFormattedText();
            if (myPrefix.length() >= 2 && otherPrefix.length() >= 2) {
                if (myPrefix.charAt(0) == '§' && otherPrefix.charAt(0) == '§') {
                    if (myPrefix.charAt(1) == otherPrefix.charAt(1) && (myPrefix.charAt(1) == 'a' || myPrefix.charAt(1) == 'b' || myPrefix.charAt(1) == '2' || myPrefix.charAt(1) == '3')) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void addFriend(String name) {
        friends.add(name.toLowerCase());
    }

    public void removeFriend(String name) {
        friends.remove(name.toLowerCase());
    }

    public Set<String> getFriends() {
        return friends;
    }

    public void render3D(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) return;

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isTeammate(player)) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - renderPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - renderPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - renderPosZ;

            if (outline.isEnabled()) {
                GlStateManager.depthMask(false);
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

                float w = player.width / 2.0F;
                AxisAlignedBB bb = new AxisAlignedBB(x - w, y, z - w, x + w, y + player.height, z + w);

                GL11.glLineWidth(2.0F);
                RenderGlobal.drawOutlinedBoundingBox(
                        bb,
                        teamColor.getRed(),
                        teamColor.getGreen(),
                        teamColor.getBlue(),
                        teamColor.getAlpha()
                );
                GL11.glLineWidth(1.0F);

                GlStateManager.enableLighting();
                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
                GlStateManager.depthMask(true);
            }
        }
    }
}
