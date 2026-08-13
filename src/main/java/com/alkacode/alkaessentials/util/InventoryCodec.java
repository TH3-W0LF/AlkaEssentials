package com.alkacode.alkaessentials.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Codifica/decodifica um array de ItemStack em Base64 usando a serializacao YAML do
 * Bukkit (ItemStack#serialize / ItemStack#deserialize) - sem depender das classes de
 * ObjectOutputStream do Bukkit, que sao deprecadas.
 */
public final class InventoryCodec {

    private InventoryCodec() {
    }

    public static String encode(ItemStack[] items) {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (ItemStack item : items) {
            list.add(item == null ? null : item.serialize());
        }
        config.set("items", list);
        return Base64.getEncoder().encodeToString(config.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static ItemStack[] decode(String source) {
        String yaml = new String(Base64.getDecoder().decode(source), StandardCharsets.UTF_8);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        List<Map<?, ?>> list = config.getMapList("items");
        ItemStack[] items = new ItemStack[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Map<?, ?> map = list.get(i);
            if (map == null || map.isEmpty()) {
                items[i] = null;
                continue;
            }
            items[i] = ItemStack.deserialize((Map<String, Object>) map);
        }
        return items;
    }
}
