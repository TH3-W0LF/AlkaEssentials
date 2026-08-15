package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.gui.InvRestoreGui;
import com.alkacode.alkaessentials.manager.InvRestoreManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Comando /invrestore <jogador> - abre GUI com o historico de snapshots pra escolher
 * QUAL restaurar, com preview do conteudo antes de confirmar (ver InvRestoreGui). */
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
        Player admin = asPlayer(sender);
        if (admin == null) {
            return true;
        }
        if (args.length < 1) {
            return false;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            ChatUtil.sendKey(sender, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        new InvRestoreGui(plugin, admin, invRestore, target).open();
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
