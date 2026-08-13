package com.alkacode.alkaessentials.manager;

import org.bukkit.plugin.java.JavaPlugin;

/** Modo manutencao: bloqueia conexoes nao-staff e muda o MOTD. Estado persistido no config.yml. */
public final class MaintenanceManager {

    private final JavaPlugin plugin;
    private boolean enabled;
    private String reason;

    public MaintenanceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("maintenance.enabled", false);
        this.reason = plugin.getConfig().getString("maintenance.reason", "");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getReason() {
        return reason;
    }

    public void setEnabled(boolean value, String reason) {
        this.enabled = value;
        this.reason = reason == null ? "" : reason;
        plugin.getConfig().set("maintenance.enabled", value);
        plugin.getConfig().set("maintenance.reason", this.reason);
        plugin.saveConfig();
    }
}
