package com.alkacode.alkaessentials.afk;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Regiao cuboide de uma zona AFK (AABB entre dois cantos). Verifica se um jogador
 * esta dentro e serializa/deserializa os cantos no formato do AlkaEssentials
 * (world;x;y;z;yaw;pitch).
 */
public final class ZoneRegion {

    private final Location corner1;
    private final Location corner2;

    public ZoneRegion(Location corner1, Location corner2) {
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        if (!loc.getWorld().equals(corner1.getWorld())) {
            return false;
        }
        int x1 = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int y1 = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int z1 = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int x2 = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int y2 = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int z2 = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        return loc.getBlockX() >= x1 && loc.getBlockX() <= x2
                && loc.getBlockY() >= y1 && loc.getBlockY() <= y2
                && loc.getBlockZ() >= z1 && loc.getBlockZ() <= z2;
    }

    /** Todos os jogadores online, vivos, no mundo da zona e com a permissao da zona. */
    public Set<Player> getPlayersInZone(String permission) {
        Set<Player> players = new HashSet<>();
        if (corner1.getWorld() == null) {
            return players;
        }
        boolean requirePermission = permission != null && !permission.isBlank();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || !player.getWorld().equals(corner1.getWorld())) {
                continue;
            }
            if (requirePermission && !player.hasPermission(permission)) {
                continue;
            }
            if (contains(player.getLocation())) {
                players.add(player);
            }
        }
        return players;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public World getWorld() {
        return corner1.getWorld();
    }

    public Location getCenter() {
        World world = corner1.getWorld();
        return new Location(world,
                (corner1.getBlockX() + corner2.getBlockX()) / 2.0,
                (corner1.getBlockY() + corner2.getBlockY()) / 2.0,
                (corner1.getBlockZ() + corner2.getBlockZ()) / 2.0);
    }
}
