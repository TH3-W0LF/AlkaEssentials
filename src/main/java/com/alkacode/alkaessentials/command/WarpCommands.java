package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.gui.WarpGui;
import com.alkacode.alkaessentials.manager.LocationStore;
import com.alkacode.alkaessentials.model.Warp;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Comandos de warp: /warp (menu sem arg), /setwarp, /delwarp. */
public final class WarpCommands extends BaseCommand {

    private final LocationStore locations;
    private final TeleportService teleports;

    public WarpCommands(JavaPlugin plugin, LocationStore locations, TeleportService teleports) {
        super(plugin);
        this.locations = locations;
        this.teleports = teleports;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "warp":
                return warp(sender, args);
            case "setwarp":
                return setWarp(sender, args);
            case "delwarp":
                return delWarp(sender, args);
            default:
                return false;
        }
    }

    private boolean warp(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            new WarpGui(plugin, player, locations, teleports).open();
            return true;
        }
        Warp warp = locations.getWarp(args[0]);
        if (warp == null || warp.getLocation() == null) {
            ChatUtil.sendKey(player, "warp-not-found", Map.of("warp", args[0]));
            return true;
        }
        if (warp.hasPermission() && !player.hasPermission(warp.getPermission())) {
            ChatUtil.sendKey(player, "warp-no-access", Map.of("warp", warp.getName()));
            return true;
        }
        teleports.teleport(player, warp.getLocation(), "warp", true,
                "warp-teleported", Map.of("warp", warp.getName()));
        return true;
    }

    private boolean setWarp(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePerm(sender, "alkassentials.admin.setwarp")) {
            return true;
        }
        if (args.length < 1) {
            ChatUtil.sendKey(player, "warp-invalid-name");
            return true;
        }
        String name = args[0];
        if (name.contains(" ") || name.contains(";")) {
            ChatUtil.sendKey(player, "warp-invalid-name");
            return true;
        }
        if (locations.hasWarp(name)) {
            ChatUtil.sendKey(player, "warp-exists", Map.of("warp", name));
            return true;
        }
        String permission = args.length > 1 ? args[1] : null;
        locations.setWarp(name, player.getLocation(), permission);
        ChatUtil.sendKey(player, "warp-set", Map.of("warp", name));
        return true;
    }

    private boolean delWarp(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePerm(sender, "alkassentials.admin.delwarp")) {
            return true;
        }
        if (args.length < 1 || !locations.hasWarp(args[0])) {
            ChatUtil.sendKey(player, "warp-not-found", Map.of("warp", args.length > 0 ? args[0] : "?"));
            return true;
        }
        locations.removeWarp(args[0]);
        ChatUtil.sendKey(player, "warp-del", Map.of("warp", args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("warp") && args.length == 1) {
            out.addAll(locations.getWarps().keySet());
        }
        return out;
    }
}
