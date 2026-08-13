package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.manager.BackManager;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Comando /back - retorna ao ultimo local salvo (antes de teleporte/morte). */
public final class BackCommand extends BaseCommand {

    private final BackManager back;
    private final TeleportService teleports;

    public BackCommand(JavaPlugin plugin, BackManager back, TeleportService teleports) {
        super(plugin);
        this.back = back;
        this.teleports = teleports;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        Location last = back.pop(player.getUniqueId());
        if (last == null || last.getWorld() == null) {
            ChatUtil.sendKey(player, "back-none");
            return true;
        }
        // nao salva o local atual como novo /back (evita loop)
        teleports.teleport(player, last, "back", false, "back-teleported", null);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
