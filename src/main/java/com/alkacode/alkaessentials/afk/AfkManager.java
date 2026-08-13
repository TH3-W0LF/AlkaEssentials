package com.alkacode.alkaessentials.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estado AFK do jogador (marca a tag [AFK] no TAB). Uma conta e AFK se estiver em
 * QUALQUER um desses estados: /afk manual, dentro de uma zona AFK, ou inativo por
 * tempo suficiente (auto-afk). A tag e aplicada/removida de forma idempotente no
 * playerListName, restaurando o nome original quando sai do AFK.
 */
public final class AfkManager {

    private final JavaPlugin plugin;
    private final Set<UUID> manualAfk = new HashSet<>();
    private final Set<UUID> zoneAfk = new HashSet<>();
    private final Set<UUID> afk = new HashSet<>();
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Component> originalNames = new HashMap<>();
    private boolean externalTabName = false;

    public AfkManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Quando o modulo de scoreboard controla o playerListName (player-name.enabled),
     * o AFK nao mexe mais no nome do TAB - so mantem o estado. */
    public void setExternalTabName(boolean external) {
        this.externalTabName = external;
    }

    public boolean isExternalTabName() {
        return externalTabName;
    }

    public boolean isAfk(UUID uuid) {
        return afk.contains(uuid);
    }

    public void setManualAfk(Player player, boolean value) {
        if (value) {
            manualAfk.add(player.getUniqueId());
        } else {
            manualAfk.remove(player.getUniqueId());
        }
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        recompute(player);
    }

    /** Chamado pela zona AFK quando o jogador entra/sai da regiao. */
    public void setInZone(UUID uuid, boolean inZone) {
        if (inZone) {
            zoneAfk.add(uuid);
        } else {
            zoneAfk.remove(uuid);
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            recompute(player);
        }
    }

    /** Qualquer interacao (mover, falar, quebrar, etc.) zera a inatividade do auto-afk. */
    public void updateActivity(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        lastActivity.put(uuid, now);
        if (afk.contains(uuid) && !manualAfk.contains(uuid) && !zoneAfk.contains(uuid)) {
            recompute(player);
        }
    }

    /** Roda periodicamente (1s) pra aplicar o auto-afk por inatividade. */
    public void tickAuto() {
        long timeout = plugin.getConfig().getLong("afk.auto-timeout", 180);
        if (timeout <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("alkassentials.afk.auto")) {
                continue;
            }
            UUID uuid = player.getUniqueId();
            if (afk.contains(uuid)) {
                continue;
            }
            long last = lastActivity.getOrDefault(uuid, now);
            if (now - last >= timeout * 1000L) {
                lastActivity.put(uuid, now);
                recompute(player);
            }
        }
    }

    private void recompute(Player player) {
        UUID uuid = player.getUniqueId();
        long timeout = plugin.getConfig().getLong("afk.auto-timeout", 180);
        boolean auto = timeout > 0
                && (System.currentTimeMillis() - lastActivity.getOrDefault(uuid, System.currentTimeMillis()))
                >= timeout * 1000L;
        boolean newAfk = manualAfk.contains(uuid) || zoneAfk.contains(uuid) || auto;

        if (newAfk == afk.contains(uuid)) {
            return;
        }
        if (newAfk) {
            afk.add(uuid);
            applyTag(player, true);
        } else {
            afk.remove(uuid);
            applyTag(player, false);
        }
    }

    private void applyTag(Player player, boolean afk) {
        if (externalTabName) {
            return;
        }
        if (!plugin.getConfig().getBoolean("afk.tab-tag", true)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (afk) {
            if (originalNames.containsKey(uuid)) {
                return;
            }
            originalNames.put(uuid, player.playerListName());
            String format = plugin.getConfig().getString("afk.tab-tag-format", "<gold>[AFK]<reset> ");
            player.playerListName(MiniMessage.miniMessage().deserialize(format).append(player.playerListName()));
        } else {
            Component original = originalNames.remove(uuid);
            if (original != null) {
                player.playerListName(original);
            }
        }
    }

    /** Limpa o estado de um jogador que saiu (sem tocar no playerListName, ja desconectado). */
    public void handleQuit(UUID uuid) {
        manualAfk.remove(uuid);
        zoneAfk.remove(uuid);
        afk.remove(uuid);
        lastActivity.remove(uuid);
        originalNames.remove(uuid);
    }

    public void clearAll() {
        for (UUID uuid : afk) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Component original = originalNames.remove(uuid);
                if (original != null) {
                    player.playerListName(original);
                }
            }
        }
        manualAfk.clear();
        zoneAfk.clear();
        afk.clear();
        lastActivity.clear();
        originalNames.clear();
    }
}
