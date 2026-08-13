package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.afk.AfkManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Qualquer interacao zera a inatividade do auto-afk; sai do jogo limpa o estado. */
public final class AfkActivityListener implements Listener {

    private final AfkManager afkManager;

    public AfkActivityListener(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            afkManager.updateActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        afkManager.handleQuit(event.getPlayer().getUniqueId());
    }
}
