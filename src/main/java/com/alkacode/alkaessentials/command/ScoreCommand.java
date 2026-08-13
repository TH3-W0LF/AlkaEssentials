package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.scoreboard.ScoreboardManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comando /scoreboard: toggle (jogador) e reload (admin). */
public final class ScoreCommand extends BaseCommand {

    private final ScoreboardManager manager;

    public ScoreCommand(JavaPlugin plugin, ScoreboardManager manager) {
        super(plugin);
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return reload(sender);
        }
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePerm(player, "alkassentials.scoreboard.toggle")) {
            return true;
        }
        boolean nowOff = manager.toggle(player);
        ChatUtil.sendKey(player, nowOff ? "scoreboard-off" : "scoreboard-on");
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!requirePerm(sender, "alkassentials.scoreboard.reload")) {
            return true;
        }
        manager.reload();
        ChatUtil.sendKey(sender, "scoreboard-reloaded", Map.of(
                "count", String.valueOf(manager.getScoreboardCount()),
                "tabs", String.valueOf(manager.getTablistCount())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("toggle", "reload");
        }
        return List.of();
    }
}
