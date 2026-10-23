package com.raidclient.event.impl;

import com.raidclient.event.Event;

public class EventMouse extends Event {
    private final int button;
    private final boolean state;

    public EventMouse(int button, boolean state) {
        this.button = button;
        this.state = state;
    }

    public int getButton() {
        return button;
    }

    public boolean isState() {
        return state;
    }
}
