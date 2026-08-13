package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.gui.TrashInventory;
import com.alkacode.alkaessentials.manager.NightVisionManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Comandos de qualidade de vida: /craft, /wb, /lixo, /trash, /nv, /luz, /ping. */
public final class QolCommands extends BaseCommand {

    private final NightVisionManager nightVision;

    public QolCommands(JavaPlugin plugin, NightVisionManager nightVision) {
        super(plugin);
        this.nightVision = nightVision;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "craft":
            case "wb":
                return craft(player);
            case "lixo":
            case "trash":
                return trash(player);
            case "nv":
            case "luz":
                return nightVision(player);
            case "ping":
                return ping(player, args);
            default:
                return false;
        }
    }

    private boolean craft(Player player) {
        if (!requirePerm(player, "alkassentials.qol.craft")) {
            return true;
        }
        String title = MessagesConfig.getInstance().getRaw("qol-craft-title");
        player.openInventory(Bukkit.createInventory(null, InventoryType.WORKBENCH,
                MiniMessage.miniMessage().deserialize(title)));
        ChatUtil.sendKey(player, "qol-craft");
        return true;
    }

    private boolean trash(Player player) {
        if (!requirePerm(player, "alkassentials.qol.trash")) {
            return true;
        }
        player.openInventory(new TrashInventory(player).getInventory());
        ChatUtil.sendKey(player, "qol-trash-open");
        return true;
    }

    private boolean nightVision(Player player) {
        if (!requirePerm(player, "alkassentials.qol.nv")) {
            return true;
        }
        boolean nowEnabled = !nightVision.isEnabled(player.getUniqueId());
        if (nowEnabled) {
            nightVision.enable(player);
        } else {
            nightVision.disable(player);
        }
        ChatUtil.sendKey(player, nowEnabled ? "qol-nv-on" : "qol-nv-off");
        return true;
    }

    private boolean ping(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.qol.ping")) {
            return true;
        }
        if (args.length == 0) {
            ChatUtil.sendKey(player, "qol-ping", Map.of("ping", String.valueOf(player.getPing())));
            return true;
        }
        Player target = matchPlayer(args[0]);
        if (target == null) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        ChatUtil.sendKey(player, "qol-ping-other", Map.of(
                "player", target.getName(),
                "ping", String.valueOf(target.getPing())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("ping") && args.length == 1) {
            List<String> out = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                out.add(online.getName());
            }
            return out;
        }
        return List.of();
    }
}
