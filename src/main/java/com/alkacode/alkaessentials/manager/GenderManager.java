package com.alkacode.alkaessentials.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Sexo (M/F) exibido no TAB/chat (/genero). Persistido em genero.yml, chaveado por
 * UUID - mesmo padrao leve do NickManager (nicks.yml), sem precisar de tabela no
 * banco pra um dado tao simples. Sem GUI de proposito (comando so por enquanto, ver
 * [[project-alkaflair]]/pedido do usuario 21/08 - GUI fica pra depois). */
public final class GenderManager {

    public enum Gender {
        M, F
    }

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public GenderManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "genero.yml");
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
            plugin.getLogger().severe("Falha ao salvar genero.yml: " + e.getMessage());
        }
    }

    public Gender get(UUID uuid) {
        String raw = config.getString(uuid.toString());
        if (raw == null) {
            return null;
        }
        try {
            return Gender.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void set(UUID uuid, Gender gender) {
        config.set(uuid.toString(), gender.name());
        save();
    }
}
