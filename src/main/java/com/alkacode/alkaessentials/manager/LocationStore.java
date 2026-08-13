package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.model.Warp;
import com.alkacode.alkaessentials.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guarda o spawn e os warps no locations.yml (YAML proprio, decisao do projeto:
 * locais sao config de rede, nao dado transacional - o modulo de punicoes/InvRestore,
 * que vem depois, usa o banco do AlkaCore). Salva no disco a cada mudanca, como o
 * AlkaMines faz com mines.yml.
 */
public final class LocationStore {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<String, Warp> warps = new LinkedHashMap<>();

    public LocationStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
        load();
    }

    private void load() {
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            config = new YamlConfiguration();
        }
        warps.clear();
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String loc = section.getString(key + ".location");
                String permission = section.getString(key + ".permission");
                if (loc != null) {
                    warps.put(key.toLowerCase(), new Warp(key, LocationUtil.deserialize(loc, null), permission));
                }
            }
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao salvar locations.yml: " + e.getMessage());
        }
    }

    // ------------------------- Spawn -------------------------

    public Location getSpawn() {
        String data = config.getString("spawn");
        return LocationUtil.deserialize(data, plugin.getServer().getWorlds().get(0));
    }

    public boolean hasSpawn() {
        return config.contains("spawn");
    }

    public void setSpawn(Location loc) {
        config.set("spawn", LocationUtil.serialize(loc));
        save();
    }

    public void removeSpawn() {
        config.set("spawn", null);
        save();
    }

    // ------------------------- Warps -------------------------

    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public Map<String, Warp> getWarps() {
        return warps;
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public void setWarp(String name, Location loc, String permission) {
        String key = name.toLowerCase();
        warps.put(key, new Warp(name, loc, permission));
        config.set("warps." + key + ".location", LocationUtil.serialize(loc));
        config.set("warps." + key + ".permission", permission);
        save();
    }

    public void removeWarp(String name) {
        String key = name.toLowerCase();
        if (warps.remove(key) != null) {
            config.set("warps." + key, null);
            save();
        }
    }
}
