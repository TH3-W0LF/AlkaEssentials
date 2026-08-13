package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Elevadores simples: parar em cima do bloco e pular sobe um andar; agachar desce.
 * Blocos configuraveis (config.yml elevators.blocks). Debounce por jogador pra nao
 * teleportar repetidamente no mesmo movimento.
 */
public final class ElevatorListener implements Listener {

    private final JavaPlugin plugin;
    private final Set<Material> blocks = new HashSet<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public ElevatorListener(JavaPlugin plugin) {
        this.plugin = plugin;
        for (String name : plugin.getConfig().getStringList("elevators.blocks")) {
            try {
                blocks.add(Material.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Bloco de elevador invalido no config: " + name);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("elevators.enabled", true)) {
            return;
        }
        if (event.getTo() == null || event.getFrom() == null || event.getTo().getWorld() == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isFlying()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(uuid, 0L);
        if (now - last < 500) {
            return;
        }

        Material under = event.getTo().getBlock().getRelative(BlockFace.DOWN).getType();
        if (!blocks.contains(under)) {
            return;
        }

        boolean movingUp = event.getTo().getY() > event.getFrom().getY();
        boolean movingDown = event.getTo().getY() < event.getFrom().getY();
        int maxDistance = plugin.getConfig().getInt("elevators.max-distance", 32);

        if (movingUp) {
            Location target = findLevel(player, event.getTo(), 1, maxDistance);
            if (target != null) {
                lastUse.put(uuid, now);
                player.teleport(target);
                ChatUtil.sendKey(player, "elevator-up");
            } else {
                ChatUtil.sendKey(player, "elevator-not-found");
            }
        } else if (movingDown && player.isSneaking()) {
            Location target = findLevel(player, event.getTo(), -1, maxDistance);
            if (target != null) {
                lastUse.put(uuid, now);
                player.teleport(target);
                ChatUtil.sendKey(player, "elevator-down");
            } else {
                ChatUtil.sendKey(player, "elevator-not-found");
            }
        }
    }

    private Location findLevel(Player player, Location current, int direction, int maxDistance) {
        World world = current.getWorld();
        int x = current.getBlockX();
        int z = current.getBlockZ();
        int startY = current.getBlockY();
        for (int i = 2; i <= maxDistance; i++) {
            int y = startY + direction * i;
            if (y < world.getMinHeight() || y > world.getMaxHeight() - 1) {
                break;
            }
            if (blocks.contains(world.getBlockAt(x, y, z).getType())) {
                return new Location(world, current.getX(), y + 1, current.getZ(),
                        player.getLocation().getYaw(), player.getLocation().getPitch());
            }
        }
        return null;
    }
}
