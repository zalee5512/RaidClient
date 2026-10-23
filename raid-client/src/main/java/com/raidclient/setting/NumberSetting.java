package com.raidclient.setting;

public class NumberSetting extends Setting {
    private double value;
    private final double minimum;
    private final double maximum;
    private final double increment;

    public NumberSetting(String name, double defaultValue, double minimum, double maximum, double increment) {
        super(name);
        this.value = defaultValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
    }

    public double getValue() {
        return value;
    }

    public float getValueFloat() {
        return (float) value;
    }

    public int getValueInt() {
        return (int) Math.round(value);
    }

    public void setValue(double value) {
        double precision = 1.0 / this.increment;
        this.value = Math.round(Math.max(this.minimum, Math.min(this.maximum, value)) * precision) / precision;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getIncrement() {
        return increment;
    }
}
