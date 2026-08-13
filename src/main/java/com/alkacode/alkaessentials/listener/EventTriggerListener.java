package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.config.EventsConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Event-Trigger Commands: roda comandos do events.yml em gatilhos do jogador. */
public final class EventTriggerListener implements Listener {

    private final JavaPlugin plugin;

    public EventTriggerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String key = player.hasPlayedBefore() ? "on-join" : "on-first-join";
        run(player, key);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        run(event.getPlayer(), "on-quit");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        run(event.getEntity(), "on-death");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        run(event.getPlayer(), "on-respawn");
    }

    private void run(Player player, String key) {
        for (String command : EventsConfig.getInstance().getCommands(key)) {
            if (command.isBlank()) {
                continue;
            }
            String cmd = command.replace("{player}", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }
    }
}
