package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Base comum dos comandos - helpers de checagem de jogador e permissao. */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final JavaPlugin plugin;

    protected BaseCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    protected Player asPlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatUtil.sendKey(sender, "player-only");
            return null;
        }
        return player;
    }

    protected boolean requirePerm(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        ChatUtil.sendKey(sender, "no-permission");
        return false;
    }

    protected Player matchPlayer(String name) {
        return plugin.getServer().getPlayer(name);
    }
}
