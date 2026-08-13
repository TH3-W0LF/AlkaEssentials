package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.MaintenanceManager;
import com.alkacode.alkaessentials.manager.ModerationManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Moderacao: freeze (movimento/comandos), commandspy, socialspy, god, vanish e manutencao. */
public final class ModerationListener implements Listener {

    private static final Set<String> SOCIALSPY_COMMANDS = Set.of(
            "/msg ", "/tell ", "/w ", "/whisper ", "/r ", "/reply ", "/t ");

    private final JavaPlugin plugin;
    private final ModerationManager moderation;
    private final MaintenanceManager maintenance;
    private final com.alkacode.alkaessentials.hook.VanishHandler vanishHandler;

    public ModerationListener(JavaPlugin plugin, ModerationManager moderation, MaintenanceManager maintenance,
                              com.alkacode.alkaessentials.hook.VanishHandler vanishHandler) {
        this.plugin = plugin;
        this.moderation = moderation;
        this.maintenance = maintenance;
        this.vanishHandler = vanishHandler;
    }

    // ---------- freeze ----------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFreezeMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (moderation.isFrozen(player.getUniqueId())
                && (event.getFrom().distanceSquared(event.getTo()) > 0.0001)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFreezeCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!moderation.isFrozen(player.getUniqueId())) {
            return;
        }
        if (!plugin.getConfig().getBoolean("moderation.freeze-block-commands", true)) {
            return;
        }
        // staff/op consegue sair sozinho: mantem comandos disponiveis p/ se descongelar
        if (player.hasPermission("alkassentials.staff.freeze")) {
            return;
        }
        event.setCancelled(true);
        ChatUtil.sendKey(player, "freeze-you");
    }

    // ---------- god ----------

    @EventHandler
    public void onGodDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && moderation.isGod(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ---------- spy ----------

    @EventHandler
    public void onCommandSpy(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        broadcastSpy("Cmd", player, message);
        for (String cmd : SOCIALSPY_COMMANDS) {
            if (message.toLowerCase().startsWith(cmd)) {
                broadcastSpy("Social", player, message);
                return;
            }
        }
    }

    private void broadcastSpy(String type, Player player, String message) {
        boolean social = type.equals("Social");
        for (Player online : Bukkit.getOnlinePlayers()) {
            boolean enabled = social
                    ? moderation.isSocialSpy(online.getUniqueId())
                    : moderation.isCommandSpy(online.getUniqueId());
            if (enabled) {
                ChatUtil.sendKey(online, "spy-log", Map.of(
                        "type", type, "player", player.getName(), "message", message));
            }
        }
    }

    // ---------- manutencao ----------

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!maintenance.isEnabled()) {
            return;
        }
        String bypass = plugin.getConfig().getString("maintenance.bypass-permission",
                "alkassentials.maintenance.bypass");
        if (event.getPlayer().hasPermission(bypass)) {
            return;
        }
        net.kyori.adventure.text.Component kick = ChatUtil.parse(
                com.alkacode.alkaessentials.config.MessagesConfig.getInstance()
                        .get("maintenance-kick", Map.of("reason", maintenance.getReason())));
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kick);
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (!maintenance.isEnabled()) {
            return;
        }
        net.kyori.adventure.text.Component motd = ChatUtil.parse(
                com.alkacode.alkaessentials.config.MessagesConfig.getInstance()
                        .get("maintenance-motd", Map.of("reason", maintenance.getReason())));
        event.motd(motd);
    }

    // ---------- vanish / quit ----------

    @EventHandler
    public void onJoinVanish(PlayerJoinEvent event) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (moderation.isVanished(online.getUniqueId()) && !online.equals(event.getPlayer())) {
                event.getPlayer().hidePlayer(plugin, online);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        moderation.handleQuit(uuid);
    }
}
