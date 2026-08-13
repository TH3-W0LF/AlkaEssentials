package com.alkacode.alkaessentials.config;

import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Centraliza titulos/nomes/lores de TODAS as GUIs no menus.yml (formatacao estilo
 * rankup) - tudo editavel sem recompilar. Itens sao definidos por menus.yml.<caminho>
 * com material/name/lore; placeholders passados como {chave} sao substituidos na hora.
 * Instancia unica (MenuConfig.getInstance()) setada no onEnable.
 */
public final class MenuConfig {

    private static MenuConfig instance;

    public static MenuConfig getInstance() {
        return instance;
    }

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    private MenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "menus.yml");
        reload();
    }

    public static void init(JavaPlugin plugin) {
        instance = new MenuConfig(plugin);
    }

    public void reload() {
        if (!file.exists()) {
            try {
                plugin.saveResource("menus.yml", false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "menus.yml nao encontrado no jar - usando vazio.", e);
                config = new YamlConfiguration();
                return;
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        mergeMissingDefaults();
    }

    /** Adiciona chaves novas do menus.yml do jar ao arquivo salvo (migracao de versao). */
    private void mergeMissingDefaults() {
        try (InputStream in = plugin.getResource("menus.yml")) {
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
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao migrar menus.yml", e);
        }
    }

    public String title(String path, Map<String, String> placeholders) {
        return apply(config.getString(path, ""), placeholders);
    }

    public YamlConfiguration getYaml() {
        return config;
    }

    /** Constroi o ItemStack a partir de menus.yml.<path> (material/name/lore) com placeholders. */
    public ItemStack item(String path, Map<String, String> placeholders) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return new ItemStack(Material.STONE);
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String name = name(path, placeholders);
        if (name != null && !name.isEmpty()) {
            meta.displayName(ChatUtil.parse(name));
        }
        List<Component> lore = lore(path, placeholders);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Nome (string MiniMessage) de menus.yml.<path> com placeholders aplicados. */
    public String name(String path, Map<String, String> placeholders) {
        return apply(config.getString(path + ".name", ""), placeholders);
    }

    /** Lore (lista de Component) de menus.yml.<path> com placeholders aplicados. */
    public List<Component> lore(String path, Map<String, String> placeholders) {
        List<Component> loreList = new ArrayList<>();
        for (String line : config.getStringList(path + ".lore")) {
            loreList.add(ChatUtil.parse(apply(line, placeholders)));
        }
        return loreList;
    }

    private static String apply(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
