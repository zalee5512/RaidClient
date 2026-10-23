package com.raidclient;

import com.raidclient.command.CommandAutoText;
import com.raidclient.command.CommandKoth;
import com.raidclient.command.CommandRaid;
import com.raidclient.command.CommandWaypoint;
import com.raidclient.config.ConfigManager;
import com.raidclient.event.EventManager;
import com.raidclient.event.EventTarget;
import com.raidclient.event.impl.EventKey;
import com.raidclient.event.impl.EventMouse;
import com.raidclient.event.impl.EventRender2D;
import com.raidclient.event.impl.EventTick;
import com.raidclient.gui.ClickGUI;
import com.raidclient.gui.RaidMainMenu;
import com.raidclient.gui.WaypointScreen;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import com.raidclient.mod.ModManager;
import com.raidclient.mod.impl.*;
import com.raidclient.mod.impl.waypoints.WaypointsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Session;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

@Mod(
    modid = RaidClient.MODID,
    name = RaidClient.NAME,
    version = RaidClient.VERSION,
    clientSideOnly = true,
    acceptedMinecraftVersions = "[1.8.9]"
)
public class RaidClient {
    public static final String MODID = "raidclient";
    public static final String NAME = "Raid Client";
    public static final String VERSION = "1.0-BETA";

    public static RaidClient INSTANCE;

    @NetworkCheckHandler
    public boolean checkModList(Map<String, String> versions, Side side) {
        return true;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private EventManager eventManager;
    private ModManager modManager;
    private ConfigManager configManager;
    private ClickGUI clickGUI;

    private boolean wasRShiftDown = false;
    private boolean wasMDown = false;

    public RaidClient() {
        INSTANCE = this;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        start();
    }

    public void start() {
        System.out.println("=========================================");
        System.out.println("  Iniciando " + NAME + " v" + VERSION);
        System.out.println("=========================================");

        this.eventManager = new EventManager();
        this.modManager = new ModManager();
        this.modManager.init();
        this.configManager = new ConfigManager();
        this.configManager.loadConfig();
        this.clickGUI = new ClickGUI();

        // Register to internal event bus
        this.eventManager.register(this);

        // Register to Forge Event Bus for guaranteed execution
        MinecraftForge.EVENT_BUS.register(this);

        // Register chat commands: /raid, /waypoint, /koth, /autotext
        try {
            ClientCommandHandler.instance.registerCommand(new CommandRaid());
            ClientCommandHandler.instance.registerCommand(new CommandWaypoint());
            ClientCommandHandler.instance.registerCommand(new CommandKoth());
            ClientCommandHandler.instance.registerCommand(new CommandAutoText());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sincronizar automáticamente la sesión activa con las credenciales del launcher
        updateSessionFromLauncher();

        // Desbloquear FPS y optimizar opciones de Minecraft en el inicio
        try {
            if (mc.gameSettings != null) {
                mc.gameSettings.enableVsync = false;
                mc.gameSettings.limitFramerate = 260; // 260 = Ilimitado
                mc.gameSettings.useVbo = true;
                mc.gameSettings.entityShadows = false;
                mc.gameSettings.mipmapLevels = 0;
                mc.gameSettings.clouds = 0;
                mc.gameSettings.ambientOcclusion = 0;
            }
            Display.setVSyncEnabled(false);
        } catch (Throwable ignored) {}

        System.out.println("[RaidClient] Inicializado correctamente. Presiona RSHIFT o escribe /raid.");
    }

    public void stop() {
        System.out.println("Deteniendo " + NAME + "...");
        if (this.configManager != null) {
            this.configManager.saveConfig();
        }
    }

    private long lastGuiToggleTime = 0;

    @EventTarget
    public void onKey(EventKey event) {
        if (event.getKey() == Keyboard.KEY_RSHIFT) {
            openClickGui();
        } else if (event.getKey() == Keyboard.KEY_M) {
            openWaypointGui();
        }
    }

    public void openClickGui() {
        long now = System.currentTimeMillis();
        if (now - lastGuiToggleTime < 200) return;
        lastGuiToggleTime = now;

        Runnable r = () -> {
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(clickGUI);
            } else if (mc.currentScreen == clickGUI) {
                mc.displayGuiScreen(null);
            }
        };

        if (mc.isCallingFromMinecraftThread()) {
            r.run();
        } else {
            mc.addScheduledTask(r);
        }
    }

    public void openWaypointGui() {
        long now = System.currentTimeMillis();
        if (now - lastGuiToggleTime < 200) return;
        lastGuiToggleTime = now;

        Runnable r = () -> {
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new WaypointScreen());
            } else if (mc.currentScreen instanceof WaypointScreen) {
                mc.displayGuiScreen(null);
            }
        };

