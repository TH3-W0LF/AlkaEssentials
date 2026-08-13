package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Restricao de itens: impede colocar materiais proibidos e crafts bloqueados. */
public final class ItemProtectionListener implements Listener {

    private final JavaPlugin plugin;

    public ItemProtectionListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (isBlocked(event.getBlock().getType(), "items.blocked-place")) {
            event.setCancelled(true);
            ChatUtil.sendKey(event.getPlayer(), "blocked-item");
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (result == null || !isBlocked(result.getType(), "items.blocked-craft")) {
            return;
        }
        event.getInventory().setResult(null);
    }

    private boolean isBlocked(Material material, String path) {
        List<String> blocked = plugin.getConfig().getStringList(path);
        for (String name : blocked) {
            if (material.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
