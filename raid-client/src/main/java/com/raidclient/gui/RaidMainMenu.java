package com.raidclient.gui;

import com.raidclient.RaidClient;
import com.raidclient.util.RenderUtils;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RaidMainMenu extends GuiScreen {

    private static final ResourceLocation BIOME_BG = new ResourceLocation("raidclient", "textures/gui/biome_bg.png");
    private static final ResourceLocation RAID_LOGO = new ResourceLocation("raidclient", "textures/gui/raid_logo.png");

    private final List<FireflyParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    private static class FireflyParticle {
        float x, y;
        float speedY, speedX;
        float size;
        float phase;
        int color;

        FireflyParticle(float x, float y, float speedY, float speedX, float size, float phase, int color) {
            this.x = x;
            this.y = y;
            this.speedY = speedY;
            this.speedX = speedX;
            this.size = size;
            this.phase = phase;
            this.color = color;
        }
    }

    @Override
    public void initGui() {
        particles.clear();
        for (int i = 0; i < 40; i++) {
            particles.add(new FireflyParticle(
                    random.nextFloat() * width,
                    random.nextFloat() * height,
                    0.25F + random.nextFloat() * 0.45F,
                    0.15F + random.nextFloat() * 0.35F,
                    2.0F + random.nextFloat() * 3.0F,
                    random.nextFloat() * 6.28F,
                    random.nextBoolean() ? 0x00E5FF : 0x00B4D8
            ));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float time = (System.currentTimeMillis() % 1000000L) / 1000.0F;

        // ─── 1. Biome Background Image with Parallax ─────────────────
        drawBiomeWallpaper(mouseX, mouseY);

        // ─── 2. Subtle Dark Vignette & Shadow Overlay ────────────────
        RenderUtils.drawRect(0, 0, width, height, 0x44000000);
        RenderUtils.drawRect(0, (float)(height * 0.65), width, (float)(height * 0.35), 0x55080C14);

        // ─── 3. Animated Floating Fireflies / Spores ─────────────────
        drawFireflies(time);

        // ─── 4. Glowing Raid Logo (Transparent, No Black Box) ────────
        drawGlowingLogo(time);

        // ─── 5. Sleek Lunar-Style Action Buttons ──────────────────────
        drawButtons(mouseX, mouseY);

        // ─── 6. User Info & Shortcut Badges ──────────────────────────
        drawDecorations();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBiomeWallpaper(int mx, int my) {
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BIOME_BG);

        // Subtle parallax movement based on cursor
        float panX = (mx - width / 2.0F) * 0.015F;
        float panY = (my - height / 2.0F) * 0.015F;

        float extra = 20.0F;
        Gui.drawModalRectWithCustomSizedTexture(
                (int)(-extra + panX),
                (int)(-extra + panY),
                0, 0,
                (int)(width + extra * 2),
                (int)(height + extra * 2),
                (int)(width + extra * 2),
                (int)(height + extra * 2)
        );
    }

    private void drawFireflies(float time) {
        for (FireflyParticle p : particles) {
            p.y -= p.speedY;
            p.x += (float) Math.sin(time + p.phase) * p.speedX;

            if (p.y < -10) {
                p.y = height + 10;
                p.x = random.nextFloat() * width;
            }

            float glow = (float) (Math.sin(time * 2.5F + p.phase) * 0.35F + 0.65F);
            int alpha = (int)(glow * 190);
            int color = (alpha << 24) | p.color;

            RenderUtils.drawRoundedRect(p.x, p.y, p.size, p.size, p.size / 2.0F, color);
        }
    }

    private void drawGlowingLogo(float time) {
        // Size of the clean hexagonal emblem
        int logoW = 145;
        int logoH = 145;

        float logoX = width / 2.0F - logoW / 2.0F;
        float logoY = Math.max(14, height * 0.11F);

        float pulse = (float) Math.sin(time * 2.0F) * 2.5F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Bind transparent Raid Logo
        mc.getTextureManager().bindTexture(RAID_LOGO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(
                (int)(logoX - pulse / 2.0F),
                (int)(logoY - pulse / 2.0F),
                0, 0,
                (int)(logoW + pulse),
                (int)(logoH + pulse),
                (int)(logoW + pulse),
                (int)(logoH + pulse)
        );

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawButtons(int mx, int my) {
        float btnW = 230;
        float btnH = 28;
        float btnX = width / 2.0F - btnW / 2.0F;
        float startY = Math.max(200, height * 0.10F + 190);

        // 1. MULTIJUGADOR (Primary Button)
        drawGlassButton("⚔  MULTIJUGADOR", btnX, startY, btnW, btnH, mx, my, 0xFF00E5FF, true);

        // 2. UN JUGADOR
        drawGlassButton("🌍  UN JUGADOR", btnX, startY + 34, btnW, btnH, mx, my, 0xFFFFFFFF, false);

        // 3. MODS DE RAID (Shift Derecho)
        drawGlassButton("⚡  MODS DE RAID (ClickGUI)", btnX, startY + 68, btnW, btnH, mx, my, 0xFFFFD700, false);

        // 4. Split row: OPCIONES / SALIR
        float halfW = (btnW - 8) / 2.0F;
        drawGlassButton("⚙  OPCIONES", btnX, startY + 102, halfW, btnH, mx, my, 0xFFA5ACB8, false);
        drawGlassButton("✕  SALIR", btnX + halfW + 8, startY + 102, halfW, btnH, mx, my, 0xFFFF5555, false);
    }

    private void drawGlassButton(String text, float x, float y, float w, float h, int mx, int my, int accentColor, boolean primary) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;

        int bgColor = hovered ? 0xDD1B2436 : 0xBB0E1420;
        int borderColor = hovered ? accentColor : (primary ? 0x6600E5FF : 0x22FFFFFF);

        RenderUtils.drawRoundedRect(x, y, w, h, 5, bgColor);
        RenderUtils.drawOutline(x, y, w, h, 1.0F, borderColor);

        int strW = fontRendererObj.getStringWidth(text);
        int textColor = hovered ? accentColor : (primary ? 0xFFFFFFFF : 0xFFBAC0CD);
        fontRendererObj.drawStringWithShadow(text, x + w / 2.0F - strW / 2.0F, y + h / 2.0F - 4, textColor);
    }

    private void drawDecorations() {
        // Player status pill top-left
        String username = mc.getSession() != null ? mc.getSession().getUsername() : "RaidPlayer";
        int nameW = fontRendererObj.getStringWidth(username);
        RenderUtils.drawRoundedRect(14, 14, nameW + 36, 22, 5, 0xCC0E1420);
        RenderUtils.drawOutline(14, 14, nameW + 36, 22, 1.0F, 0x3300E5FF);
        RenderUtils.drawRoundedRect(20, 20, 10, 10, 5, 0xFF00E5FF);
        fontRendererObj.drawStringWithShadow(username, 36, 20, 0xFFFFFFFF);

        // Footer left subtle branding
        fontRendererObj.drawStringWithShadow("Raid Client v1.8.9", 14, height - 16, 0x55AAAAAA);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float btnW = 230;
        float btnH = 28;
        float btnX = width / 2.0F - btnW / 2.0F;
        float startY = Math.max(200, height * 0.10F + 190);

        // 1. MULTIJUGADOR
        if (isHovered(btnX, startY, btnW, btnH, mouseX, mouseY)) {
            mc.displayGuiScreen(new GuiMultiplayer(this));
            return;
        }

        // 2. UN JUGADOR
        if (isHovered(btnX, startY + 34, btnW, btnH, mouseX, mouseY)) {
            mc.displayGuiScreen(new GuiSelectWorld(this));
            return;
        }

        // 3. MODS DE RAID
        if (isHovered(btnX, startY + 68, btnW, btnH, mouseX, mouseY)) {
            mc.displayGuiScreen(RaidClient.INSTANCE.getClickGUI());
            return;
        }

        // 4. OPCIONES
        float halfW = (btnW - 8) / 2.0F;
        if (isHovered(btnX, startY + 102, halfW, btnH, mouseX, mouseY)) {
            mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
            return;
        }

        // 5. SALIR
        if (isHovered(btnX + halfW + 8, startY + 102, halfW, btnH, mouseX, mouseY)) {
            mc.shutdown();
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isHovered(float x, float y, float w, float h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
