package com.alkacode.alkaessentials.scoreboard;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Carrega scoreboards.yml e tablists.yml + a config de nome do TAB (config.yml). */
public final class ScoreboardConfig {

    private final JavaPlugin plugin;
    private final Map<String, AlkaScoreboard> scoreboards = new HashMap<>();
    private final Map<String, AlkaTablist> tablists = new HashMap<>();
    private boolean playerNameEnabled;
    private String playerNameFormat;

    public ScoreboardConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        loadScoreboards();
        loadTablists();
        this.playerNameEnabled = plugin.getConfig().getBoolean("scoreboard.player-name.enabled", true);
        this.playerNameFormat = plugin.getConfig().getString("scoreboard.player-name.format", "<gray>{player}");
    }

    private void loadScoreboards() {
        scoreboards.clear();
        YamlConfiguration config = loadResource("scoreboards.yml");
        if (config == null) {
            return;
        }
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            int priority = section.getInt("priority", 0);
            List<String> worlds = section.getStringList("worlds");
            // WorldGuard sempre normaliza IDs de regiao pra minusculo - normaliza aqui tambem
            // pra "Pesca" no YAML nao deixar de bater com a regiao real "pesca".
            List<String> regions = section.getStringList("regions").stream()
                    .map(r -> r.toLowerCase(java.util.Locale.ROOT))
                    .toList();
            String permission = section.getString("permission", "");
            ScoreboardEntry title = parseEntry(section.getConfigurationSection("title"));
            List<ScoreboardEntry> lines = parseLines(section.getList("lines"));
            scoreboards.put(id, new AlkaScoreboard(id, priority, worlds, regions,
                    permission, title, lines));
        }
    }

    private void loadTablists() {
        tablists.clear();
        YamlConfiguration config = loadResource("tablists.yml");
        if (config == null) {
            return;
        }
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            tablists.put(id, new AlkaTablist(
                    id,
                    section.getStringList("worlds"),
                    section.getString("permission", ""),
                    section.getStringList("header"),
                    section.getStringList("footer")));
        }
    }

    private YamlConfiguration loadResource(String resource) {
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private ScoreboardEntry parseEntry(ConfigurationSection section) {
        if (section == null) {
            return new ScoreboardEntry(List.of(""), 0.0, "");
        }
        List<String> frames = asStringList(section.get("frames"));
        double interval = section.getDouble("interval", 0.0);
        String condition = section.getString("condition", "");
        return new ScoreboardEntry(frames, interval, condition);
    }

    private List<ScoreboardEntry> parseLines(List<?> list) {
        List<ScoreboardEntry> entries = new ArrayList<>();
        if (list == null) {
            return entries;
        }
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                List<String> frames = asStringList(m.get("frames"));
                double interval = m.get("interval") instanceof Number n ? n.doubleValue() : 0.0;
                Object cond = m.get("condition");
                String condition = cond == null ? "" : String.valueOf(cond);
                entries.add(new ScoreboardEntry(frames, interval, condition));
            }
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value == null) {
            return List.of("");
        }
        if (value instanceof String s) {
            return List.of(s);
        }
        if (value instanceof List<?> l) {
            List<String> result = new ArrayList<>();
            for (Object o : l) {
                result.add(String.valueOf(o));
            }
            return result;
        }
        return List.of("");
    }

    public AlkaScoreboard getScoreboard(String id) {
        return scoreboards.get(id);
    }

    public Map<String, AlkaScoreboard> getScoreboards() {
        return scoreboards;
    }

    public AlkaTablist getTablist(String id) {
        return tablists.get(id);
    }

    public Map<String, AlkaTablist> getTablists() {
        return tablists;
    }

    public boolean isPlayerNameEnabled() {
        return playerNameEnabled;
    }

    public String getPlayerNameFormat() {
        return playerNameFormat;
    }
}
