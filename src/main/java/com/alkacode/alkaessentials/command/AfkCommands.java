package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.afk.AfkManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Comando /afk - alterna o modo AFK manual. */
public final class AfkCommands extends BaseCommand {

    private final AfkManager afkManager;

    public AfkCommands(JavaPlugin plugin, AfkManager afkManager) {
        super(plugin);
        this.afkManager = afkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        boolean nowAfk = !afkManager.isAfk(player.getUniqueId());
        afkManager.setManualAfk(player, nowAfk);
        ChatUtil.sendKey(player, nowAfk ? "afk-enabled" : "afk-disabled");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
