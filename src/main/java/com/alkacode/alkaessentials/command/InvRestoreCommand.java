package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.manager.InvRestoreManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comando /invrestore <jogador> - devolve o ultimo inventario salvo na morte. */
public final class InvRestoreCommand extends BaseCommand {

    private final InvRestoreManager invRestore;

    public InvRestoreCommand(JavaPlugin plugin, InvRestoreManager invRestore) {
        super(plugin);
        this.invRestore = invRestore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePerm(sender, "alkassentials.admin.invrestore")) {
            return true;
        }
        if (args.length < 1) {
            return false;
        }
        Player target = matchPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            ChatUtil.sendKey(sender, "invrestore-offline", Map.of("player", args[0]));
            return true;
        }
        if (!invRestore.restore(target)) {
            ChatUtil.sendKey(sender, "invrestore-none", Map.of("player", target.getName()));
            return true;
        }
        ChatUtil.sendKey(sender, "invrestore-success", Map.of("player", target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName).toList();
        }
        return List.of();
    }
}
