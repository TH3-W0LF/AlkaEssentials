package com.alkacode.alkaessentials.manager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sistema de /ignore: quem cada jogador bloqueia (chat, TPA e mensagens). */
public final class IgnoreManager {

    private final Map<UUID, Set<UUID>> ignored = new HashMap<>();

    public IgnoreManager(JavaPlugin plugin) {
    }

    public boolean isIgnoring(UUID who, UUID ignoredUuid) {
        Set<UUID> set = ignored.get(who);
        return set != null && set.contains(ignoredUuid);
    }

    /** Alterna o ignore. Retorna true se agora esta ignorando. */
    public boolean toggle(UUID who, UUID target) {
        Set<UUID> set = ignored.computeIfAbsent(who, k -> new HashSet<>());
        if (!set.add(target)) {
            set.remove(target);
            return false;
        }
        return true;
    }

    public void handleQuit(UUID uuid) {
        ignored.remove(uuid);
        for (Set<UUID> set : ignored.values()) {
            set.remove(uuid);
        }
    }
}
