package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.afk.AfkZoneManager;
import com.alkacode.alkaessentials.afk.ZoneSelection;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/** Varinha de zona AFK: clique direito define o canto 1, clique esquerdo o canto 2. */
public final class WandListener implements Listener {

    private final AfkZoneManager manager;
    private final NamespacedKey wandKey;

    public WandListener(JavaPlugin plugin, AfkZoneManager manager) {
        this.manager = manager;
        this.wandKey = new NamespacedKey(plugin, "afk_wand");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR
                || item.getItemMeta() == null
                || !item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.STRING)) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        ZoneSelection selection = manager.getSelection(player.getUniqueId());
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setPosition1(event.getClickedBlock().getLocation());
            sendPos(player, "afkzone-pos1", event.getClickedBlock().getLocation());
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selection.setPosition2(event.getClickedBlock().getLocation());
            sendPos(player, "afkzone-pos2", event.getClickedBlock().getLocation());
        }
    }

    private void sendPos(Player player, String key, org.bukkit.Location loc) {
        ChatUtil.sendKey(player, key, Map.of(
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ())));
    }
}
