package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.DeathChestManager;
import com.alkacode.alkaessentials.manager.InvRestoreManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Modulo de morte: snapshot pro InvRestore, tumulo (deathchest) e mensagem de morte customizada. */
public final class DeathListener implements Listener {

    private final JavaPlugin plugin;
    private final InvRestoreManager invRestore;
    private final DeathChestManager deathChest;

    public DeathListener(JavaPlugin plugin, InvRestoreManager invRestore, DeathChestManager deathChest) {
        this.plugin = plugin;
        this.invRestore = invRestore;
        this.deathChest = deathChest;
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

        // mensagem de morte customizada
        if (plugin.getConfig().getBoolean("death-message.enabled", true)) {
            String message;
            if (player.getKiller() != null) {
                message = "death-message-killer";
            } else {
                message = "death-message";
            }
            String killerName = player.getKiller() != null ? player.getKiller().getName() : "";
            event.deathMessage(null);
            for (Player online : Bukkit.getOnlinePlayers()) {
                ChatUtil.sendKey(online, message, Map.of(
                        "player", player.getName(),
                        "killer", killerName));
            }
        }
    }
}
