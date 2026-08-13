package com.alkacode.alkaessentials.manager;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Nicks de exibicao (/nick). Persistidos em nicks.yml, chaveado por UUID. */
public final class NickManager {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public NickManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "nicks.yml");
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
            plugin.getLogger().severe("Falha ao salvar nicks.yml: " + e.getMessage());
        }
    }

    public String getNick(UUID uuid) {
        return config.getString(uuid.toString());
    }

    public boolean hasNick(UUID uuid) {
        return config.contains(uuid.toString());
    }

    public void setNick(UUID uuid, String nick) {
        config.set(uuid.toString(), nick);
        save();
    }

    public void clearNick(UUID uuid) {
        config.set(uuid.toString(), null);
        save();
    }

    /** Aplica cores/estilos (prefixo MiniMessage) ao nick atual do jogador (ou ao nome real, se sem nick).
     * Retorna o texto limpo do nick. */
    public String applyColorToNick(Player player, String prefix) {
        String currentPlain = hasNick(player.getUniqueId())
                ? MiniMessage.miniMessage().stripTags(getNick(player.getUniqueId())).trim()
                : player.getName();
        String mini = prefix + currentPlain;
        setNick(player.getUniqueId(), mini);
        player.displayName(MiniMessage.miniMessage().deserialize(mini));
        player.customName(MiniMessage.miniMessage().deserialize(mini));
        return currentPlain;
    }

    /** Busca o UUID real de quem usa o nick informado (case-insensitive, ignora cores/tags). */
    public UUID findRealName(String nick) {
        if (nick == null) return null;
        String plain = MiniMessage.miniMessage().stripTags(nick).trim();
        for (Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
            if (entry.getValue() == null) continue;
            String stored = MiniMessage.miniMessage().stripTags(entry.getValue().toString()).trim();
            if (stored.equalsIgnoreCase(plain)) {
                return UUID.fromString(entry.getKey());
            }
        }
        return null;
    }
}
