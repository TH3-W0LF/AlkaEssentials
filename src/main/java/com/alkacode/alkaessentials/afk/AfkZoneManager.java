package com.alkacode.alkaessentials.afk;

import com.alkacode.alkaessentials.config.MessagesConfig;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia as zonas AFK (pasta zones/*.yml) e as selecoes da varinha. O ticker
 * (task da main thread) chama {@link #tick()} a cada tick pra acumular tempo nas
 * zonas e desenhar as particulas das selecoes ativas.
 */
public final class AfkZoneManager {

    private final JavaPlugin plugin;
    private final AfkManager afkManager;
    private final File zonesDir;
    private final Map<String, AfkZone> zones = new HashMap<>();
    private final Map<UUID, ZoneSelection> selections = new HashMap<>();

    public AfkZoneManager(JavaPlugin plugin, AfkManager afkManager) {
        this.plugin = plugin;
        this.afkManager = afkManager;
        this.zonesDir = new File(plugin.getDataFolder(), "zones");
        loadAll();
    }

    public void loadAll() {
        if (!zonesDir.exists()) {
            zonesDir.mkdirs();
        }
        zones.clear();
        File[] files = zonesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".yml", "");
                zones.put(name, new AfkZone(plugin, afkManager, name, file,
                        YamlConfiguration.loadConfiguration(file)));
            }
        }
    }

    public void tick() {
        for (AfkZone zone : zones.values()) {
            zone.tick();
        }
        for (ZoneSelection selection : selections.values()) {
            selection.show();
        }
    }

    public boolean createZone(String name, ZoneRegion region) {
        String key = name.toLowerCase();
        if (zones.containsKey(key)) {
            return false;
        }
        File file = new File(zonesDir, key + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        YamlConfiguration d = MessagesConfig.getInstance().getYaml();
        config.set("messages.entered", d.getString("zone-defaults.messages.entered",
                "<yellow>Voce entrou na zona AFK <gold>%zone%<yellow>! Recompensa em <gold>%time%<yellow>."));
        config.set("messages.left", d.getString("zone-defaults.messages.left",
                "<yellow>Voce saiu da zona AFK. Ficou <gold>%time%<yellow>."));
        config.set("messages.reward", d.getStringList("zone-defaults.messages.reward"));

        config.set("in-zone.title", d.getString("zone-defaults.in-zone.title", "<gold>AFK"));
        config.set("in-zone.subtitle", d.getString("zone-defaults.in-zone.subtitle",
                "<yellow>Proxima recompensa em: <gold>%time%"));
        config.set("in-zone.actionbar", d.getString("zone-defaults.in-zone.actionbar", ""));
        config.set("in-zone.bossbar.name", d.getString("zone-defaults.in-zone.bossbar.name", "<gold>AFK"));
        config.set("in-zone.bossbar.color", d.getString("zone-defaults.in-zone.bossbar.color", "RED"));
        config.set("in-zone.bossbar.style", d.getString("zone-defaults.in-zone.bossbar.style", "NOTCHED_20"));

        config.set("permission", "");
        config.set("reward-time-seconds", 60);
        config.set("roll-amount", 1);
        config.set("reset-after-reward", false);
        config.set("rewards", java.util.List.of());

        config.set("zone.location1", com.alkacode.alkaessentials.util.LocationUtil.serialize(region.getCorner1()));
        config.set("zone.location2", com.alkacode.alkaessentials.util.LocationUtil.serialize(region.getCorner2()));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao criar zones/" + key + ".yml: " + e.getMessage());
            return false;
        }
        zones.put(key, new AfkZone(plugin, afkManager, key, file, config));
        return true;
    }

    public boolean deleteZone(String name) {
        AfkZone zone = zones.remove(name.toLowerCase());
        if (zone == null) {
            return false;
        }
        zone.disable();
        return zone.getFile().delete();
    }

    public void redefineZone(String name, ZoneRegion region) {
        AfkZone zone = zones.get(name.toLowerCase());
        if (zone != null) {
            zone.setRegion(region);
        }
    }

    public void reload() {
        for (AfkZone zone : zones.values()) {
            zone.disable();
        }
        zones.clear();
        loadAll();
    }

    public void disableAll() {
        for (AfkZone zone : zones.values()) {
            zone.disable();
        }
        zones.clear();
    }

    public AfkZone getZone(String name) {
        return zones.get(name.toLowerCase());
    }

    public Map<String, AfkZone> getZones() {
        return zones;
    }

    // ------------------------- selecao da varinha -------------------------

    public ZoneSelection getSelection(UUID uuid) {
        return selections.computeIfAbsent(uuid, k -> new ZoneSelection(plugin.getServer().getPlayer(uuid)));
    }

    public void removeSelection(UUID uuid) {
        selections.remove(uuid);
    }

    public boolean hasSelection(UUID uuid) {
        return selections.containsKey(uuid);
    }
}
