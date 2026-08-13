package com.alkacode.alkaessentials.manager;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ultimo local salvo de cada jogador pro /back (em memoria, so da sessao). */
public final class BackManager {

    private final Map<UUID, Location> back = new HashMap<>();

    public void save(UUID uuid, Location loc) {
        back.put(uuid, loc);
    }

    /** Retorna e remove o ultimo local salvo. */
    public Location pop(UUID uuid) {
        return back.remove(uuid);
    }

    public Location peek(UUID uuid) {
        return back.get(uuid);
    }
}
