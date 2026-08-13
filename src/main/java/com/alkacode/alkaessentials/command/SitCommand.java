package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.manager.SeatManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Comando /sit - senta/levanta. */
public final class SitCommand extends BaseCommand {

    private final SeatManager seats;

    public SitCommand(JavaPlugin plugin, SeatManager seats) {
        super(plugin);
        this.seats = seats;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePerm(player, "alkassentials.qol.sit")) {
            return true;
        }
        boolean wasSeated = seats.isSeated(player.getUniqueId());
        seats.toggle(player);
        if (wasSeated) {
            ChatUtil.sendKey(player, "qol-sit-off");
        } else if (seats.isSeated(player.getUniqueId())) {
            ChatUtil.sendKey(player, "qol-sit-on");
        } else {
            ChatUtil.sendKey(player, "qol-sit-blocked");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
