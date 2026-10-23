package com.raidclient.mod.impl.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventRender3D;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.NumberSetting;
import com.raidclient.util.ColorUtils;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WaypointsMod extends Mod {

    public static WaypointsMod INSTANCE;

    public final BooleanSetting showBeams = new BooleanSetting("Rayo de Luz (Beam)", true);
    public final BooleanSetting showDistance = new BooleanSetting("Mostrar Distancia", true);
    public final BooleanSetting autoKoths = new BooleanSetting("KoTHs Automáticos", true);
    public final BooleanSetting autoRally = new BooleanSetting("Auto Rally Facción", true);
    public final BooleanSetting throughWalls = new BooleanSetting("Ver tras paredes", true);
    public final NumberSetting beamHeight = new NumberSetting("Altura Rayo", 100, 20, 256, 10);

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final Set<String> initializedServers = new HashSet<>();
    private String lastServerIp = "";

    private final File storageFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public WaypointsMod() {
        super("Waypoints", "Marcadores 3D para KoTHs, Spawn y bases (Estilo Lunar Client)", Category.RENDER);
        INSTANCE = this;
        addSettings(showBeams, showDistance, autoKoths, autoRally, throughWalls, beamHeight);
        setEnabled(true);

        File dir = new File(mc.mcDataDir, "raidclient");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.storageFile = new File(dir, "waypoints.json");
        loadWaypoints();

        MinecraftForge.EVENT_BUS.register(this);
    }

    public String getCurrentServerIp() {
        if (mc.isSingleplayer()) return "singleplayer";
        ServerData data = mc.getCurrentServerData();
        if (data != null && data.serverIP != null && !data.serverIP.isEmpty()) {
            return data.serverIP.toLowerCase().trim();
        }
        return "default";
    }

    public void checkServerChange() {
        String current = getCurrentServerIp();
        if (!current.equalsIgnoreCase(lastServerIp)) {
            lastServerIp = current;
            if (autoKoths.isEnabled() && !initializedServers.contains(current)) {
                initializedServers.add(current);
                setupDefaultWaypointsForServer(current);
            }
        }
    }

    private void setupDefaultWaypointsForServer(String serverIp) {
        // If waypoints already exist for this server, don't duplicate
        boolean hasWaypoints = waypoints.stream().anyMatch(w -> w.getServer().equalsIgnoreCase(serverIp));
        if (hasWaypoints) return;

        // Auto-configure tactical KoTHs & Spawn according to server type
        // Standard HCF / PvP coordinates
        waypoints.add(new Waypoint("Spawn", 0, 70, 0, 0xFF00E5FF, serverIp, false)); // Cyan
        waypoints.add(new Waypoint("KoTH Citadel", 150, 72, 150, 0xFFFFD700, serverIp, true)); // Gold
        waypoints.add(new Waypoint("KoTH Palace", -150, 75, 150, 0xFFFF8C00, serverIp, true)); // Dark Orange
        waypoints.add(new Waypoint("KoTH Nether", 0, 80, -200, 0xFFFF3333, serverIp, true)); // Red
        waypoints.add(new Waypoint("Conquest", 200, 70, -200, 0xFFBA55D3, serverIp, true)); // Medium Orchid

        saveWaypoints();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) return;
        checkServerChange();

        String currentServer = getCurrentServerIp();
        List<Waypoint> activeWaypoints = waypoints.stream()
                .filter(Waypoint::isEnabled)
                .filter(w -> w.getServer().equalsIgnoreCase(currentServer) || w.getServer().equalsIgnoreCase("global"))
                .collect(Collectors.toList());

        if (activeWaypoints.isEmpty()) return;

        RenderManager rm = mc.getRenderManager();
        double renderPosX = rm.viewerPosX;
        double renderPosY = rm.viewerPosY;
        double renderPosZ = rm.viewerPosZ;

        for (Waypoint wp : activeWaypoints) {
            double dx = wp.getX() - renderPosX;
            double dy = wp.getY() - renderPosY;
            double dz = wp.getZ() - renderPosZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // ─── 1. Render Beacon Beam & Box ─────────────────────────────
            if (showBeams.isEnabled()) {
                renderBeaconBeam(dx, dy, dz, wp.getColor(), beamHeight.getValueFloat());
            }

            // ─── 2. Render 3D Floating NameTag ───────────────────────────
            renderWaypointTag(rm, dx, dy, dz, distance, wp);
        }
    }

    private void renderBeaconBeam(double x, double y, double z, int color, float height) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);

        // Ground base box (1x1x1)
        AxisAlignedBB bb = new AxisAlignedBB(x - 0.5, y, z - 0.5, x + 0.5, y + 1.0, z + 0.5);
        GL11.glLineWidth(2.0F);
        RenderGlobal.drawOutlinedBoundingBox(bb, (int)(r * 255), (int)(g * 255), (int)(b * 255), 200);

        // Vertical Beam
        float width = 0.25F;
        GL11.glColor4f(r, g, b, 0.35F);
        GL11.glBegin(GL11.GL_QUADS);

        // North face
        GL11.glVertex3d(x - width, y, z - width);
        GL11.glVertex3d(x + width, y, z - width);
        GL11.glVertex3d(x + width, y + height, z - width);
        GL11.glVertex3d(x - width, y + height, z - width);

        // South face
        GL11.glVertex3d(x - width, y, z + width);
        GL11.glVertex3d(x + width, y, z + width);
        GL11.glVertex3d(x + width, y + height, z + width);
        GL11.glVertex3d(x - width, y + height, z + width);

        // West face
        GL11.glVertex3d(x - width, y, z - width);
        GL11.glVertex3d(x - width, y, z + width);
        GL11.glVertex3d(x - width, y + height, z + width);
        GL11.glVertex3d(x - width, y + height, z - width);

        // East face
        GL11.glVertex3d(x + width, y, z - width);
        GL11.glVertex3d(x + width, y, z + width);
        GL11.glVertex3d(x + width, y + height, z + width);
        GL11.glVertex3d(x + width, y + height, z - width);

        GL11.glEnd();

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private void renderWaypointTag(RenderManager rm, double x, double y, double z, double distance, Waypoint wp) {
        String tag = wp.isKoth() ? "⚔ " + wp.getName() : wp.getName();
        if (wp.getName().equalsIgnoreCase("Rally")) {
            tag = "⚡ RALLY";
        }
        if (showDistance.isEnabled()) {
            tag += " [" + (int) distance + "m]";
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 2.2, z);

        // Billboard rotation facing player
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);

        // Distance scale so it stays readable regardless of distance
        float scale = (float) Math.max(0.022F, distance * 0.0035F);
        GlStateManager.scale(-scale, -scale, scale);

        if (throughWalls.isEnabled()) {
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int strWidth = mc.fontRendererObj.getStringWidth(tag);
        int halfWidth = strWidth / 2;

        // Background box
        RenderUtils.drawRoundedRect(-halfWidth - 4, -12, strWidth + 8, 14, 3, 0x880E1118);
        RenderUtils.drawOutline(-halfWidth - 4, -12, strWidth + 8, 14, 1.0F, wp.getColor());

        // Text
        mc.fontRendererObj.drawStringWithShadow(tag, -halfWidth, -9, wp.getColor());

        if (throughWalls.isEnabled()) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public void addWaypoint(String name, double x, double y, double z, int color, boolean isKoth) {
        String s = getCurrentServerIp();
        waypoints.removeIf(w -> w.getServer().equalsIgnoreCase(s) && w.getName().equalsIgnoreCase(name));
        waypoints.add(new Waypoint(name, x, y, z, color, s, isKoth));
        saveWaypoints();
    }

    public void setRally(double x, double y, double z) {
        String s = getCurrentServerIp();
        waypoints.removeIf(w -> (w.getServer().equalsIgnoreCase(s) || w.getServer().equalsIgnoreCase("global")) && w.getName().equalsIgnoreCase("Rally"));
        Waypoint rally = new Waypoint("Rally", x, y, z, 0xFFFFD700, s, false);
        waypoints.add(rally);
        saveWaypoints();

        if (mc.thePlayer != null) {
            double dx = x - mc.thePlayer.posX;
            double dy = y - mc.thePlayer.posY;
            double dz = z - mc.thePlayer.posZ;
            int dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    net.minecraft.util.EnumChatFormatting.GOLD + "[Raid Client] " +
                    net.minecraft.util.EnumChatFormatting.YELLOW + "⚡ Rally de facción fijado en " +
                    net.minecraft.util.EnumChatFormatting.WHITE + "[" + (int) x + ", " + (int) y + ", " + (int) z + "] " +
                    net.minecraft.util.EnumChatFormatting.GRAY + "(" + dist + "m)"
            ));
            try {
                mc.thePlayer.playSound("random.orb", 1.0F, 1.2F);
            } catch (Throwable ignored) {}
        }
    }

    public void removeRally() {
        String s = getCurrentServerIp();
        boolean removed = waypoints.removeIf(w -> (w.getServer().equalsIgnoreCase(s) || w.getServer().equalsIgnoreCase("global")) && w.getName().equalsIgnoreCase("Rally"));
        if (removed) {
            saveWaypoints();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.GOLD + "[Raid Client] " +
                        net.minecraft.util.EnumChatFormatting.GRAY + "⚡ Rally de facción eliminado."
                ));
            }
        }
    }

    private static final Pattern RALLY_COORD_PATTERN = Pattern.compile("(?:\\[|\\(|X:\\s*)?(-?\\d{1,6})[\\s,]+(?:Y:\\s*)?(-?\\d{1,3})[\\s,]+(?:Z:\\s*)?(-?\\d{1,6})(?:\\]|\\))?");

    public void handleChatMessage(String unformattedText) {
        if (!autoRally.isEnabled() || unformattedText == null) return;
        String lower = unformattedText.toLowerCase();

        // Si el mensaje del servidor indica eliminación de rally
        if (lower.contains("rally") && (lower.contains("remove") || lower.contains("unmark") || lower.contains("clear") || lower.contains("desmarcado") || lower.contains("eliminado") || lower.contains("expired"))) {
            removeRally();
            return;
        }

        // Si el mensaje contiene rally y coordenadas
        if (lower.contains("rally") || lower.contains("reunion") || lower.contains("punto de")) {
            Matcher m = RALLY_COORD_PATTERN.matcher(unformattedText);
            if (m.find()) {
                try {
                    double x = Double.parseDouble(m.group(1));
                    double y = Double.parseDouble(m.group(2));
                    double z = Double.parseDouble(m.group(3));
                    setRally(x, y, z);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public boolean removeWaypoint(String name) {
        String s = getCurrentServerIp();
        boolean removed = waypoints.removeIf(w -> w.getServer().equalsIgnoreCase(s) && w.getName().equalsIgnoreCase(name));
        if (removed) saveWaypoints();
        return removed;
    }

    public List<Waypoint> getWaypointsForCurrentServer() {
        String s = getCurrentServerIp();
        return waypoints.stream()
                .filter(w -> w.getServer().equalsIgnoreCase(s))
                .collect(Collectors.toList());
    }

    public void saveWaypoints() {
        try {
            if (!storageFile.getParentFile().exists()) {
                storageFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(storageFile)) {
                gson.toJson(waypoints, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWaypoints() {
        try {
            if (!storageFile.exists()) return;
            try (FileReader reader = new FileReader(storageFile)) {
                Type listType = new TypeToken<ArrayList<Waypoint>>(){}.getType();
                List<Waypoint> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    waypoints.clear();
                    waypoints.addAll(loaded);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
