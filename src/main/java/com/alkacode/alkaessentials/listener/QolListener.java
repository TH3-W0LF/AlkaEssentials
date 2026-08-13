package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.gui.TrashInventory;
import com.alkacode.alkaessentials.manager.CooldownManager;
import com.alkacode.alkaessentials.manager.SeatManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Qualidade de vida: sopa cura, cooldown de ender pearl, bigorna infinita + lore "Forjado por", eject do /sit. */
public final class QolListener implements Listener {

    private final JavaPlugin plugin;
    private final CooldownManager cooldowns;
    private final SeatManager seats;

    public QolListener(JavaPlugin plugin, CooldownManager cooldowns, SeatManager seats) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.seats = seats;
    }

    // ------------------------- Soup Heal -------------------------

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getConfig().getBoolean("qol.soup.enabled", true)) {
            return;
        }
        Material type = event.getItem().getType();
        if (type != Material.MUSHROOM_STEW && type != Material.SUSPICIOUS_STEW) {
            return;
        }
        Player player = event.getPlayer();
        List<String> worlds = plugin.getConfig().getStringList("qol.soup.worlds");
        if (!worlds.isEmpty() && !worlds.contains(player.getWorld().getName())) {
            return;
        }
        int heal = plugin.getConfig().getInt("qol.soup.heal-amount", 7);
        double max = 20.0;
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr != null) {
            max = attr.getValue();
        }
        player.setHealth(Math.min(player.getHealth() + heal, max));
    }

    // ------------------------- Ender Pearl Cooldown -------------------------

    @EventHandler
    public void onPearl(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("qol.ender-pearl.enabled", true)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_PEARL) {
            return;
        }
        Player player = event.getPlayer();
        long remaining = cooldowns.remainingSeconds(player.getUniqueId(), "ender_pearl");
        if (remaining > 0) {
            event.setCancelled(true);
            ChatUtil.sendKey(player, "qol-pearl-cooldown",
                    Map.of("time", TimeUtil.formatSeconds(remaining)));
            return;
        }
        int seconds = plugin.getConfig().getInt("qol.ender-pearl.cooldown-seconds", 16);
        cooldowns.set(player.getUniqueId(), "ender_pearl", seconds);
    }

    // ------------------------- Bigorna -------------------------

    @EventHandler
    public void onAnvilLore(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("qol.anvil.add-forge-lore", true)) {
            return;
        }
        if (!(event.getView().getTopInventory() instanceof AnvilInventory) || event.getRawSlot() != 2) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        if (result == null) {
            return;
        }
        if (isExcluded(result.getType())) {
            return;
        }
        String forge = plugin.getConfig().getString("qol.anvil.forge-lore", "<gray>Forjado por <white>{player}")
                .replace("{player}", player.getName());
        ItemMeta meta = result.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(ChatUtil.parse(forge));
        meta.lore(lore);
        result.setItemMeta(meta);
    }

    @EventHandler
    public void onAnvilBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("qol.anvil.infinite", true)) {
            return;
        }
        if (isAnvil(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvilFallDestroy(EntityChangeBlockEvent event) {
        if (!plugin.getConfig().getBoolean("qol.anvil.infinite", true)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof FallingBlock falling && isAnvil(falling.getBlockData().getMaterial())) {
            event.setCancelled(true);
        }
    }

    private boolean isAnvil(Material material) {
        return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
    }

    private boolean isExcluded(Material material) {
        List<String> excluded = plugin.getConfig().getStringList("qol.anvil.excluded-materials");
        for (String name : excluded) {
            if (material.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------- Lixeira (apagar ao fechar) -------------------------

    @EventHandler
    public void onTrashClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TrashInventory) {
            event.getInventory().clear();
        }
    }

    // ------------------------- /sit (levantar) -------------------------

    @EventHandler
    public void onSeatMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!seats.isSeated(player.getUniqueId()) || seats.ignoreMove(player.getUniqueId())) {
            return;
        }
        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            seats.unseat(player);
        }
    }

    @EventHandler
    public void onSeatDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && seats.isSeated(player.getUniqueId())) {
            seats.unseat(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        seats.handleQuit(event.getPlayer().getUniqueId());
    }
}
