package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.gui.WorldRulesGui;
import com.alkacode.alkaessentials.manager.WorldRulesManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comando /worldrules - abre o menu de regras por mundo. */
public final class WorldRulesCommand extends BaseCommand {

    private final WorldRulesManager rules;

    public WorldRulesCommand(JavaPlugin plugin, WorldRulesManager rules) {
        super(plugin);
        this.rules = rules;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        if (!requirePerm(player, "alkassentials.worldrules")) return true;
        if (args.length >= 1) {
            World world = Bukkit.getWorld(args[0]);
            if (world == null) {
                ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[0]));
                return true;
            }
            new com.alkacode.alkaessentials.gui.WorldRuleToggleGui(plugin, player, rules, world).open();
            return true;
        }
        new WorldRulesGui(plugin, player, rules).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getWorlds().stream().map(World::getName).toList();
        }
        return List.of();
    }
}
