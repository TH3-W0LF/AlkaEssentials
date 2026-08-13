package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.model.Punishment;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.util.TimeUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/** Aplica os efeitos das punicoes: ban no join, mute no chat e bloqueio de comandos. */
public final class PunishmentListener implements Listener {

    private final JavaPlugin plugin;
    private final PunishmentManager manager;

    public PunishmentListener(JavaPlugin plugin, PunishmentManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (manager.hasActiveBan(player.getUniqueId())) {
            Punishment ban = manager.getRepository().getActive(player.getUniqueId(), "BAN").get(0);
            String time = ban.isPermanent() ? "Permanente"
                    : TimeUtil.formatSeconds((ban.getEndTime() - System.currentTimeMillis()) / 1000);
            String reason = ban.getReason() == null ? "" : ban.getReason();
            String msg = com.alkacode.alkaessentials.config.MessagesConfig.getInstance()
                    .get("punish-ban-kick", Map.of("reason", reason, "time", time));
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> player.kick(ChatUtil.parse(msg)), 1L);
        } else if (manager.hasActiveMute(player.getUniqueId())) {
            Punishment mute = manager.getActiveMute(player.getUniqueId());
            String time = mute.isPermanent() ? "Permanente"
                    : TimeUtil.formatSeconds((mute.getEndTime() - System.currentTimeMillis()) / 1000);
            ChatUtil.sendKey(player, "punish-mute-notify", Map.of(
                    "reason", mute.getReason() == null ? "" : mute.getReason(), "time", time));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (manager.hasActiveMute(player.getUniqueId())) {
            event.setCancelled(true);
            ChatUtil.sendKey(player, "punish-muted-chat");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!manager.hasActiveMute(player.getUniqueId())) {
            return;
        }
        String lower = event.getMessage().toLowerCase();
        for (String blocked : plugin.getConfig().getStringList("punish.block-commands")) {
            if (lower.startsWith(blocked.toLowerCase())) {
                event.setCancelled(true);
                ChatUtil.sendKey(player, "punish-muted-chat");
                return;
            }
        }
    }
}