        if (mc.isCallingFromMinecraftThread()) {
            r.run();
        } else {
            mc.addScheduledTask(r);
        }
    }

    @SubscribeEvent
    public void onKeyInput(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if (key == Keyboard.KEY_RSHIFT) {
                openClickGui();
            } else if (key == Keyboard.KEY_M) {
                openWaypointGui();
            }

            if (modManager != null) {
                AutoTextMod autoText = modManager.getMod(AutoTextMod.class);
                if (autoText != null && autoText.isEnabled()) {
                    autoText.onRawKey(key);
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent event) {
        int button = Mouse.getEventButton();
        boolean state = Mouse.getEventButtonState();

        if (button != -1) {
            // Disparar EventMouse exactamente una vez por evento de ratón físico
            if (eventManager != null) {
                eventManager.call(new EventMouse(button, state));
            }

            if (state) {
                int unifiedKey = -100 + button;
                if (modManager != null) {
                    AutoTextMod autoText = modManager.getMod(AutoTextMod.class);
                    if (autoText != null && autoText.isEnabled()) {
                        autoText.onRawKey(unifiedKey);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiMainMenu && !(event.gui instanceof RaidMainMenu)) {
            event.gui = new RaidMainMenu();
        } else if (event.gui instanceof net.minecraft.client.gui.GuiMultiplayer) {
            updateSessionFromLauncher();
        }
    }

    public void updateSessionFromLauncher() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null) return;
            File configFile = new File(appData, ".raidlauncher/config.json");
            if (!configFile.exists()) return;

            FileReader reader = new FileReader(configFile);
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            reader.close();

            if (!json.has("Accounts") || !json.has("SelectedAccountUuid")) return;

            String selectedUuid = json.get("SelectedAccountUuid").getAsString();
            JsonArray accounts = json.getAsJsonArray("Accounts");

            for (JsonElement elem : accounts) {
                JsonObject acc = elem.getAsJsonObject();
                String uuid = acc.has("Uuid") ? acc.get("Uuid").getAsString() : "";
                if (uuid.equalsIgnoreCase(selectedUuid)) {
                    String username = acc.get("Username").getAsString();
                    String accessToken = acc.has("AccessToken") ? acc.get("AccessToken").getAsString() : "0";
                    boolean isPremium = acc.has("IsPremium") && acc.get("IsPremium").getAsBoolean();

                    String type = isPremium ? "mojang" : "legacy";
                    Session session = new Session(username, uuid, accessToken, type);

                    Field sessionField = null;
                    for (Field f : Minecraft.class.getDeclaredFields()) {
                        if (f.getType() == Session.class) {
                            sessionField = f;
                            break;
                        }
                    }

                    if (sessionField != null) {
                        sessionField.setAccessible(true);
                        try {
                            Field modifiersField = Field.class.getDeclaredField("modifiers");
                            modifiersField.setAccessible(true);
                            modifiersField.setInt(sessionField, sessionField.getModifiers() & ~Modifier.FINAL);
                        } catch (Throwable ignored) {}

                        sessionField.set(mc, session);
                        System.out.println("[RaidClient] Sesión sincronizada con éxito: " + username + " (Premium: " + isPremium + ")");
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            System.err.println("[RaidClient] Error al sincronizar sesión desde launcher: " + t.getMessage());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Dispatch internal EventTick
        if (eventManager != null) {
            eventManager.call(new EventTick());
        }

        // Right Shift key press detection
        boolean isRShiftDown = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (isRShiftDown && !wasRShiftDown) {
            openClickGui();
        }
        wasRShiftDown = isRShiftDown;

        // 'M' key detection for Waypoints HUD
        boolean isMDown = Keyboard.isKeyDown(Keyboard.KEY_M);
        if (isMDown && !wasMDown && mc.currentScreen == null) {
            openWaypointGui();
        }
        wasMDown = isMDown;

        // Freelook ALT key detection
        if (modManager != null) {
            FreelookMod freelook = modManager.getMod(FreelookMod.class);
            if (freelook != null && freelook.isEnabled()) {
                boolean isAltDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
                freelook.handleKey(isAltDown);
            }
        }
    }

    @SubscribeEvent
    public void onClientChat(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        if (event.message != null && modManager != null) {
            WaypointsMod waypointsMod = modManager.getMod(WaypointsMod.class);
            if (waypointsMod != null && waypointsMod.isEnabled()) {
                waypointsMod.handleChatMessage(event.message.getUnformattedText());
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            if (eventManager != null) {
                eventManager.call(new EventRender2D(
                        event.partialTicks,
                        event.resolution.getScaledWidth(),
                        event.resolution.getScaledHeight()
                ));
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            if (modManager != null) {
                CrosshairMod crosshairMod = modManager.getMod(CrosshairMod.class);
                if (crosshairMod != null && crosshairMod.isEnabled()) {
                    event.setCanceled(true); // Cancels vanilla crosshair
                }
            }
        }
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(net.minecraftforge.client.event.DrawBlockHighlightEvent event) {
        if (modManager != null) {
            BlockOverlayMod overlayMod = modManager.getMod(BlockOverlayMod.class);
            if (overlayMod != null && overlayMod.isEnabled()) {
                event.setCanceled(true); // Cancels vanilla black outline
                overlayMod.renderOverlay(event);
            }
        }
    }

    @SubscribeEvent
    public void onFogDensity(net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity event) {
        if (modManager != null) {
            NoFogMod noFogMod = modManager.getMod(NoFogMod.class);
            if (noFogMod != null && noFogMod.isEnabled() && noFogMod.waterFog.isEnabled()) {
                event.density = 0.0F;
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderFog(net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent event) {
        if (modManager != null) {
            NoFogMod noFogMod = modManager.getMod(NoFogMod.class);
            if (noFogMod != null && noFogMod.isEnabled() && noFogMod.distanceFog.isEnabled()) {
                GlStateManager.setFogStart(999999.0F);
                GlStateManager.setFogEnd(9999999.0F);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (modManager == null || mc.theWorld == null || mc.thePlayer == null) return;

        TeamViewMod teamView = modManager.getMod(TeamViewMod.class);
        if (teamView != null && teamView.isEnabled()) {
            teamView.render3D(event);
        }

        NametagMod nametagMod = modManager.getMod(NametagMod.class);
        if (nametagMod != null && nametagMod.isEnabled()) {
            nametagMod.render3D(event);
        }

        HitboxMod hitboxMod = modManager.getMod(HitboxMod.class);
        if (hitboxMod == null || !hitboxMod.isEnabled()) return;

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue;
            if (hitboxMod.onlyPlayers.isEnabled() && !(entity instanceof EntityPlayer)) continue;

            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - renderPosX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - renderPosY;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - renderPosZ;

            float w = entity.width / 2.0F;
            AxisAlignedBB bb = new AxisAlignedBB(
                    x - w, y, z - w,
                    x + w, y + entity.height, z + w
            );

            // Outline
            GL11.glLineWidth(hitboxMod.lineWidth.getValueFloat());
            int r = hitboxMod.outlineColor.getRed();
            int g = hitboxMod.outlineColor.getGreen();
            int b = hitboxMod.outlineColor.getBlue();
            int a = hitboxMod.outlineColor.getAlpha();
            RenderGlobal.drawOutlinedBoundingBox(bb, r, g, b, a);

            // Eye vector line
            if (hitboxMod.eyeVector.isEnabled()) {
                Vec3 look = entity.getLook(event.partialTicks);
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
        }

        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
    }

    @SubscribeEvent
    public void onRenderLivingSpecialsPre(RenderLivingEvent.Specials.Pre<?> event) {
        if (modManager != null) {
            NametagMod nametagMod = modManager.getMod(NametagMod.class);
            if (nametagMod != null && nametagMod.isEnabled()) {
                if (event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
                    event.setCanceled(true);
                } else if (event.entity instanceof net.minecraft.entity.item.EntityArmorStand) {
                    if (nametagMod.isNametagArmorStand((net.minecraft.entity.item.EntityArmorStand) event.entity)) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public ModManager getModManager() {
        return modManager;
    }

    public ClickGUI getClickGUI() {
        return clickGUI;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
