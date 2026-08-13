package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.config.EventsConfig;
import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.config.ReasonsConfig;
import com.alkacode.alkaessentials.scoreboard.ScoreboardManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comando base: /alkaessentials (help) e /alkaessentials reload (recarrega todas as configs). */
public final class AlkaEssentialsCommand extends BaseCommand {

    private final ScoreboardManager scoreboardManager;

    public AlkaEssentialsCommand(JavaPlugin plugin, ScoreboardManager scoreboardManager) {
        super(plugin);
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return reload(sender);
        }
        help(sender);
        return true;
    }

    private void help(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        for (String line : MessagesConfig.getInstance().getYaml().getStringList("alka-help")) {
            ChatUtil.send(sender, line.replace("{version}", version));
        }
    }

    private boolean reload(CommandSender sender) {
        if (!requirePerm(sender, "alkassentials.admin.reload")) {
            return true;
        }
        plugin.reloadConfig();
        MessagesConfig.getInstance().reload();
        MenuConfig.getInstance().reload();
        ReasonsConfig.getInstance().reload();
        EventsConfig.getInstance().reload();
        scoreboardManager.reload();
        ChatUtil.sendKey(sender, "alka-reloaded");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "help");
        }
        return List.of();
    }
}
