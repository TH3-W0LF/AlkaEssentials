package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.gui.HomeGui;
import com.alkacode.alkaessentials.manager.HomeLimit;
import com.alkacode.alkaessentials.manager.HomeManager;
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

/** Comandos de home: /home, /sethome, /delhome, /homes. */
public final class HomeCommands extends BaseCommand {

    private final HomeManager homes;
    private final HomeLimit homeLimit;
    private final TeleportService teleports;

    public HomeCommands(JavaPlugin plugin, HomeManager homes, HomeLimit homeLimit, TeleportService teleports) {
        super(plugin);
        this.homes = homes;
        this.homeLimit = homeLimit;
        this.teleports = teleports;
    }

    private String defaultName() {
        return plugin.getConfig().getString("homes.default-name", "home");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "home":
                return home(sender, args);
            case "sethome":
                return setHome(sender, args);
            case "delhome":
                return delHome(sender, args);
            case "homes":
                return homes(sender);
            default:
                return false;
        }
    }

    private boolean home(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length > 0 ? args[0] : defaultName();
        Location loc = homes.getHome(player.getUniqueId(), name);
        if (loc == null) {
            ChatUtil.sendKey(player, "home-not-found", Map.of("home", name));
            return true;
        }
        teleports.teleport(player, loc, "home", true, "home-teleported", Map.of("home", name));
        return true;
    }

    private boolean setHome(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length > 0 ? args[0] : defaultName();
        if (name.contains(" ") || name.contains(";")) {
            ChatUtil.sendKey(player, "home-invalid-name");
            return true;
        }
        if (plugin.getConfig().getStringList("homes.blocked-worlds").contains(player.getWorld().getName())) {
            ChatUtil.sendKey(player, "home-blocked-world");
            return true;
        }
        int limit = homeLimit.limitFor(player);
        if (!homes.hasHome(player.getUniqueId(), name) && homes.getHomes(player.getUniqueId()).size() >= limit) {
            ChatUtil.sendKey(player, "home-limit", Map.of("limit", String.valueOf(limit)));
            return true;
        }
        homes.setHome(player.getUniqueId(), name, player.getLocation());
        ChatUtil.sendKey(player, "home-set", Map.of("home", name));
        return true;
    }

    private boolean delHome(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length > 0 ? args[0] : defaultName();
        if (!homes.hasHome(player.getUniqueId(), name)) {
            ChatUtil.sendKey(player, "home-not-found", Map.of("home", name));
            return true;
        }
        homes.removeHome(player.getUniqueId(), name);
        ChatUtil.sendKey(player, "home-del", Map.of("home", name));
        return true;
    }

    private boolean homes(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        new HomeGui(plugin, player, homes, teleports).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        String cmd = command.getName().toLowerCase();
        if ((cmd.equals("home") || cmd.equals("delhome")) && args.length == 1) {
            out.addAll(homes.getHomes(player.getUniqueId()).keySet());
        }
        return out;
    }
}
