package com.raidclient.gui;

import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.ProgressManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.SharedDrawable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL11.*;

public class RaidSplashScreen {

    private static volatile boolean running = false;
    private static volatile boolean done = false;
    private static volatile boolean pause = false;
    private static Drawable drawable;
    private static Thread thread;
    private static final Lock lock = new ReentrantLock(true);
    private static int logoTextureId = -1;
    private static float currentProgress = 0.05F;
    private static Semaphore mutex;

    static {
        try {
            Field f = SplashProgress.class.getDeclaredField("mutex");
            f.setAccessible(true);
            mutex = (Semaphore) f.get(null);
        } catch (Throwable t) {
            mutex = new Semaphore(1);
        }
    }

    public static void start() {
        if (running) return;
        running = true;
        done = false;
        pause = false;

        try {
            drawable = new SharedDrawable(Display.getDrawable());
            Display.getDrawable().releaseContext();
            drawable.makeCurrent();
        } catch (LWJGLException e) {
            e.printStackTrace();
            running = false;
            return;
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    Display.getDrawable().makeCurrent();
                } catch (LWJGLException e) {
                    e.printStackTrace();
                    lock.unlock();
                    return;
                }

                // Load logo texture in thread context
                logoTextureId = loadTexture("/assets/raidclient/textures/gui/raid_logo.png");

                glClearColor(0.039F, 0.051F, 0.082F, 1.0F); // #0A0D15
                glDisable(GL_LIGHTING);
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                while (!done) {
                    glClear(GL_COLOR_BUFFER_BIT);

                    int w = Display.getWidth();
                    int h = Display.getHeight();
                    if (w <= 0 || h <= 0) {
                        w = 854;
                        h = 480;
                    }

                    glViewport(0, 0, w, h);
                    glMatrixMode(GL_PROJECTION);
                    glLoadIdentity();
                    glOrtho(0, w, h, 0, -1, 1);
                    glMatrixMode(GL_MODELVIEW);
                    glLoadIdentity();

                    // 1. Draw centered emblem / logo
                    int logoSize = Math.max(120, Math.min(220, Math.min(w, h) / 3));
                    int logoX = (w - logoSize) / 2;
                    int logoY = (h - logoSize) / 2 - 35;

                    if (logoTextureId > 0) {
                        glEnable(GL_TEXTURE_2D);
                        glBindTexture(GL_TEXTURE_2D, logoTextureId);
                        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

                        glBegin(GL_QUADS);
                        glTexCoord2f(0.0F, 0.0F); glVertex2f(logoX, logoY);
                        glTexCoord2f(0.0F, 1.0F); glVertex2f(logoX, logoY + logoSize);
                        glTexCoord2f(1.0F, 1.0F); glVertex2f(logoX + logoSize, logoY + logoSize);
                        glTexCoord2f(1.0F, 0.0F); glVertex2f(logoX + logoSize, logoY);
                        glEnd();

                        glDisable(GL_TEXTURE_2D);
                    }

                    // 2. Draw modern sleek cyan progress bar
                    int barW = Math.max(200, Math.min(380, (int) (w * 0.45F)));
                    int barH = 5;
                    int barX = (w - barW) / 2;
                    int barY = logoY + logoSize + 30;

                    // Track background (#141926)
                    drawColoredRect(barX, barY, barW, barH, 0.078F, 0.098F, 0.149F, 1.0F);

                    // Target progress from Forge ProgressManager
                    float targetProgress = computeTargetProgress();
                    // Smooth easing
                    currentProgress += (targetProgress - currentProgress) * 0.10F;
                    if (currentProgress < 0.05F) currentProgress = 0.05F;
                    if (currentProgress > 1.0F) currentProgress = 1.0F;

                    int fillW = (int) (barW * currentProgress);
                    if (fillW > 0) {
                        // Cyan bar (#00E5FF)
                        drawColoredRect(barX, barY, fillW, barH, 0.0F, 0.898F, 1.0F, 1.0F);
                    }

                    // 3. Thread-safe Display Update with Forge message pump mutex
                    if (mutex != null) mutex.acquireUninterruptibly();
                    try {
                        Display.update();
                    } finally {
                        if (mutex != null) mutex.release();
                    }

                    if (pause) {
                        try {
                            Display.getDrawable().releaseContext();
                        } catch (LWJGLException ignored) {}
                        lock.unlock();

                        while (pause && !done) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ignored) {}
                        }

                        lock.lock();
                        try {
                            Display.getDrawable().makeCurrent();
                        } catch (LWJGLException ignored) {}
                    }

                    Display.sync(60);
                }

                if (logoTextureId > 0) {
                    try {
                        glDeleteTextures(logoTextureId);
                    } catch (Throwable ignored) {}
                    logoTextureId = -1;
                }

                // Release context before finishing
                try {
                    Display.getDrawable().releaseContext();
                } catch (LWJGLException ignored) {}
                lock.unlock();
            }
        }, "RaidClient-Splash-Thread");

        thread.setDaemon(true);
        thread.start();
    }

    private static float computeTargetProgress() {
        try {
            Iterator<ProgressManager.ProgressBar> iterator = ProgressManager.barIterator();
            ProgressManager.ProgressBar first = null;
            ProgressManager.ProgressBar last = null;
            while (iterator.hasNext()) {
                ProgressManager.ProgressBar b = iterator.next();
                if (first == null) first = b;
                last = b;
            }
            if (first != null && first.getSteps() > 0) {
                float fProgress = (float) first.getStep() / (float) first.getSteps();
                if (last != null && last != first && last.getSteps() > 0) {
                    float lProgress = (float) last.getStep() / (float) last.getSteps();
                    return Math.min(1.0F, fProgress + (lProgress / first.getSteps()));
                }
                return Math.min(1.0F, fProgress);
            }
        } catch (Throwable ignored) {}
        return 0.5F;
    }

    private static void drawColoredRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        glColor4f(r, g, b, a);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x, y + height);
        glVertex2f(x + width, y + height);
        glVertex2f(x + width, y);
        glEnd();
    }

    private static int loadTexture(String path) {
        try {
            InputStream in = RaidSplashScreen.class.getResourceAsStream(path);
            if (in == null) return -1;
            BufferedImage image = ImageIO.read(in);
            in.close();
            if (image == null) return -1;

            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            int texId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            return texId;
        } catch (Throwable e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void pause() {
        if (!running) return;
        pause = true;
        lock.lock();
        try {
            if (drawable != null) {
                drawable.releaseContext();
                Display.getDrawable().makeCurrent();
            }
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
    }

    public static void resume() {
        if (!running) return;
        pause = false;
        try {
            Display.getDrawable().releaseContext();
            if (drawable != null) {
                drawable.makeCurrent();
            }
        } catch (LWJGLException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void finish() {
        if (!running) return;
        done = true;
        try {
            if (thread != null) {
                thread.join(3000);
            }
            if (drawable != null) {
                drawable.releaseContext();
                drawable.destroy();
                drawable = null;
            }
            Display.getDrawable().makeCurrent();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null) {
                mc.displayWidth = Display.getWidth();
                mc.displayHeight = Display.getHeight();
                mc.resize(mc.displayWidth, mc.displayHeight);
            }
            glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glEnable(GL_ALPHA_TEST);
            glAlphaFunc(GL_GREATER, 0.1F);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        running = false;
    }
}
