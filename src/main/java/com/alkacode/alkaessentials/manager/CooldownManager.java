package com.alkacode.alkaessentials.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cooldowns por jogador+acao (em memoria). Chave composta uuid|acao -> timestamp de expiracao. */
public final class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();

    private static String key(UUID uuid, String action) {
        return uuid + "|" + action;
    }

    public void set(UUID uuid, String action, long seconds) {
        if (seconds <= 0) {
            cooldowns.remove(key(uuid, action));
            return;
        }
        cooldowns.put(key(uuid, action), System.currentTimeMillis() + seconds * 1000L);
    }

    /** Segundos restantes (0 = liberado). */
    public long remainingSeconds(UUID uuid, String action) {
        Long until = cooldowns.get(key(uuid, action));
        if (until == null) {
            return 0;
        }
        long remaining = (until - System.currentTimeMillis()) / 1000L;
        if (remaining <= 0) {
            cooldowns.remove(key(uuid, action));
            return 0;
        }
        return remaining;
    }

    public void clear(UUID uuid) {
        cooldowns.entrySet().removeIf(e -> e.getKey().startsWith(uuid + "|"));
    }
}
