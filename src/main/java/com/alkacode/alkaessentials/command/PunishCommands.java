package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.gui.PunishGui;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.model.Punishment;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.alkaessentials.util.DurationParser;
import com.alkacode.core.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Comandos de punicao: /warn, /kick, /ban, /tempban, /mute, /tempmute, /unban, /unmute, /punishinfo. */
public final class PunishCommands extends BaseCommand {

    private final PunishmentManager manager;

    public PunishCommands(JavaPlugin plugin, PunishmentManager manager) {
        super(plugin);
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "warn":
                return warn(sender, args);
            case "kick":
                return kick(sender, args);
            case "ban":
                return ban(sender, args, false);
            case "tempban":
                return ban(sender, args, true);
            case "mute":
                return mute(sender, args, false);
            case "tempmute":
                return mute(sender, args, true);
            case "unban":
                return revoke(sender, args, "BAN", "punish-unban");
            case "unmute":
                return revoke(sender, args, "MUTE", "punish-unmute");
            case "punishinfo":
                return punishInfo(sender, args);
            case "punish":
                return punish(sender, args);
            default:
                return false;
        }
    }

    private boolean punish(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        if (!requirePerm(player, "alkassentials.punish")) return true;
        if (args.length < 1) return false;
        Player target = matchPlayer(args[0]);
        if (target == null) {
            ChatUtil.sendKey(player, "punish-not-found", Map.of("player", args[0]));
            return true;
        }
        new PunishGui(plugin, player, target, manager).open();
        return true;
    }

    private boolean warn(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        boolean ok = applyPunishment(sender, args[0], "WARN", "Aviso", "avisado", null, null);
        if (ok) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            Punishment auto = manager.autoPunishOnMaxWarns(target.getUniqueId(), args[0]);
            if (auto != null) {
                manager.broadcast(auto, auto.getType(), "punido", "Sistema", auto.getReason());
            }
        }
        return true;
    }

    private boolean kick(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getUniqueId() == null) {
            ChatUtil.sendKey(sender, "punish-not-found", Map.of("player", args[0]));
            return true;
        }
        if (!checkSelf(sender, target.getUniqueId())) return true;
        applyAndBroadcast(sender, args[0], target.getUniqueId(), "KICK", "Expulsao",
                "expulso", joinReason(args, 1), null, null);
        return true;
    }

    private boolean ban(CommandSender sender, String[] args, boolean temp) {
        if (args.length < 2) return false;
        String duration = null;
        int reasonStart = 1;
        if (temp) {
            duration = args[1];
            if (DurationParser.parse(duration) < 0) {
                ChatUtil.sendKey(sender, "punish-invalid-duration", Map.of("duration", duration));
                return true;
            }
            reasonStart = 2;
            if (args.length < 3) return false;
        }
        return applyPunishment(sender, args[0], temp ? "TEMPBAN" : "BAN", "Banimento",
                "banido", duration, joinReason(args, reasonStart));
    }

    private boolean mute(CommandSender sender, String[] args, boolean temp) {
        if (args.length < 2) return false;
        String duration = null;
        int reasonStart = 1;
        if (temp) {
            duration = args[1];
            if (DurationParser.parse(duration) < 0) {
                ChatUtil.sendKey(sender, "punish-invalid-duration", Map.of("duration", duration));
                return true;
            }
            reasonStart = 2;
            if (args.length < 3) return false;
        }
        return applyPunishment(sender, args[0], temp ? "TEMPMUTE" : "MUTE", "Silenciamento",
                "silenciado", duration, joinReason(args, reasonStart));
    }

    private boolean applyPunishment(CommandSender sender, String targetName, String type, String typeName,
                                    String action, String duration, String reason) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getUniqueId() == null) {
            ChatUtil.sendKey(sender, "punish-not-found", Map.of("player", targetName));
            return true;
        }
        if (!checkSelf(sender, target.getUniqueId())) return true;
        applyAndBroadcast(sender, targetName, target.getUniqueId(), type, typeName, action,
                reason == null ? plugin.getConfig().getString("punish.no-reason", "Nenhum motivo") : reason,
                null, duration);
        return true;
    }

    private void applyAndBroadcast(CommandSender sender, String targetName, UUID target, String type,
                                   String typeName, String action, String reason, String proof, String duration) {
        // silent: motivo terminando em "-s" nao anuncia
        boolean silent = reason != null && reason.trim().endsWith("-s");
        if (silent) {
            reason = reason.trim().replaceFirst("\\s*-s$", "");
        }
        String issuer = sender instanceof Player p ? p.getName() : manager.consoleName();
        Punishment p = manager.apply(target, targetName, type, reason, proof, issuer, duration);

        manager.applyImmediate(p);
        ChatUtil.sendKey(sender, "punish-applied", Map.of("player", targetName));
        if (!silent) {
            manager.broadcast(p, typeName, action, issuer, reason);
        }
    }

    private boolean revoke(CommandSender sender, String[] args, String typeLike, String msgKey) {
        if (args.length < 1) return false;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        int count = manager.revoke(target.getUniqueId(), typeLike);
        ChatUtil.sendKey(sender, msgKey, Map.of("player", args[0]));
        if (count == 0) {
            ChatUtil.sendKey(sender, "punish-no-active", Map.of("player", args[0]));
        }
        return true;
    }

    private boolean punishInfo(CommandSender sender, String[] args) {
        if (args.length < 1) return false;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        List<Punishment> history = manager.getRepository().getHistory(target.getUniqueId(), 10);
        if (history.isEmpty()) {
            ChatUtil.sendKey(sender, "punish-no-active", Map.of("player", args[0]));
            return true;
        }
        ChatUtil.sendKey(sender, "punish-info-header", Map.of("player", args[0]));
        for (Punishment p : history) {
            ChatUtil.sendKey(sender, "punish-info-line", Map.of(
                    "id", String.valueOf(p.getId()), "type", p.getType(),
                    "reason", p.getReason() == null ? "" : p.getReason(),
                    "issuer", p.getIssuer() == null ? "?" : p.getIssuer()));
        }
        return true;
    }

    private boolean checkSelf(CommandSender sender, UUID target) {
        if (sender instanceof Player p && p.getUniqueId().equals(target)
                && !plugin.getConfig().getBoolean("punish.self-punish", false)) {
            ChatUtil.sendKey(sender, "punish-self");
            return false;
        }
        return true;
    }

    private String joinReason(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private String message(String key, Map<String, String> placeholders) {
        return MessagesConfig.getInstance().get(key, placeholders);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}

