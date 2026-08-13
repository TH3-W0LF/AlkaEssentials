package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Controle de lotacao: limite de jogadores + slots reservados para VIP. */
public final class SlotListener implements Listener {

    private final JavaPlugin plugin;

    public SlotListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.getConfig().getBoolean("slots.enabled", true)) {
            return;
        }
        int maxPlayers = plugin.getConfig().getInt("slots.max-players", 0);
        if (maxPlayers <= 0) {
            maxPlayers = plugin.getServer().getMaxPlayers();
        }
        int reserved = plugin.getConfig().getInt("slots.reserved-slots", 0);
        int online = Bukkit.getOnlinePlayers().size();

        if (online >= maxPlayers) {
            kick(event, "slots-full");
            return;
        }
        if (reserved > 0 && online >= (maxPlayers - reserved)) {
            String vipPerm = plugin.getConfig().getString("slots.vip-permission", "alkassentials.slots.vip");
            Player player = event.getPlayer();
            if (player == null || !player.hasPermission(vipPerm)) {
                kick(event, "slots-vip");
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void kick(PlayerLoginEvent event, String messageKey) {
        Component msg = ChatUtil.parse(com.alkacode.alkaessentials.config.MessagesConfig.getInstance().get(messageKey));
        event.disallow(PlayerLoginEvent.Result.KICK_FULL, msg);
    }
}
