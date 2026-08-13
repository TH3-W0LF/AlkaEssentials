package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comando /ride - monta na entidade que esta olhando (players so com permissao). */
public final class RideCommand extends BaseCommand {

    public RideCommand(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePerm(player, "alkassentials.qol.ride")) {
            return true;
        }
        Entity target = player.getTargetEntity(6, true);
        if (target == null || target.equals(player)) {
            ChatUtil.sendKey(player, "qol-ride-none");
            return true;
        }
        if (target instanceof Player && !player.hasPermission("alkassentials.qol.ride.others")) {
            ChatUtil.sendKey(player, "no-permission");
            return true;
        }
        target.addPassenger(player);
        ChatUtil.sendKey(player, "qol-ride", Map.of("entity", target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
