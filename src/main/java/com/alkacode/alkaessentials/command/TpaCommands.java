package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.manager.IgnoreManager;
import com.alkacode.alkaessentials.manager.TpaManager;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Comandos de pedido de teleporte: /tpa, /tpahere, /tpaccept, /tpdeny, /tptoggle. */
public final class TpaCommands extends BaseCommand {

    private final TpaManager tpa;
    private final TeleportService teleports;
    private final IgnoreManager ignores;

    public TpaCommands(JavaPlugin plugin, TpaManager tpa, TeleportService teleports, IgnoreManager ignores) {
        super(plugin);
        this.tpa = tpa;
        this.teleports = teleports;
        this.ignores = ignores;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "tpa":
                return request(player, args, false);
            case "tpahere":
                return request(player, args, true);
            case "tpaccept":
                return accept(player);
            case "tpdeny":
                return deny(player);
            case "tptoggle":
                return toggle(player);
            default:
                return false;
        }
    }

    private boolean request(Player sender, String[] args, boolean here) {
        if (args.length < 1) {
            return false;
        }
        Player target = matchPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            ChatUtil.sendKey(sender, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            ChatUtil.sendKey(sender, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        if (ignores.isIgnoring(target.getUniqueId(), sender.getUniqueId())) {
            ChatUtil.sendKey(sender, "tpa-toggle-blocked");
            return true;
        }
        String toggleBypass = plugin.getConfig().getString("tpa.toggle-bypass", "alkassentials.tpa.togglebypass");
        if (tpa.isBlocked(target.getUniqueId()) && !target.hasPermission(toggleBypass)) {
            ChatUtil.sendKey(sender, "tpa-toggle-blocked");
            return true;
        }
        long timeout = plugin.getConfig().getLong("tpa.request-timeout", 60) * 1000L;
        tpa.request(sender.getUniqueId(), target.getUniqueId(), here, timeout);

        if (here) {
            ChatUtil.sendKey(sender, "tpahere-sent", Map.of("player", target.getName()));
            ChatUtil.sendKey(target, "tpahere-received", Map.of("player", sender.getName()));
        } else {
            ChatUtil.sendKey(sender, "tpa-sent", Map.of("player", target.getName()));
            ChatUtil.sendKey(target, "tpa-received", Map.of("player", sender.getName()));
        }
        return true;
    }

    private boolean accept(Player player) {
        TpaManager.Request req = tpa.getPending(player.getUniqueId());
        if (req == null) {
            ChatUtil.sendKey(player, "tpa-none");
            return true;
        }
        tpa.remove(player.getUniqueId());
        Player source = plugin.getServer().getPlayer(req.getSource());
        Player target = plugin.getServer().getPlayer(req.getTarget());
        if (target == null || !target.isOnline()) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", "?"));
            return true;
        }
        if (req.isHere()) {
            // o alvo aceita e vai ate quem pediu
            if (source == null || !source.isOnline()) {
                ChatUtil.sendKey(player, "invalid-player", Map.of("player", "?"));
                return true;
            }
            teleports.teleport(target, source.getLocation(), "tpa", true, null, null);
        } else {
            // quem pediu vai ate o alvo que aceitou
            if (source == null || !source.isOnline()) {
                ChatUtil.sendKey(player, "invalid-player", Map.of("player", "?"));
                return true;
            }
            teleports.teleport(source, target.getLocation(), "tpa", true, null, null);
        }
        ChatUtil.sendKey(player, "tpa-accepted");
        return true;
    }

    private boolean deny(Player player) {
        TpaManager.Request req = tpa.getPending(player.getUniqueId());
        if (req == null) {
            ChatUtil.sendKey(player, "tpa-none");
            return true;
        }
        tpa.remove(player.getUniqueId());
        ChatUtil.sendKey(player, "tpa-denied");
        Player source = plugin.getServer().getPlayer(req.getSource());
        if (source != null && source.isOnline()) {
            ChatUtil.sendKey(source, "tpa-denied");
        }
        return true;
    }

    private boolean toggle(Player player) {
        boolean nowBlocked = !tpa.isBlocked(player.getUniqueId());
        tpa.setBlocked(player.getUniqueId(), nowBlocked);
        ChatUtil.sendKey(player, nowBlocked ? "tpa-toggle-off" : "tpa-toggle-on");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ((command.getName().equalsIgnoreCase("tpa") || command.getName().equalsIgnoreCase("tpahere")) && args.length == 1) {
            List<String> out = new ArrayList<>();
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                out.add(online.getName());
            }
            return out;
        }
        return List.of();
    }
}
