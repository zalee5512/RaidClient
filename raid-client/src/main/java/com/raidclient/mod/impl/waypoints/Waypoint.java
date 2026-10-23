package com.raidclient.mod.impl.waypoints;

public class Waypoint {
    private String name;
    private double x;
    private double y;
    private double z;
    private int color; // ARGB
    private String server;
    private boolean enabled;
    private boolean isKoth;

    public Waypoint(String name, double x, double y, double z, int color, String server, boolean isKoth) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.server = server != null ? server.toLowerCase() : "singleplayer";
        this.enabled = true;
        this.isKoth = isKoth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server != null ? server.toLowerCase() : "singleplayer";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isKoth() {
        return isKoth;
    }

    public void setKoth(boolean koth) {
        isKoth = koth;
    }
}
