package com.alkacode.alkaessentials.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/** Carrega events.yml (comandos executados em gatilhos do jogador). */
public final class EventsConfig {

    private static EventsConfig instance;

    public static EventsConfig getInstance() {
        return instance;
    }

    private final JavaPlugin plugin;
    private YamlConfiguration config;

    private EventsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public static void init(JavaPlugin plugin) {
        instance = new EventsConfig(plugin);
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "events.yml");
        if (!file.exists()) {
            plugin.saveResource("events.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public List<String> getCommands(String key) {
        return config.getStringList(key);
    }
}
