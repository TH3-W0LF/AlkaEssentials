package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.WarmupManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Cancela o warmup de teleporte se o jogador andar (acima do limiar) ou tomar dano. */
public final class WarmupListener implements Listener {

    private final JavaPlugin plugin;
    private final WarmupManager warmups;

    public WarmupListener(JavaPlugin plugin, WarmupManager warmups) {
        this.plugin = plugin;
        this.warmups = warmups;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!warmups.hasPending(player.getUniqueId())) {
            return;
        }
        if (!plugin.getConfig().getBoolean("teleport.cancel-warmup-on-move", true)) {
            return;
        }
        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            warmups.cancel(player.getUniqueId());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!warmups.hasPending(player.getUniqueId())) {
            return;
        }
        if (!plugin.getConfig().getBoolean("teleport.cancel-warmup-on-damage", true)) {
            return;
        }
        warmups.cancel(player.getUniqueId());
    }
}
