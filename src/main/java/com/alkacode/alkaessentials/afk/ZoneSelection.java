package com.alkacode.alkaessentials.afk;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Selecao da varinha de zona AFK: dois cantos + visualizador de particulas nas bordas. */
public final class ZoneSelection {

    private final Player player;
    private Location position1;
    private Location position2;
    private final List<Location> borders = new ArrayList<>();

    public ZoneSelection(Player player) {
        this.player = player;
    }

    public boolean isComplete() {
        return position1 != null && position2 != null && Objects.equals(position1.getWorld(), position2.getWorld());
    }

    public void setPosition1(Location loc) {
        this.position1 = loc.clone();
        updateVisualizer();
    }

    public void setPosition2(Location loc) {
        this.position2 = loc.clone();
        updateVisualizer();
    }

    public ZoneRegion buildRegion() {
        if (!isComplete()) {
            return null;
        }
        return new ZoneRegion(position1, position2);
    }

    private void updateVisualizer() {
        borders.clear();
        if (!isComplete()) {
            return;
        }
        int minX = Math.min(position1.getBlockX(), position2.getBlockX());
        int minY = Math.min(position1.getBlockY(), position2.getBlockY());
        int minZ = Math.min(position1.getBlockZ(), position2.getBlockZ());
        int maxX = Math.max(position1.getBlockX(), position2.getBlockX());
        int maxY = Math.max(position1.getBlockY(), position2.getBlockY());
        int maxZ = Math.max(position1.getBlockZ(), position2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int edges = 0;
                    if (x == minX || x == maxX) edges++;
                    if (y == minY || y == maxY) edges++;
                    if (z == minZ || z == maxZ) edges++;
                    if (edges >= 2) {
                        borders.add(new Location(position1.getWorld(), x + 0.5, y + 0.5, z + 0.5));
                    }
                }
            }
        }
    }

    /** Mostra as particulas das bordas. Chamado periodicamente pelo ticker do manager. */
    public void show() {
        if (borders.isEmpty() || !player.isOnline()) {
            return;
        }
        for (Location loc : borders) {
            player.spawnParticle(Particle.WAX_ON, loc, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public Player getPlayer() {
        return player;
    }
}
