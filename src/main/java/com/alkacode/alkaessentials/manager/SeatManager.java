package com.alkacode.alkaessentials.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de /sit: cria um ArmorStand invisivel onde o jogador se senta (vira
 * passageiro). Levanta ao usar /sit de novo, se mover, tomar dano ou sair. Evita
 * sentar em blocos que prendam (agua, lava, ar, folhas) ou no void.
 */
public final class SeatManager {

    private final JavaPlugin plugin;
    private final Map<UUID, ArmorStand> seats = new HashMap<>();
    private final Map<UUID, Long> seatTimes = new HashMap<>();

    public SeatManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSeated(UUID uuid) {
        return seats.containsKey(uuid);
    }

    /** Ignora eventos de movimento logo apos sentar (o proprio teleporte do /sit dispara um move). */
    public boolean ignoreMove(UUID uuid) {
        Long time = seatTimes.get(uuid);
        return time != null && System.currentTimeMillis() - time < 800;
    }

    public void toggle(Player player) {
        if (isSeated(player.getUniqueId())) {
            unseat(player);
        } else {
            seat(player);
        }
    }

    public void seat(Player player) {
        if (isSeated(player.getUniqueId())) {
            return;
        }
        Location loc = player.getLocation();
        if (!isSeatable(loc)) {
            return;
        }
        ArmorStand stand = loc.getWorld().spawn(loc.clone().add(0, -0.25, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setSilent(true);
            s.setInvulnerable(true);
            s.setCanPickupItems(false);
            s.setSmall(true);
            s.setPose(Pose.SITTING);
        });
        player.teleport(stand.getLocation());
        stand.addPassenger(player);
        seats.put(player.getUniqueId(), stand);
        seatTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void unseat(Player player) {
        ArmorStand stand = seats.remove(player.getUniqueId());
        seatTimes.remove(player.getUniqueId());
        if (stand != null) {
            stand.removePassenger(player);
            stand.remove();
        }
    }

    private boolean isSeatable(Location loc) {
        if (loc.getY() < loc.getWorld().getMinHeight() + 1) {
            return false;
        }
        Material block = loc.getBlock().getType();
        if (block == Material.WATER || block == Material.LAVA || block == Material.AIR) {
            return false;
        }
        Material support = loc.clone().subtract(0, 1, 0).getBlock().getType();
        if (support == Material.WATER || support == Material.LAVA) {
            return false;
        }
        return true;
    }

    public void handleQuit(UUID uuid) {
        ArmorStand stand = seats.remove(uuid);
        seatTimes.remove(uuid);
        if (stand != null) {
            stand.remove();
        }
    }
}
