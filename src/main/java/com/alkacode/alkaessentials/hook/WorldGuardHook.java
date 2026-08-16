package com.alkacode.alkaessentials.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

/**
 * Ponte pro WorldGuard - usada so pelo filtro `regions` do scoreboards.yml (ver
 * ScoreboardManager#pick). Mesmo padrao de resolve() + isAvailable() do
 * AlkaEconomyHook local - WorldGuard continua softdepend em runtime, so as
 * classes com.sk89q.worldguard.* sao carregadas de verdade se o plugin existir.
 */
public final class WorldGuardHook {

    private final boolean available;

    private WorldGuardHook(boolean available) {
        this.available = available;
    }

    public static WorldGuardHook resolve() {
        return new WorldGuardHook(Bukkit.getPluginManager().isPluginEnabled("WorldGuard"));
    }

    public boolean isAvailable() {
        return available;
    }

    /** IDs (minusculos, convencao do WorldGuard) das regioes que contem esse local. Vazio se indisponivel/nenhuma. */
    public Set<String> getRegionIdsAt(Location location) {
        if (!available || location.getWorld() == null) {
            return Set.of();
        }
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        Set<String> ids = new HashSet<>();
        for (ProtectedRegion region : query.getApplicableRegions(BukkitAdapter.adapt(location)).getRegions()) {
            ids.add(region.getId());
        }
        return ids;
    }
}
