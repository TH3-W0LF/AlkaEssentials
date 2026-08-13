package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.alkaessentials.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Comando /rtp - teleporte aleatorio seguro (nao cai em lava/agua/ar). */
public final class RtpCommand extends BaseCommand {

    private final TeleportService teleports;

    public RtpCommand(JavaPlugin plugin, TeleportService teleports) {
        super(plugin);
        this.teleports = teleports;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        World world = player.getWorld();
        String configured = plugin.getConfig().getString("rtp.world", "");
        if (!configured.isEmpty()) {
            World target = plugin.getServer().getWorld(configured);
            if (target == null) {
                ChatUtil.sendKey(player, "rtp-wrong-world");
                return true;
            }
            world = target;
        }
        int radius = plugin.getConfig().getInt("rtp.radius", 3000);
        int attempts = plugin.getConfig().getInt("rtp.attempts", 10);

        Location safe = LocationUtil.findSafeLocation(player, world, radius, attempts);
        if (safe == null) {
            ChatUtil.sendKey(player, "rtp-failed");
            return true;
        }
        teleports.teleport(player, safe, "rtp", true, "rtp-teleported", null);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
