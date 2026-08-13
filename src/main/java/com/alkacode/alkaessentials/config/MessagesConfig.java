package com.alkacode.alkaessentials.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Centraliza TODAS as mensagens de chat do AlkaEssentials no messages.yml (mesmo
 * padrao do MenuConfig pra GUIs) - tudo editavel sem recompilar. Formatacao
 * MiniMessage (regra R3 do studio: ZERO codigo legacy em mensagem de chat).
 * Instancia unica (MessagesConfig.getInstance()) setada no onEnable.
 */
public final class MessagesConfig {

    private static MessagesConfig instance;
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private String prefix;

    private MessagesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public static void init(JavaPlugin plugin) {
        instance = new MessagesConfig(plugin);
    }

    public static MessagesConfig getInstance() {
        return instance;
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        mergeMissingDefaults();
        prefix = config.getString("prefix", "<dark_gray>[<gold>AlkaEssentials<dark_gray>] ");
    }

    /**
     * Adiciona ao arquivo do disco qualquer chave que exista no messages.yml do jar mas
     * nao no arquivo salvo (migracao de versao: o saveResource(false) nao sobrescreve o
     * arquivo existente, entao chaves novas de updates passados sumiriam sem isso).
     * Preserva todas as edicoes que o admin ja fez.
     */
    private void mergeMissingDefaults() {
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao migrar messages.yml: " + e.getMessage());
        }
    }

    public String getRaw(String key) {
        String value = config.getString(key);
        if (value == null) {
            plugin.getLogger().warning("Mensagem nao encontrada em messages.yml: " + key);
            return "<red>[Missing: " + key + "]";
        }
        return value;
    }

    public String get(String key, Map<String, String> placeholders) {
        String value = getRaw(key);
        value = value.replace("{prefix}", prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                value = value.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return value;
    }

    public String get(String key) {
        return get(key, null);
    }

    /** Acesso cru ao YAML carregado - permite ler secoes/lista/material com MiniMessage. */
    public YamlConfiguration getYaml() {
        return config;
    }

    public Component getComponent(String key, Map<String, String> placeholders) {
        return MiniMessage.miniMessage().deserialize(get(key, placeholders));
    }

    public Component getComponent(String key) {
        return getComponent(key, null);
    }
}
