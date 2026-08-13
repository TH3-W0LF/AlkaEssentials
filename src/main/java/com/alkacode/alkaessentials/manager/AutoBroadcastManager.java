package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Auto-Broadcast: envia mensagens em loop do config.yml (chat.autobroadcast). */
public final class AutoBroadcastManager {

    private final JavaPlugin plugin;
    private int index;

    public AutoBroadcastManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Envia a proxima mensagem do ciclo para todos os online. */
    public void broadcastNext() {
        List<String> messages = plugin.getConfig().getStringList("chat.autobroadcast.messages");
        if (messages.isEmpty()) {
            return;
        }
        if (index >= messages.size()) {
            index = 0;
        }
        String message = messages.get(index++);
        if (message.isBlank()) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            ChatUtil.send(online, message);
        }
    }
}
