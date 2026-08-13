package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Seguranca de comandos: bloqueia comandos de infraestrutura (/pl, /ver, etc.). */
public final class CommandSecurityListener implements Listener {

    private final JavaPlugin plugin;

    public CommandSecurityListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("command-security.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        String bypass = plugin.getConfig().getString("command-security.bypass-permission",
                "alkassentials.commands.block.bypass");
        if (player.hasPermission(bypass)) {
            return;
        }
        String message = event.getMessage().trim();
        if (message.isEmpty() || message.charAt(0) != '/') {
            return;
        }
        // extrai o comando principal (primeira palavra sem o '/')
        String command = message.substring(1).split(" ")[0].toLowerCase();
        List<String> blocked = plugin.getConfig().getStringList("command-security.blocked");
        for (String blockedCommand : blocked) {
            if (command.equals(blockedCommand.toLowerCase())) {
                event.setCancelled(true);
                ChatUtil.sendKey(player, "blocked-command");
                return;
            }
        }
    }
}
