package com.raidclient.mod.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.raidclient.mod.Category;
import com.raidclient.mod.Mod;
import com.raidclient.setting.BooleanSetting;
import com.raidclient.setting.ColorSetting;
import com.raidclient.setting.NumberSetting;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScoreboardMod extends Mod {

    public final NumberSetting scale = new NumberSetting("Escala", 1.0, 0.5, 2.0, 0.05);
    public final NumberSetting customWidth = new NumberSetting("Anchura Extra", 0.0, 0.0, 150.0, 5.0);
    public final NumberSetting offsetX = new NumberSetting("Desplazamiento X", 0.0, -200.0, 200.0, 5.0);
    public final NumberSetting offsetY = new NumberSetting("Desplazamiento Y", 0.0, -200.0, 200.0, 5.0);
    public final BooleanSetting useCustomPos = new BooleanSetting("Posición Libre (HUD)", false);
    public final BooleanSetting hideNumbers = new BooleanSetting("Ocultar Números Rojos", true);
    public final BooleanSetting background = new BooleanSetting("Fondo Translúcido", true);
    public final BooleanSetting textShadow = new BooleanSetting("Sombra de Texto", true);
    public final ColorSetting bgColor = new ColorSetting("Color Fondo", 15, 17, 25, 120);

    public ScoreboardMod() {
        super("Scoreboard", "Personaliza la tabla lateral: escala, anchura, posición y colores", Category.HUD);
        setX(10);
        setY(100);
        setWidth(120);
        setHeight(100);
        addSettings(scale, customWidth, offsetX, offsetY, useCustomPos, hideNumbers, background, textShadow, bgColor);
        setEnabled(true);
    }

    public void renderScoreboard(ScaledResolution sr) {
        if (mc.theWorld == null) return;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = null;
        ScorePlayerTeam team = scoreboard.getPlayersTeam(mc.thePlayer.getName());

        if (team != null) {
            int slot = team.getChatFormat().getColorIndex();
            if (slot >= 0) {
                objective = scoreboard.getObjectiveInDisplaySlot(3 + slot);
            }
        }

        if (objective == null) {
            objective = scoreboard.getObjectiveInDisplaySlot(1);
        }

        if (objective == null) return;

        drawCustomScoreboard(objective, sr);
    }

    private void drawCustomScoreboard(ScoreObjective objective, ScaledResolution sr) {
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        List<Score> filteredScores = new ArrayList<>();

        for (Score score : scores) {
            if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                filteredScores.add(score);
            }
        }

        if (filteredScores.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(filteredScores, filteredScores.size() - 15));
        } else {
            scores = filteredScores;
        }

        FontRenderer font = mc.fontRendererObj;
        String title = objective.getDisplayName();
        int maxWidth = font.getStringWidth(title);

        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String formatted = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            int w = font.getStringWidth(formatted);
            if (!hideNumbers.isEnabled()) {
                w += font.getStringWidth(" " + score.getScorePoints());
            }
            if (w > maxWidth) {
                maxWidth = w;
            }
        }

        int scoreCount = scores.size();
        int lineHeight = font.FONT_HEIGHT;
        int totalHeight = (scoreCount + 1) * (lineHeight + 1) + 4;
        int renderWidth = maxWidth + 8 + customWidth.getValueInt();

        float s = scale.getValueFloat();
        boolean applyScale = Math.abs(s - 1.0f) > 0.001f;

        float drawX;
        float drawY;

        if (useCustomPos.isEnabled()) {
            drawX = getX();
            drawY = getY();
        } else {
            drawX = sr.getScaledWidth() - (renderWidth * s) - 3 + offsetX.getValueFloat();
            drawY = (sr.getScaledHeight() / 2.0F - (totalHeight * s) / 2.0F) + offsetY.getValueFloat();
        }

        setWidth(renderWidth * s);
        setHeight(totalHeight * s);

        if (applyScale) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(drawX, drawY, 0.0F);
            GlStateManager.scale(s, s, 1.0F);
            GlStateManager.translate(-drawX, -drawY, 0.0F);
        }

        // Fondo personalizado
        if (background.isEnabled()) {
            RenderUtils.drawRoundedRect(drawX, drawY, renderWidth, totalHeight, 3, bgColor.getRGB());
            RenderUtils.drawOutline(drawX, drawY, renderWidth, totalHeight, 1.0F, 0x22FFFFFF);
        }

        // Título de la Scoreboard
        int titleX = (int) (drawX + (renderWidth - font.getStringWidth(title)) / 2.0F);
        font.drawString(title, titleX, (int) drawY + 3, 0xFFFFFFFF, textShadow.isEnabled());

        // Líneas / Puntuaciones
        int index = 0;
        for (Score score : scores) {
            index++;
            ScorePlayerTeam scoreTeam = scoreboard.getPlayersTeam(score.getPlayerName());
            String formatted = ScorePlayerTeam.formatPlayerName(scoreTeam, score.getPlayerName());
            int currentY = (int) (drawY + index * (lineHeight + 1) + 2);

            font.drawString(formatted, (int) drawX + 4, currentY, 0xFFFFFFFF, textShadow.isEnabled());

            if (!hideNumbers.isEnabled()) {
                String numStr = EnumChatFormatting.RED + "" + score.getScorePoints();
                int numX = (int) (drawX + renderWidth - font.getStringWidth(numStr) - 3);
                font.drawString(numStr, numX, currentY, 0xFFFFFFFF, textShadow.isEnabled());
            }
        }

        if (applyScale) {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void renderDummy(int mouseX, int mouseY) {
        float s = scale.getValueFloat();
        boolean applyScale = Math.abs(s - 1.0f) > 0.001f;

        FontRenderer font = mc.fontRendererObj;
        String title = EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "RAID CLIENT";
        String line1 = "Kills: " + EnumChatFormatting.GREEN + "12";
        String line2 = "KOTH: " + EnumChatFormatting.YELLOW + "04:12";
        String line3 = "Faction: " + EnumChatFormatting.LIGHT_PURPLE + "RaidTeam";

        int baseWidth = 100 + customWidth.getValueInt();
        int baseHeight = 52;
        setWidth(baseWidth * s);
        setHeight(baseHeight * s);

        float drawX = getX();
        float drawY = getY();

        if (applyScale) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(drawX, drawY, 0.0F);
            GlStateManager.scale(s, s, 1.0F);
            GlStateManager.translate(-drawX, -drawY, 0.0F);
        }

        if (background.isEnabled()) {
            RenderUtils.drawRoundedRect(drawX, drawY, baseWidth, baseHeight, 3, bgColor.getRGB());
            RenderUtils.drawOutline(drawX, drawY, baseWidth, baseHeight, 1.0F, 0x4400E5FF);
        }

        font.drawString(title, (int) (drawX + (baseWidth - font.getStringWidth(title)) / 2.0F), (int) drawY + 3, 0xFFFFFFFF, textShadow.isEnabled());
        font.drawString(line1, (int) drawX + 4, (int) drawY + 16, 0xFFFFFFFF, textShadow.isEnabled());
        font.drawString(line2, (int) drawX + 4, (int) drawY + 27, 0xFFFFFFFF, textShadow.isEnabled());
        font.drawString(line3, (int) drawX + 4, (int) drawY + 38, 0xFFFFFFFF, textShadow.isEnabled());

        if (applyScale) {
            GlStateManager.popMatrix();
        }
    }
}
