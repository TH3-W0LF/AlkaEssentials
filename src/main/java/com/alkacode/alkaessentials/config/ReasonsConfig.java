package com.alkacode.alkaessentials.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Carrega reasons.yml (tipos de punicao + motivos + duracoes do menu premium /punish). */
public final class ReasonsConfig {

    private static ReasonsConfig instance;

    public static ReasonsConfig getInstance() {
        return instance;
    }

    private final JavaPlugin plugin;
    private YamlConfiguration config;

    private ReasonsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public static void init(JavaPlugin plugin) {
        instance = new ReasonsConfig(plugin);
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "reasons.yml");
        if (!file.exists()) {
            plugin.saveResource("reasons.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public List<String> getTypes() {
        List<String> out = new ArrayList<>();
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("types");
        if (section != null) {
            out.addAll(section.getKeys(false));
        }
        return out;
    }

    public String getTypeName(String type) {
        return config.getString("types." + type + ".name", type);
    }

    public List<String> getReasons(String type) {
        return config.getStringList("types." + type + ".reasons");
    }

    public List<String> getDurations() {
        return config.getStringList("durations");
    }
}
