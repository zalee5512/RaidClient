package com.raidclient.setting;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
        if (this.index == -1) this.index = 0;
    }

    public String getMode() {
        return modes.get(index);
    }

    public boolean is(String mode) {
        return getMode().equalsIgnoreCase(mode);
    }

    public void cycle() {
        if (index < modes.size() - 1) {
            index++;
        } else {
            index = 0;
        }
    }

    public void setMode(String mode) {
        int idx = modes.indexOf(mode);
        if (idx != -1) {
            this.index = idx;
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return index;
    }
}
