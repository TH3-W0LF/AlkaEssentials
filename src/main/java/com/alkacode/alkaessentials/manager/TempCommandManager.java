package com.alkacode.alkaessentials.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Concede beneficios temporarios (/fly e /god com duracao) e remove sozinhos quando
 * o tempo acaba, via task agendada.
 */
public final class TempCommandManager {

    private final JavaPlugin plugin;
    private final ModerationManager moderation;

    public TempCommandManager(JavaPlugin plugin, ModerationManager moderation) {
        this.plugin = plugin;
        this.moderation = moderation;
    }

    /** Habilita voo por N segundos (0 = permanente). onExpire roda quando o tempo acaba. */
    public void grantFlight(Player player, long seconds, Runnable onExpire) {
        player.setAllowFlight(true);
        player.setFlying(true);
        if (seconds <= 0) {
            return;
        }
        schedule(player, seconds, onExpire);
    }

    /** Remove o voo. */
    public void removeFlight(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    /** Habilita god por N segundos (0 = permanente). onExpire roda quando o tempo acaba. */
    public void grantGod(Player player, long seconds, Runnable onExpire) {
        moderation.setGod(player.getUniqueId(), true);
        if (seconds <= 0) {
            return;
        }
        schedule(player, seconds, onExpire);
    }

    /** Remove o god. */
    public void removeGod(Player player) {
        moderation.setGod(player.getUniqueId(), false);
    }

    private void schedule(Player player, long seconds, Runnable onExpire) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && onExpire != null) {
                onExpire.run();
            }
        }, seconds * 20L);
    }
}
