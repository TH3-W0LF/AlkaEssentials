package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.BackManager;
import com.alkacode.alkaessentials.manager.WarmupManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Gatilhos de spawn: primeiro login, reviver, void - e limpeza de warmup/back no quit. */
public final class TeleportListener implements Listener {

    private final JavaPlugin plugin;
    private final LocationStoreRef spawn;
    private final BackManager back;
    private final WarmupManager warmups;
    private final Set<UUID> voidHandled = new HashSet<>();

    public TeleportListener(JavaPlugin plugin, LocationStoreRef spawn, BackManager back, WarmupManager warmups) {
        this.plugin = plugin;
        this.spawn = spawn;
        this.back = back;
        this.warmups = warmups;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("spawn.teleport-on-first-join", true)
                && !event.getPlayer().hasPlayedBefore()) {
            Location spawnLoc = spawn.get();
            if (spawnLoc != null) {
                event.getPlayer().teleport(spawnLoc);
                ChatUtil.sendKey(event.getPlayer(), "first-join",
                        java.util.Map.of("player", event.getPlayer().getName()));
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("spawn.teleport-on-respawn", false)) {
            return;
        }
        Location spawnLoc = spawn.get();
        if (spawnLoc != null) {
            event.setRespawnLocation(spawnLoc);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("spawn.teleport-on-void", true)) {
            return;
        }
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        int threshold = plugin.getConfig().getInt("spawn.void-threshold", -64);
        boolean below = event.getTo().getY() < threshold;

        if (!below) {
            voidHandled.remove(uuid);
            return;
        }
        if (!voidHandled.add(uuid)) {
            return;
        }
        Location spawnLoc = spawn.get();
        if (spawnLoc == null) {
            return;
        }
        back.save(uuid, event.getTo().clone());
        event.setTo(spawnLoc);
        ChatUtil.sendKey(event.getPlayer(), "spawn-teleported");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getEntity() != null) {
            back.save(event.getEntity().getUniqueId(), event.getEntity().getLocation().clone());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        warmups.cancelIfPresent(uuid);
        voidHandled.remove(uuid);
    }

    /** Referencia resolvida por chamada pro spawn, desacoplada do store pra o listener nao segurar estado pesado. */
    public interface LocationStoreRef {
        Location get();
    }
}
