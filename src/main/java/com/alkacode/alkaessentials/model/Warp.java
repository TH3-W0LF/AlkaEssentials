package com.alkacode.alkaessentials.model;

import org.bukkit.Location;

/** Warp registrado no locations.yml - nome, local e permissao opcional de acesso. */
public final class Warp {

    private final String name;
    private final Location location;
    private final String permission;

    public Warp(String name, Location location, String permission) {
        this.name = name;
        this.location = location;
        this.permission = permission;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }
}
