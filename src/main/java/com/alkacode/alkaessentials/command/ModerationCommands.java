package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.gui.StaffGui;
import com.alkacode.alkaessentials.hook.VanishHandler;
import com.alkacode.alkaessentials.manager.MaintenanceManager;
import com.alkacode.alkaessentials.manager.ModerationManager;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comandos de moderacao: /vanish, /socialspy, /commandspy, /maintenance e /staff (GUI). */
public final class ModerationCommands extends BaseCommand {

    private final ModerationManager moderation;
    private final MaintenanceManager maintenance;
    private final PunishmentManager punishments;
    private final VanishHandler vanishHandler;

    public ModerationCommands(JavaPlugin plugin, ModerationManager moderation,
                              MaintenanceManager maintenance, PunishmentManager punishments,
                              VanishHandler vanishHandler) {
        super(plugin);
        this.moderation = moderation;
        this.maintenance = maintenance;
        this.punishments = punishments;
        this.vanishHandler = vanishHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "vanish": return vanish(player);
            case "socialspy": return socialspy(player);
            case "commandspy": return commandspy(player);
            case "maintenance": return maintenance(player, args);
            case "staff": return staff(player, args);
            default: return false;
        }
    }

    private boolean vanish(Player player) {
        if (!requirePerm(player, "alkassentials.staff.vanish")) return true;
        boolean on = moderation.toggleVanished(player.getUniqueId());
        if (vanishHandler != null) {
            if (on) {
                vanishHandler.apply(player);
            } else {
                vanishHandler.clear(player);
            }
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (on) {
                online.hidePlayer(plugin, player);
            } else {
                online.showPlayer(plugin, player);
            }
        }
        ChatUtil.sendKey(player, on ? "vanish-on" : "vanish-off");
        return true;
    }

    private boolean socialspy(Player player) {
        if (!requirePerm(player, "alkassentials.staff.socialspy")) return true;
        boolean on = moderation.toggleSocialSpy(player.getUniqueId());
        ChatUtil.sendKey(player, on ? "socialspy-on" : "socialspy-off");
        return true;
    }

    private boolean commandspy(Player player) {
        if (!requirePerm(player, "alkassentials.staff.commandspy")) return true;
        boolean on = moderation.toggleCommandSpy(player.getUniqueId());
        ChatUtil.sendKey(player, on ? "commandspy-on" : "commandspy-off");
        return true;
    }

    private boolean maintenance(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.maintenance")) return true;
        boolean on;
        String reason = "";
        if (args.length == 0) {
            on = !maintenance.isEnabled();
        } else if (args[0].equalsIgnoreCase("on")) {
            on = true;
            if (args.length > 1) reason = join(args, 1);
        } else if (args[0].equalsIgnoreCase("off")) {
            on = false;
        } else {
            on = true;
            reason = join(args, 0);
        }
        maintenance.setEnabled(on, reason);
        ChatUtil.sendKey(player, on ? "maintenance-on" : "maintenance-off");
        if (on) {
            String bypass = plugin.getConfig().getString("maintenance.bypass-permission", "alkassentials.maintenance.bypass");
            net.kyori.adventure.text.Component kick = ChatUtil.parse(com.alkacode.alkaessentials.config.MessagesConfig.getInstance()
                    .get("maintenance-kick", Map.of("reason", reason)));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission(bypass)) {
                    online.kick(kick);
                }
            }
        }
        return true;
    }

    private boolean staff(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.staff.menu")) return true;
        if (args.length < 1) {
            return false;
        }
        Player target = matchPlayer(args[0]);
        if (target == null) {
            ChatUtil.sendKey(player, "punish-not-found", Map.of("player", args[0]));
            return true;
        }
        new StaffGui(plugin, player, target, moderation, punishments).open();
        return true;
    }

    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("staff") && args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (command.getName().equalsIgnoreCase("maintenance") && args.length == 1) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
