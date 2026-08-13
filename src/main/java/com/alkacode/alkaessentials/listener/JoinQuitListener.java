package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

/** Mensagens customizadas de join, quit e troca de mundo (editaveis em messages.yml). */
public final class JoinQuitListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!Bukkit.getPluginManager().isPluginEnabled("nChat")) {
            String msg = MessagesConfig.getInstance().get("join-message",
                    Map.of("player", event.getPlayer().getName()));
            event.joinMessage(ChatUtil.parse(msg));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!Bukkit.getPluginManager().isPluginEnabled("nChat")) {
            String msg = MessagesConfig.getInstance().get("quit-message",
                    Map.of("player", event.getPlayer().getName()));
            event.quitMessage(ChatUtil.parse(msg));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (Bukkit.getPluginManager().isPluginEnabled("nChat")) {
            return;
        }
        Player player = event.getPlayer();
        String msg = MessagesConfig.getInstance().get("world-change-message", Map.of(
                "player", player.getName(),
                "world", player.getWorld().getName()));
        player.sendMessage(ChatUtil.parse(msg));
    }
}
