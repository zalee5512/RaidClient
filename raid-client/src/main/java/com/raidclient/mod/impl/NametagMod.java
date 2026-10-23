package com.raidclient.mod.impl;

import com.raidclient.RaidClient;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.NumberSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NametagMod extends Mod {

    public final BooleanSetting showFaction = new BooleanSetting("Mostrar Facción", true);
    public final BooleanSetting showHealth = new BooleanSetting("Mostrar Vida (HP)", true);
    public final BooleanSetting showStats = new BooleanSetting("Mostrar Estadísticas", true);
    public final BooleanSetting showPing = new BooleanSetting("Mostrar Ping", false);
    public final BooleanSetting showKills = new BooleanSetting("Mostrar Kills", true);
    public final BooleanSetting showDeaths = new BooleanSetting("Mostrar Deaths", true);
    public final BooleanSetting showArmor = new BooleanSetting("Mostrar Armadura", false);
    public final BooleanSetting showEmblem = new BooleanSetting("Mostrar Emblema", false);
    public final BooleanSetting throughWalls = new BooleanSetting("A través de Paredes", true);
    public final BooleanSetting background = new BooleanSetting("Fondo Oscuro", true);
    public final NumberSetting scale = new NumberSetting("Escala", 1.0, 0.5, 2.5, 0.05);
    public final NumberSetting maxDistance = new NumberSetting("Distancia Máxima", 64.0, 16.0, 128.0, 4.0);

    private static final Pattern BRACKET_TAG = Pattern.compile("[\\[<\\(\\{]([a-zA-Z0-9_]{2,16})[\\]>\\)\\}]");
    private static final Pattern STAR_TAG = Pattern.compile("\\*([a-zA-Z0-9_]{2,16})\\*");
    private static final Pattern BAR_TAG = Pattern.compile("\\|([a-zA-Z0-9_]{2,16})\\|");

    // Lista negra estricta para evitar que palabras de sistema o nombres de equipos del scoreboard sean tratadas como facción
    private static final Set<String> BLACKLISTED_TAGS = new HashSet<>(Arrays.asList(
            "enemies", "enemy", "enemigo", "enemigos",
            "allies", "ally", "aliado", "aliados",
            "neutral", "neutrals",
            "teammates", "teammate", "team", "teams", "miembro", "miembros",
            "compañero", "compañeros", "friend", "friends", "amigo", "amigos",
            "faction", "factions", "fac",
            "none", "null", "unknown", "player", "players", "jugador", "jugadores",
            "spectator", "spectators", "spectate",
            "archer", "archers", "bard", "bards", "miner", "miners", "rogue", "rogues",
            "diamond", "diamonds", "pvp", "spawn", "safe", "warzone", "wilderness",
            "staff", "mod", "admin", "owner", "helper", "trial", "builder",
            "vip", "vip+", "mvp", "mvp+", "pro", "elite", "hero", "legend",
            "media", "yt", "youtube", "twitch", "streamer",
            "default", "member", "user", "guest", "citizens", "npc", "npcs"
    ));

    public NametagMod() {
        super("Nametags", "Etiquetas estilo HCF con facción, DTR, rango, vida, ping, kills y deaths", Category.RENDER);
        addSettings(showFaction, showHealth, showStats, showPing, showKills, showDeaths, showArmor, showEmblem, throughWalls, background, scale, maxDistance);
        setEnabled(true);
    }

    public boolean isNametagArmorStand(EntityArmorStand as) {
        if (as == null) return false;
        if (as.hasCustomName()) {
            String name = as.getCustomNameTag();
            if (name == null || name.isEmpty()) return false;

            String lower = name.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim().toLowerCase();
            if (lower.contains("enem") || lower.contains("alli") || lower.contains("ally") ||
                lower.contains("neutr") || lower.contains("team") || lower.contains("dtr") ||
                lower.contains("■") || lower.contains("⚒") || lower.contains("⚔") || lower.contains("☠") ||
                lower.contains("silver") || lower.contains("gold") || lower.contains("bronze") ||
                lower.contains("kills") || lower.contains("deaths") ||
                (name.contains("[") && name.contains("]"))) {
                return true;
            }

            if (as.isInvisible() || as.hasMarker()) {
                return true;
            }

            // Si está situado sobre cualquier jugador conectado, es un holograma de nametag del servidor
            if (mc.theWorld != null) {
                for (EntityPlayer p : mc.theWorld.playerEntities) {
                    if (p == null) continue;
                    double dx = as.posX - p.posX;
                    double dy = as.posY - p.posY;
                    double dz = as.posZ - p.posZ;
                    if (dx * dx + dz * dz <= 4.0 && dy >= -1.5 && dy <= 5.0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onRenderLivingSpecialsPre(RenderLivingEvent.Specials.Pre<?> event) {
        if (!isEnabled()) return;
        if (event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
            event.setCanceled(true);
        } else if (event.entity instanceof EntityArmorStand) {
            if (isNametagArmorStand((EntityArmorStand) event.entity)) {
                event.setCanceled(true);
            }
        }
    }

    public void render3D(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player == mc.thePlayer) continue;
            if (player.isDead || player.getHealth() <= 0.0F) continue;
            if (player.isInvisibleToPlayer(mc.thePlayer)) continue;
            if (player.isSpectator()) continue;

            double dx = player.posX - mc.thePlayer.posX;
            double dy = player.posY - mc.thePlayer.posY;
            double dz = player.posZ - mc.thePlayer.posZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > maxDistSq) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - renderPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - renderPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - renderPosZ;

            renderPlayerNametag(player, x, y, z);
        }
    }

    private static class NametagLine {
        final String text;
        final boolean hasBackground;

        NametagLine(String text, boolean hasBackground) {
            this.text = text;
            this.hasBackground = hasBackground;
        }
    }

    private void renderPlayerNametag(EntityPlayer player, double x, double y, double z) {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        double distance = mc.thePlayer.getDistanceToEntity(player);
        String relationCode = getRelationColorCode(player);

        // Buscar si el servidor tiene líneas en ArmorStands adyacentes al jugador
        String serverStatsLine = null;
        String serverFactionLine = null;

        if (mc.theWorld != null) {
            for (Entity e : mc.theWorld.loadedEntityList) {
                if (e instanceof EntityArmorStand) {
                    EntityArmorStand as = (EntityArmorStand) e;
                    if (as.hasCustomName()) {
                        double dx = as.posX - player.posX;
                        double dy = as.posY - player.posY;
                        double dz = as.posZ - player.posZ;
                        double horizDistSq = dx * dx + dz * dz;

                        // Los ArmorStands de nametags en Spigot/Paper flotan sobre la cabeza del jugador
                        if (horizDistSq <= 4.0 && dy >= -1.5 && dy <= 5.0) {
                            String txt = as.getCustomNameTag();
                            if (txt != null && !txt.isEmpty()) {
                                String clean = txt.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
                                String cleanLower = clean.toLowerCase();
                                if ((txt.contains("■") || txt.contains("◄") || txt.contains("◀") || (txt.contains("[") && txt.contains("|")) || cleanLower.contains("dtr")) &&
                                    !cleanLower.contains("enem") && !cleanLower.contains("alli") && !cleanLower.contains("team")) {
                                    serverFactionLine = txt;
                                } else if (txt.contains("⚒") || txt.contains("⚔") || txt.contains("☠") || txt.contains("✖") ||
                                           cleanLower.contains("kill") || cleanLower.contains("death") ||
                                           cleanLower.contains("muerte") || (txt.contains(":") && txt.contains("|"))) {
                                    // Descartar si solo es indicador de distancia en metros o bloques
                                    if (!cleanLower.endsWith("m") && !cleanLower.contains("bloque") && !cleanLower.contains("block")) {
                                        serverStatsLine = txt;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Construir la lista de líneas en el orden exacto del diseño HCF
        List<NametagLine> lines = new ArrayList<>();

        // Línea 1: Nombre del jugador (sin caja oscura, texto limpio con sombra como en el diseño)
        String nameText = relationCode + player.getName();
        lines.add(new NametagLine(nameText, false));

        // Línea 2: Vida (20 ❤ sin caja, texto blanco y corazón rojo flotante)
        if (showHealth.isEnabled()) {
            float hp = (float) Math.ceil(player.getHealth() + player.getAbsorptionAmount());
            String healthText = "§f" + (int) hp + " §c❤";
            lines.add(new NametagLine(healthText, false));
        }

        // Línea 3: Stats (⚒: 3 | ⚔: 10 con caja oscura)
        if (showStats.isEnabled()) {
            String statsText;
            if (serverStatsLine != null) {
                statsText = serverStatsLine;
            } else {
                List<String> parts = new ArrayList<>();
                if (showPing.isEnabled()) {
                    parts.add("§a⚒: " + getPing(player));
                }
                if (showKills.isEnabled()) {
                    parts.add("§a⚒: " + getKills(player));
                }
                if (showDeaths.isEnabled()) {
                    parts.add("§c⚔: " + getDeaths(player));
                }
                if (parts.size() == 2 && showKills.isEnabled() && showDeaths.isEnabled() && !showPing.isEnabled()) {
                    statsText = "§a⚒: " + getKills(player) + " §7| §c⚔: " + getDeaths(player);
                } else {
                    statsText = String.join(" §7| ", parts);
                }
            }
            if (statsText != null && !statsText.isEmpty()) {
                lines.add(new NametagLine(statsText, true));
            }
        }

        // Línea 4: Facción con DTR y Rango ([yol | 1.8◄ | Silver IV] con caja oscura)
        if (showFaction.isEnabled()) {
            String factionText = null;

            if (serverFactionLine != null) {
                String cleanServer = serverFactionLine.replaceAll("(?i)[§&][0-9a-fk-or]", "").toLowerCase();
                if (!cleanServer.contains("enem") && !cleanServer.contains("alli") && !cleanServer.contains("team")) {
                    factionText = serverFactionLine;
                }
            }

            if (factionText == null) {
                String faction = getFaction(player);
                if (faction != null && !faction.isEmpty() && isValidFaction(faction)) {
                    String dtr = getDTR(player);
                    String rank = getRank(player);
                    factionText = "§6[" + relationCode + faction + " §7| §a" + dtr + "◄ §7| §7" + rank + "§6]";
                }
            }

            if (factionText != null) {
                lines.add(new NametagLine(factionText, true));
            }
        }

        if (lines.isEmpty()) return;

        // Escala con factor de distancia suave para conservar legibilidad a lo lejos
        float distFactor = 1.0F;
        if (distance > 12.0) {
            distFactor = 1.0F + (float) ((distance - 12.0) * 0.012F);
        }
        float s = 0.024F * this.scale.getValueFloat() * distFactor;

        int lineHeight = 11;
        int totalHeight = lines.size() * lineHeight;

        // Posición Y de inicio calculada para que la base quede justo sobre la cabeza
        double tagY = y + (player.isSneaking() ? (player.height + 0.35D) : (player.height + 0.55D)) + (totalHeight * s);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) tagY, (float) z);

        // Orientar hacia la cámara (Billboard)
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        if (mc.gameSettings.thirdPersonView == 2) {
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        }

        GlStateManager.scale(-s, -s, s);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);

        // Control de profundidad para ver a través de paredes
        boolean walls = throughWalls.isEnabled();
        if (walls) {
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
        } else {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
        }

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Emblema superior (opcional, desactivado por defecto)
        if (showEmblem.isEnabled()) {
            String emblem = "§bᛟ";
            int emblemW = fr.getStringWidth(emblem);
            fr.drawStringWithShadow(emblem, -emblemW / 2, -10, 0xFFFFFFFF);
        }

        // Renderizar las líneas apiladas
        int currentY = 0;
        for (NametagLine line : lines) {
            int lineWidth = fr.getStringWidth(line.text);
            int halfW = lineWidth / 2;

            if (line.hasBackground && background.isEnabled()) {
                Gui.drawRect(-halfW - 2, currentY - 1, halfW + 2, currentY + 9, 0x75000000);
            }

            fr.drawStringWithShadow(line.text, -halfW, currentY, 0xFFFFFFFF);
            currentY += lineHeight;
        }

        // Renderizar armadura opcional
        if (showArmor.isEnabled()) {
            List<ItemStack> items = new ArrayList<>();
            if (player.getHeldItem() != null) {
                items.add(player.getHeldItem());
            }
            for (int i = 3; i >= 0; i--) {
                ItemStack armor = player.getCurrentArmor(i);
                if (armor != null) {
                    items.add(armor);
                }
            }

            if (!items.isEmpty()) {
                GlStateManager.pushMatrix();
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
                GlStateManager.enableRescaleNormal();

                int totalWidth = items.size() * 16;
                int itemX = -totalWidth / 2;
                int itemY = -24;

                for (ItemStack stack : items) {
                    mc.getRenderItem().renderItemAndEffectIntoGUI(stack, itemX, itemY);
                    mc.getRenderItem().renderItemOverlays(fr, stack, itemX, itemY);
                    itemX += 16;
                }

                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableLighting();
                GlStateManager.popMatrix();
            }
        }

        // Restaurar estado GL
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();
    }

    public int getPing(EntityPlayer player) {
        if (player == null || mc.getNetHandler() == null) return 0;
        try {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null) {
                return Math.max(0, info.getResponseTime());
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public int getKills(EntityPlayer player) {
        if (player == null || mc.theWorld == null) return 0;
        try {
            Scoreboard sb = mc.theWorld.getScoreboard();
            if (sb != null) {
                for (ScoreObjective obj : sb.getScoreObjectives()) {
                    if (obj == null) continue;
                    String criteria = obj.getCriteria() != null ? obj.getCriteria().getName().toLowerCase() : "";
                    String objName = obj.getName() != null ? obj.getName().toLowerCase() : "";
                    String display = obj.getDisplayName() != null ? obj.getDisplayName().toLowerCase() : "";

                    if (criteria.contains("kill") || objName.contains("kill") || objName.contains("streak") ||
                        display.contains("kill") || display.contains("streak")) {
                        Score score = sb.getValueFromObjective(player.getName(), obj);
                        if (score != null && score.getScorePoints() >= 0) {
                            return score.getScorePoints();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public int getDeaths(EntityPlayer player) {
        if (player == null || mc.theWorld == null) return 0;
        try {
            Scoreboard sb = mc.theWorld.getScoreboard();
            if (sb != null) {
                for (ScoreObjective obj : sb.getScoreObjectives()) {
                    if (obj == null) continue;
                    String criteria = obj.getCriteria() != null ? obj.getCriteria().getName().toLowerCase() : "";
                    String objName = obj.getName() != null ? obj.getName().toLowerCase() : "";
                    String display = obj.getDisplayName() != null ? obj.getDisplayName().toLowerCase() : "";

                    if (criteria.contains("death") || objName.contains("death") || objName.contains("muerte") ||
                        objName.contains("dead") || display.contains("death") || display.contains("muerte")) {
                        Score score = sb.getValueFromObjective(player.getName(), obj);
                        if (score != null && score.getScorePoints() >= 0) {
                            return score.getScorePoints();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public String getDTR(EntityPlayer player) {
        if (player == null || mc.theWorld == null) return "1.8";
        try {
            for (Entity e : mc.theWorld.loadedEntityList) {
                if (e instanceof EntityArmorStand) {
                    EntityArmorStand as = (EntityArmorStand) e;
                    if (as.hasCustomName()) {
                        double dx = as.posX - player.posX;
                        double dy = as.posY - player.posY;
                        double dz = as.posZ - player.posZ;
                        if (dx * dx + dz * dz <= 4.0 && dy >= -1.5 && dy <= 5.0) {
                            String name = as.getCustomNameTag();
                            if (name != null) {
                                String clean = name.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
                                if (clean.contains("◄") || clean.contains("◀") || clean.contains("■")) {
                                    Matcher m = Pattern.compile("(-?[0-9]+(?:\\.[0-9]+)?)\\s*[◄◀■]").matcher(clean);
                                    if (m.find()) {
                                        return m.group(1);
                                    }
                                } else if (clean.toLowerCase().contains("dtr")) {
                                    Matcher m = Pattern.compile("(-?[0-9]+(?:\\.[0-9]+)?)\\s*dtr", Pattern.CASE_INSENSITIVE).matcher(clean);
                                    if (m.find()) {
                                        return m.group(1);
                                    }
                                    m = Pattern.compile("dtr:?\\s*(-?[0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE).matcher(clean);
                                    if (m.find()) {
                                        return m.group(1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return "1.8";
    }

    public String getRank(EntityPlayer player) {
        if (player == null) return "Silver IV";
        try {
            if (mc.theWorld != null) {
                for (Entity e : mc.theWorld.loadedEntityList) {
                    if (e instanceof EntityArmorStand) {
                        EntityArmorStand as = (EntityArmorStand) e;
                        if (as.hasCustomName()) {
                            double dx = as.posX - player.posX;
                            double dy = as.posY - player.posY;
                            double dz = as.posZ - player.posZ;
                            if (dx * dx + dz * dz <= 4.0 && dy >= -1.5 && dy <= 5.0) {
                                String name = as.getCustomNameTag();
                                if (name != null && name.contains("|")) {
                                    String clean = name.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
                                    String[] parts = clean.split("\\|");
                                    if (parts.length >= 3) {
                                        String rankCand = parts[2].replace("]", "").replace("}", "").trim();
                                        if (!rankCand.isEmpty() && rankCand.length() <= 16 && !rankCand.equalsIgnoreCase(player.getName())) {
                                            return rankCand;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();
            if (team != null) {
                String suffix = team.getColorSuffix();
                if (suffix != null && !suffix.trim().isEmpty()) {
                    String clean = suffix.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
                    if (!clean.isEmpty() && clean.length() <= 12 && !clean.equalsIgnoreCase(player.getName())) {
                        return clean;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return "Silver IV";
    }

    public boolean isTeammate(EntityPlayer player) {
        if (player == null || mc.thePlayer == null || player == mc.thePlayer) return false;

        TeamViewMod teamView = RaidClient.INSTANCE.getModManager() != null ?
                RaidClient.INSTANCE.getModManager().getMod(TeamViewMod.class) : null;
        if (teamView != null && teamView.isTeammate(player)) {
            return true;
        }

        if (mc.thePlayer.isOnSameTeam(player)) {
            return true;
        }

        String myPrefix = mc.thePlayer.getDisplayName().getFormattedText();
        String otherPrefix = player.getDisplayName().getFormattedText();
        if (myPrefix.length() >= 2 && otherPrefix.length() >= 2) {
            if (myPrefix.charAt(0) == '§' && otherPrefix.charAt(0) == '§') {
                char c = otherPrefix.charAt(1);
                if (myPrefix.charAt(1) == c && (c == 'a' || c == '2' || c == 'b')) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getRelationColorCode(EntityPlayer player) {
        if (isTeammate(player)) {
            return "§a";
        }
        ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();
        if (team != null) {
            String prefix = team.getColorPrefix();
            if (prefix != null && prefix.length() >= 2 && prefix.charAt(0) == '§') {
                char c = prefix.charAt(1);
                if (c == 'd' || c == '5' || c == 'b' || c == '3') {
                    return "§d";
                }
                if (c == 'e' || c == '6' || c == 'f' || c == '7') {
                    return "§e";
                }
            }
        }
        return "§c"; // Enemigo por defecto
    }

    public String getFaction(EntityPlayer player) {
        if (player == null) return null;
        String pName = player.getName();

        // 1. Extraer desde ArmorStands del servidor (ej: [Bulls | 6.0 ■ | Silver V] -> "Bulls", o [Bulls])
        if (mc.theWorld != null) {
            for (Entity e : mc.theWorld.loadedEntityList) {
                if (e instanceof EntityArmorStand) {
                    EntityArmorStand as = (EntityArmorStand) e;
                    if (as.hasCustomName()) {
                        double dx = as.posX - player.posX;
                        double dy = as.posY - player.posY;
                        double dz = as.posZ - player.posZ;
                        if (dx * dx + dz * dz <= 4.0 && dy >= -1.5 && dy <= 5.0) {
                            String tag = extractFactionTag(as.getCustomNameTag(), pName);
                            if (tag != null && isValidFaction(tag)) {
                                return tag;
                            }
                        }
                    }
                }
            }
        }

        // 2. Extraer desde Team Prefix y Suffix
        ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();
        if (team != null) {
            String tag = extractFactionTag(team.getColorPrefix(), pName);
            if (tag != null && isValidFaction(tag)) return tag;

            tag = extractFactionTag(team.getColorSuffix(), pName);
            if (tag != null && isValidFaction(tag)) return tag;
        }

        // 3. Extraer desde DisplayName
        String displayName = player.getDisplayName().getFormattedText();
        if (displayName != null) {
            String tag = extractFactionTag(displayName, pName);
            if (tag != null && isValidFaction(tag)) return tag;
        }

        // 4. Extraer desde Tab List (NetworkPlayerInfo)
        try {
            if (mc.getNetHandler() != null) {
                NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
                if (info != null) {
                    if (info.getDisplayName() != null) {
                        String tabText = info.getDisplayName().getFormattedText();
                        String tag = extractFactionTag(tabText, pName);
                        if (tag != null && isValidFaction(tag)) return tag;
                    }

                    ScorePlayerTeam tabTeam = info.getPlayerTeam();
                    if (tabTeam != null) {
                        String tag = extractFactionTag(tabTeam.getColorPrefix(), pName);
                        if (tag != null && isValidFaction(tag)) return tag;

                        tag = extractFactionTag(tabTeam.getColorSuffix(), pName);
                        if (tag != null && isValidFaction(tag)) return tag;

                        String formatted = ScorePlayerTeam.formatPlayerName(tabTeam, info.getGameProfile().getName());
                        tag = extractFactionTag(formatted, pName);
                        if (tag != null && isValidFaction(tag)) return tag;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 5. Nombre de equipo registrado si no es genérico ni clasificador
        if (team != null) {
            String teamName = team.getRegisteredName();
            if (teamName != null) {
                String cleanTeam = teamName.replaceAll("^[0-9]+_", ""); // quitar ordenadores 01_, 02_
                if (cleanTeam.toLowerCase().startsWith("fac_") || cleanTeam.toLowerCase().startsWith("f_") || cleanTeam.toLowerCase().startsWith("team_")) {
                    int idx = cleanTeam.indexOf('_');
                    if (idx != -1 && idx < cleanTeam.length() - 1) {
                        cleanTeam = cleanTeam.substring(idx + 1);
                    }
                }
                if (isValidFaction(cleanTeam) && !cleanTeam.equalsIgnoreCase(pName)) {
                    return cleanTeam;
                }
            }
        }

        return null;
    }

    private String extractFactionTag(String text, String playerName) {
        if (text == null || text.trim().isEmpty()) return null;

        String clean = text.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
        if (clean.isEmpty()) return null;

        // Caso HCF compuesto: [Bulls | 6.0 ■ | Silver V]
        if (clean.contains("[") && clean.contains("|")) {
            int start = clean.indexOf('[');
            int pipe = clean.indexOf('|', start);
            if (pipe > start + 1) {
                String cand = clean.substring(start + 1, pipe).trim();
                cand = cand.replaceAll("[^a-zA-Z0-9_]", "");
                if (isValidFaction(cand) && (playerName == null || !cand.equalsIgnoreCase(playerName))) {
                    return cand;
                }
            }
        }

        // Caso corchetes directos: [Bulls], o múltiples corchetes: [Enemy] [Bulls]
        Matcher m = BRACKET_TAG.matcher(clean);
        while (m.find()) {
            String tag = m.group(1).trim();
            if (isValidFaction(tag) && (playerName == null || !tag.equalsIgnoreCase(playerName))) {
                return tag;
            }
        }

        // Caso asteriscos: *Bulls*
        m = STAR_TAG.matcher(clean);
        while (m.find()) {
            String tag = m.group(1).trim();
            if (isValidFaction(tag) && (playerName == null || !tag.equalsIgnoreCase(playerName))) {
                return tag;
            }
        }

        // Caso barras: |Bulls|
        m = BAR_TAG.matcher(clean);
        while (m.find()) {
            String tag = m.group(1).trim();
            if (isValidFaction(tag) && (playerName == null || !tag.equalsIgnoreCase(playerName))) {
                return tag;
            }
        }

        // Buscar en todas las palabras separadas por espacios
        String[] parts = clean.split("\\s+");
        for (String part : parts) {
            String candidate = part.replaceAll("[^a-zA-Z0-9_]", "");
            if (isValidFaction(candidate) && (playerName == null || !candidate.equalsIgnoreCase(playerName))) {
                return candidate;
            }
        }

        return null;
    }

    public boolean isValidFaction(String tag) {
        if (tag == null) return false;
        String clean = tag.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
        clean = clean.replaceAll("[^a-zA-Z0-9_]", "");
        if (clean.length() < 2 || clean.length() > 16) return false;
        if (clean.matches("^[0-9]+$")) return false;

        String lower = clean.toLowerCase();

        // Comprobación de subcadenas no deseadas (relaciones, roles, estadísticas, colores)
        if (lower.contains("enem") || lower.contains("alli") || lower.contains("ally") ||
            lower.contains("neutr") || lower.contains("team") || lower.contains("lead") ||
            lower.contains("offic") || lower.contains("capt") || lower.contains("colead") ||
            lower.contains("factions") || lower.contains("faction") || lower.contains("miembr") ||
            lower.contains("member") || lower.contains("death") || lower.contains("kill") ||
            lower.contains("dtr") || lower.contains("ping") || lower.contains("heart") ||
            lower.contains("vida") || lower.contains("dist") || lower.contains("bloq") ||
            lower.contains("bloque") || lower.contains("block")) {
            return false;
        }

        if (BLACKLISTED_TAGS.contains(lower)) {
            return false;
        }

        return !isVanillaColorName(lower);
    }

    private boolean isVanillaColorName(String name) {
        return name.equals("red") || name.equals("blue") || name.equals("green") ||
               name.equals("yellow") || name.equals("white") || name.equals("black") ||
               name.equals("gray") || name.equals("dark_red") || name.equals("dark_blue") ||
               name.equals("dark_green") || name.equals("dark_aqua") || name.equals("dark_purple") ||
               name.equals("gold") || name.equals("aqua") || name.equals("light_purple");
    }
}
