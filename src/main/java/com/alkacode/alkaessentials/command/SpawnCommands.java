package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.manager.LocationStore;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Comandos de spawn: /spawn, /setspawn, /delspawn. */
public final class SpawnCommands extends BaseCommand {

    private final LocationStore locations;
    private final TeleportService teleports;

    public SpawnCommands(JavaPlugin plugin, LocationStore locations, TeleportService teleports) {
        super(plugin);
        this.locations = locations;
        this.teleports = teleports;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "spawn":
                return spawn(sender);
            case "setspawn":
                return setSpawn(sender);
            case "delspawn":
                return delSpawn(sender);
            default:
                return false;
        }
    }

    private boolean spawn(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        Location spawn = locations.getSpawn();
        if (spawn == null) {
            ChatUtil.sendKey(player, "no-spawn-set");
            return true;
        }
        teleports.teleport(player, spawn, "spawn", true, "spawn-teleported", null);
        return true;
    }

    private boolean setSpawn(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePerm(sender, "alkassentials.admin.setspawn")) {
            return true;
        }
        locations.setSpawn(player.getLocation());
        ChatUtil.sendKey(player, "spawn-set");
        return true;
    }

    private boolean delSpawn(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePerm(sender, "alkassentials.admin.delspawn")) {
            return true;
        }
        locations.removeSpawn();
        ChatUtil.sendKey(player, "spawn-del");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
