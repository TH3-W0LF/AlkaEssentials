package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Placas de comando: placa com "[cmd] <comando>" na primeira linha executa ao clicar. */
public final class CommandSignListener implements Listener {

    private final JavaPlugin plugin;

    public CommandSignListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }
        String prefix = plugin.getConfig().getString("signs.command-signs.prefix", "[cmd]");
        String firstLine = sign.getLine(0).trim();
        if (!firstLine.startsWith(prefix)) {
            return;
        }
        Player player = event.getPlayer();
        String permission = plugin.getConfig().getString("signs.command-signs.permission",
                "alkassentials.signs.command");
        if (!player.hasPermission(permission)) {
            ChatUtil.sendKey(player, "signs-no-permission");
            return;
        }
        event.setCancelled(true);
        String command = firstLine.substring(prefix.length()).trim();
        if (command.isEmpty()) {
            return;
        }
        command = command.replace("{player}", player.getName());
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
    }
}
