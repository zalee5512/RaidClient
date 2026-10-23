package com.raidclient.event.impl;

import com.raidclient.event.Event;

public class EventRender2D extends Event {
    private final float partialTicks;
    private final int width;
    private final int height;

    public EventRender2D(float partialTicks, int width, int height) {
        this.partialTicks = partialTicks;
        this.width = width;
        this.height = height;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
