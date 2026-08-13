package com.alkacode.alkaessentials.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerenciador dos pedidos de teleporte (TPA). Chaveado pelo UUID de quem RECEBE o
 * pedido; suporta um pedido pendente por vez (novo pedido sobrescreve o anterior).
 * Tambem guarda o toggle /tptoggle (quem bloqueia pedidos).
 */
public final class TpaManager {

    public static final class Request {
        private final UUID source;
        private final UUID target;
        private final boolean here;
        private final long expiresAt;

        public Request(UUID source, UUID target, boolean here, long timeoutMillis) {
            this.source = source;
            this.target = target;
            this.here = here;
            this.expiresAt = System.currentTimeMillis() + timeoutMillis;
        }

        public UUID getSource() { return source; }
        public UUID getTarget() { return target; }
        public boolean isHere() { return here; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    private final Map<UUID, Request> requests = new HashMap<>();
    private final Map<UUID, Long> blockedUntil = new HashMap<>();

    public void request(UUID source, UUID target, boolean here, long timeoutMillis) {
        requests.put(target, new Request(source, target, here, timeoutMillis));
    }

    /** Pedido pendente e nao expirado para o alvo, ou null. */
    public Request getPending(UUID target) {
        Request req = requests.get(target);
        if (req == null) {
            return null;
        }
        if (req.isExpired()) {
            requests.remove(target);
            return null;
        }
        return req;
    }

    public void remove(UUID target) {
        requests.remove(target);
    }

    // ------------------------- /tptoggle -------------------------

    public boolean isBlocked(UUID uuid) {
        Long until = blockedUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            blockedUntil.remove(uuid);
            return false;
        }
        return true;
    }

    public void setBlocked(UUID uuid, boolean blocked) {
        if (blocked) {
            blockedUntil.put(uuid, Long.MAX_VALUE);
        } else {
            blockedUntil.remove(uuid);
        }
    }
}
