package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.manager.DeathChestManager;
import com.alkacode.alkaessentials.manager.InvRestoreManager;
import com.alkacode.alkaessentials.manager.KillStreakManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Modulo de morte: snapshot pro InvRestore, tumulo (deathchest), killstreak e mensagem
 * de morte customizada (causa especifica, PVP com arma/distancia). */
public final class DeathListener implements Listener {

    private final JavaPlugin plugin;
    private final InvRestoreManager invRestore;
    private final DeathChestManager deathChest;
    private final KillStreakManager killStreaks;

    public DeathListener(JavaPlugin plugin, InvRestoreManager invRestore, DeathChestManager deathChest,
                          KillStreakManager killStreaks) {
        this.plugin = plugin;
        this.invRestore = invRestore;
        this.deathChest = deathChest;
        this.killStreaks = killStreaks;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // snapshot do inventario pro /invrestore
        if (plugin.getConfig().getBoolean("invrestore.enabled", true)) {
            invRestore.save(player);
        }

        // tumulo de morte
        boolean chestEnabled = plugin.getConfig().getBoolean("deathchest.enabled", true);
        List<String> worlds = plugin.getConfig().getStringList("deathchest.worlds");
        if (chestEnabled && (worlds.isEmpty() || worlds.contains(player.getWorld().getName()))) {
            List<ItemStack> drops = new ArrayList<>(event.getDrops());
            if (!drops.isEmpty() && deathChest.spawn(player, drops)) {
                event.getDrops().clear();
            }
        }

        Player killer = player.getKiller();
        int endedStreak = killStreaks.onDeath(player.getUniqueId());

        if (plugin.getConfig().getBoolean("death-message.enabled", true)) {
            event.deathMessage(null);
            broadcastDeathMessage(player, killer);
        }

        if (killer == null) {
            return;
        }
        boolean killstreakEnabled = plugin.getConfig().getBoolean("death-message.killstreak.enabled", true);
        if (!killstreakEnabled) {
            return;
        }
        int streak = killStreaks.onKill(killer.getUniqueId());
        List<Integer> milestones = plugin.getConfig().getIntegerList("death-message.killstreak.milestones");
        if (milestones.contains(streak)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                ChatUtil.sendKey(online, "death-messages.killstreak.milestone",
                        Map.of("killer", killer.getName(), "streak", String.valueOf(streak)));
            }
        }
        // so anuncia o fim de uma sequencia que ja tinha virado noticia (bateu um milestone)
        if (endedStreak >= (milestones.isEmpty() ? Integer.MAX_VALUE : milestones.get(0))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                ChatUtil.sendKey(online, "death-messages.killstreak.ended",
                        Map.of("player", killer.getName(), "killer", player.getName(),
                                "streak", String.valueOf(endedStreak)));
            }
        }
    }

    private void broadcastDeathMessage(Player victim, Player killer) {
        String key;
        Map<String, String> placeholders;

        if (killer != null) {
            ItemStack weaponItem = killer.getInventory().getItemInMainHand();
            boolean ranged = isRanged(victim);
            double distance = killer.getLocation().distance(victim.getLocation());
            placeholders = Map.of(
                    "player", victim.getName(),
                    "killer", killer.getName(),
                    "weapon", weaponName(weaponItem),
                    "distance", String.format(Locale.US, "%.1f", distance)
            );
            if (ranged) {
                key = "death-messages.pvp.ranged";
            } else if (!weaponItem.getType().isAir()) {
                key = "death-messages.pvp.with-weapon";
            } else {
                key = "death-messages.pvp.default";
            }
        } else {
            EntityDamageEvent lastDamage = victim.getLastDamageCause();
            String causeKey = lastDamage != null ? "death-messages.causes." + lastDamage.getCause().name() : null;
            key = causeKey != null && MessagesConfig.getInstance().getYaml().contains(causeKey)
                    ? causeKey : "death-messages.default";
            placeholders = Map.of("player", victim.getName());
        }

        int radius = plugin.getConfig().getInt("death-message.broadcast-radius", 0);
        Location center = victim.getLocation();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (radius > 0 && (!online.getWorld().equals(center.getWorld())
                    || online.getLocation().distanceSquared(center) > (double) radius * radius)) {
                continue;
            }
            ChatUtil.sendKey(online, key, placeholders);
        }
    }

    /** Ranged = a ultima causa de dano veio de um Projectile (flecha/tridente/etc) - o
     * PlayerDeathEvent#getKiller ja resolve o atirador, entao so precisamos saber SE foi
     * por projetil pra escolher o template "ranged" em vez de "with-weapon". */
    private boolean isRanged(Player victim) {
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        return lastDamage instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Projectile;
    }

    private String weaponName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "as maos";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        String raw = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] words = raw.split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
