package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.manager.ModerationManager;
import com.alkacode.alkaessentials.manager.TempCommandManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.alkaessentials.util.DurationParser;
import com.alkacode.core.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comandos de staff: /clear, /heal, /feed, /fly, /god, /freeze, /invsee, /ptime, /pweather. */
public final class StaffCommands extends BaseCommand {

    private final ModerationManager moderation;
    private final TempCommandManager temp;

    public StaffCommands(JavaPlugin plugin, ModerationManager moderation, TempCommandManager temp) {
        super(plugin);
        this.moderation = moderation;
        this.temp = temp;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "clear": return clear(sender, args);
            case "heal": return heal(sender, args);
            case "feed": return feed(sender, args);
            case "fly": return fly(sender, args);
            case "god": return god(sender, args);
            case "freeze": return freeze(sender, args);
            case "invsee": return invsee(sender, args);
            case "ptime": return ptime(sender, args);
            case "pweather": return pweather(sender, args);
            default: return false;
        }
    }

    private boolean clear(CommandSender sender, String[] args) {
        Player target = targetOrSelf(sender, args, "alkassentials.staff.clear");
        if (target == null) return true;
        target.getInventory().clear();
        ChatUtil.sendKey(sender, "clear-inv", Map.of("player", target.getName()));
        return true;
    }

    private boolean heal(CommandSender sender, String[] args) {
        Player target = targetOrSelf(sender, args, "alkassentials.staff.heal");
        if (target == null) return true;
        double max = 20.0;
        var attr = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr != null) {
            max = attr.getValue();
        }
        target.setHealth(max);
        ChatUtil.sendKey(sender, "heal-done", Map.of("player", target.getName()));
        return true;
    }

    private boolean feed(CommandSender sender, String[] args) {
        Player target = targetOrSelf(sender, args, "alkassentials.staff.feed");
        if (target == null) return true;
        target.setFoodLevel(20);
        target.setSaturation(20f);
        ChatUtil.sendKey(sender, "feed-done", Map.of("player", target.getName()));
        return true;
    }

    private boolean fly(CommandSender sender, String[] args) {
        TargetInfo info = resolveTarget(sender, args, "alkassentials.staff.fly");
        if (info == null) return true;
        Player target = info.player;
        if (!info.hasDuration) {
            boolean on = !target.getAllowFlight();
            if (on) temp.grantFlight(target, 0, null);
            else temp.removeFlight(target);
            ChatUtil.sendKey(sender, on ? "fly-on" : "fly-off", Map.of("player", target.getName()));
        } else {
            temp.grantFlight(target, info.seconds, () -> temp.removeFlight(target));
            ChatUtil.sendKey(sender, "fly-temp", Map.of("player", target.getName(),
                    "time", TimeUtil.formatSeconds(info.seconds)));
        }
        return true;
    }

    private boolean god(CommandSender sender, String[] args) {
        TargetInfo info = resolveTarget(sender, args, "alkassentials.staff.god");
        if (info == null) return true;
        Player target = info.player;
        boolean on = !moderation.isGod(target.getUniqueId());
        if (on) temp.grantGod(target, info.seconds, () -> temp.removeGod(target));
        else temp.removeGod(target);
        ChatUtil.sendKey(sender, on ? (info.hasDuration ? "god-temp" : "god-on") : "god-off",
                Map.of("player", target.getName(),
                        "time", TimeUtil.formatSeconds(info.seconds)));
        return true;
    }

    private boolean freeze(CommandSender sender, String[] args) {
        Player target = targetOrSelf(sender, args, "alkassentials.staff.freeze");
        if (target == null) return true;
        boolean frozen = moderation.toggleFrozen(target.getUniqueId());
        ChatUtil.sendKey(sender, frozen ? "freeze-on" : "freeze-off", Map.of("player", target.getName()));
        if (frozen) {
            ChatUtil.sendKey(target, "freeze-you");
        }
        return true;
    }

    private boolean invsee(CommandSender sender, String[] args) {
        Player target = targetOrSelf(sender, args, "alkassentials.staff.invsee");
        if (target == null) return true;
        Player staff = asPlayer(sender);
        if (staff == null) return true;
        staff.openInventory(target.getInventory());
        ChatUtil.sendKey(sender, "invsee-open", Map.of("player", target.getName()));
        return true;
    }

    private boolean ptime(CommandSender sender, String[] args) {
        if (!requirePerm(sender, "alkassentials.staff.ptime")) return true;
        Player player = asPlayer(sender);
        if (player == null) return true;
        Player target = player;
        String value;
        if (args.length >= 2) {
            target = matchPlayer(args[0]);
            if (target == null) { ChatUtil.sendKey(sender, "punish-not-found", Map.of("player", args[0])); return true; }
            value = args[1];
        } else {
            value = args[0];
        }
        switch (value.toLowerCase()) {
            case "day" -> target.setPlayerTime(6000L, false);
            case "night" -> target.setPlayerTime(18000L, false);
            case "reset" -> target.resetPlayerTime();
            default -> { return false; }
        }
        ChatUtil.sendKey(sender, "ptime-set", Map.of("time", value, "player", target.getName()));
        return true;
    }

    private boolean pweather(CommandSender sender, String[] args) {
        if (!requirePerm(sender, "alkassentials.staff.pweather")) return true;
        Player player = asPlayer(sender);
        if (player == null) return true;
        Player target = player;
        String value;
        if (args.length >= 2) {
            target = matchPlayer(args[0]);
            if (target == null) { ChatUtil.sendKey(sender, "punish-not-found", Map.of("player", args[0])); return true; }
            value = args[1];
        } else {
            value = args[0];
        }
        switch (value.toLowerCase()) {
            case "sun" -> target.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
            case "rain" -> target.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
            case "reset" -> target.resetPlayerWeather();
            default -> { return false; }
        }
        ChatUtil.sendKey(sender, "pweather-set", Map.of("weather", value, "player", target.getName()));
        return true;
    }

    // ---------- helpers ----------

    private Player targetOrSelf(CommandSender sender, String[] args, String permission) {
        if (!requirePerm(sender, permission)) return null;
        Player self = asPlayer(sender);
        if (self == null) return null;
        if (args.length >= 1) {
            Player target = matchPlayer(args[0]);
            if (target == null) { ChatUtil.sendKey(sender, "punish-not-found", Map.of("player", args[0])); return null; }
            return target;
        }
        return self;
    }

    /** Resultado da resolucao de [jogador] [tempo]. */
    private static final class TargetInfo {
        final Player player;
        final long seconds;
        final boolean hasDuration;
        TargetInfo(Player player, long seconds, boolean hasDuration) {
            this.player = player;
            this.seconds = seconds;
            this.hasDuration = hasDuration;
        }
    }

    /** Resolve [jogador] [tempo]; sem args usa o proprio comando (self). */
    private TargetInfo resolveTarget(CommandSender sender, String[] args, String permission) {
        if (!requirePerm(sender, permission)) return null;
        Player self = asPlayer(sender);
        if (self == null) return null;
        Player target = self;
        long seconds = 0;
        boolean hasDuration = false;
        if (args.length >= 1) {
            Player matched = matchPlayer(args[0]);
            if (matched != null) {
                target = matched;
                if (args.length >= 2) {
                    seconds = parseDuration(sender, args[1]);
                    if (seconds < 0) return null;
                    hasDuration = true;
                }
            } else {
                seconds = parseDuration(sender, args[0]);
                if (seconds < 0) return null;
                hasDuration = true;
            }
        }
        return new TargetInfo(target, seconds, hasDuration);
    }

    private long parseDuration(CommandSender sender, String raw) {
        long parsed = DurationParser.parse(raw);
        if (parsed < 0) {
            ChatUtil.sendKey(sender, "punish-invalid-duration", Map.of("duration", raw));
        }
        return parsed;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
