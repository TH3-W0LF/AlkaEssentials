package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda as homes dos jogadores no homes.yml (YAML proprio, decisao do projeto).
 * Chave primaria e o UUID (nao o nick, que muda com /nick). Nome de home e lowercase.
 */
public final class HomeManager {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public HomeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    private void load() {
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            config = new YamlConfiguration();
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao salvar homes.yml: " + e.getMessage());
        }
    }

    public boolean hasHome(UUID uuid, String name) {
        return config.contains("homes." + uuid + "." + name.toLowerCase());
    }

    public Location getHome(UUID uuid, String name) {
        String data = config.getString("homes." + uuid + "." + name.toLowerCase());
        return LocationUtil.deserialize(data, null);
    }

    public Map<String, Location> getHomes(UUID uuid) {
        Map<String, Location> result = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("homes." + uuid);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Location loc = LocationUtil.deserialize(section.getString(key), null);
                if (loc != null) {
                    result.put(key, loc);
                }
            }
        }
        return result;
    }

    public void setHome(UUID uuid, String name, Location loc) {
        config.set("homes." + uuid + "." + name.toLowerCase(), LocationUtil.serialize(loc));
        save();
    }

    public void removeHome(UUID uuid, String name) {
        if (config.contains("homes." + uuid + "." + name.toLowerCase())) {
            config.set("homes." + uuid + "." + name.toLowerCase(), null);
            save();
        }
    }
}
