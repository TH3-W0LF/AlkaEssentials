package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tumulo de morte (deathchest): no lugar dos itens cairem no chao, surge um baú com
 * um holograma (armor stand invisivel) mostrando o dono e a contagem regressiva. Depois
 * do tempo limite o baú some e os itens sao perdidos. O bloco nao pode ser quebrado
 * (DeathChestListener), mas qualquer um pode abrir.
 */
public final class DeathChestManager {

    private static final class DeathChest {
        final Block block;
        final ArmorStand stand;
        final String ownerName;
        final long expireAt;

        DeathChest(Block block, ArmorStand stand, String ownerName, long expireAt) {
            this.block = block;
            this.stand = stand;
            this.ownerName = ownerName;
            this.expireAt = expireAt;
        }
    }

    private final JavaPlugin plugin;
    private final Map<Block, DeathChest> chests = new HashMap<>();

    public DeathChestManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Cria o tumulo com os itens que cairiam. Retorna false se nao achou lugar seguro. */
    public boolean spawn(Player player, List<ItemStack> drops) {
        World world = player.getWorld();
        Block block = findSafeBlock(world, player.getLocation());
        if (block == null) {
            return false;
        }
        block.setType(Material.CHEST, false);
        Chest chest = (Chest) block.getState();
        for (ItemStack drop : drops) {
            ItemStack leftover = chest.getBlockInventory().addItem(drop).get(0);
            if (leftover != null) {
                world.dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), leftover);
            }
        }

        long lifetime = plugin.getConfig().getLong("deathchest.lifetime-seconds", 600) * 1000L;
        ArmorStand stand = spawnStand(block.getLocation());
        updateStandName(stand, player.getName(), lifetime / 1000);

        chests.put(block, new DeathChest(block, stand, player.getName(),
                System.currentTimeMillis() + lifetime));
        return true;
    }

    private ArmorStand spawnStand(Location blockLoc) {
        Location loc = blockLoc.clone().add(0.5, 1.2, 0.5);
        return blockLoc.getWorld().spawn(loc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setSmall(true);
            s.setMarker(true);
            s.setInvulnerable(true);
            s.setCanPickupItems(false);
            s.setCustomNameVisible(true);
        });
    }

    private void updateStandName(ArmorStand stand, String ownerName, long secondsLeft) {
        MessagesConfig cfg = MessagesConfig.getInstance();
        String title = cfg.get("death-chest-title", Map.of("player", ownerName));
        String time = cfg.get("death-chest-time-left", Map.of("time", TimeUtil.formatSeconds(secondsLeft)));
        stand.customName(ChatUtil.parse(title + " " + time));
    }

    /** Roda a cada segundo: remove tumulos expirados. */
    public void tick() {
        long now = System.currentTimeMillis();
        List<Block> expired = new ArrayList<>();
        for (Map.Entry<Block, DeathChest> entry : chests.entrySet()) {
            if (now >= entry.getValue().expireAt) {
                expired.add(entry.getKey());
            } else {
                updateStandName(entry.getValue().stand, entry.getValue().ownerName,
                        (entry.getValue().expireAt - now) / 1000);
            }
        }
        for (Block block : expired) {
            DeathChest dc = chests.remove(block);
            if (dc != null) {
                dc.stand.remove();
                if (block.getType() == Material.CHEST) {
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    private Block findSafeBlock(World world, Location deathLoc) {
        for (int i = 0; i < 4; i++) {
            Location candidate = deathLoc.clone().add(0, i, 0);
            Block block = candidate.getBlock();
            Block below = candidate.clone().subtract(0, 1, 0).getBlock();
            if (block.getType() == Material.AIR && below.getType().isSolid()) {
                return block;
            }
        }
        return null;
    }

    public boolean isDeathChest(Block block) {
        return chests.containsKey(block);
    }

    public void clearAll() {
        for (DeathChest dc : chests.values()) {
            dc.stand.remove();
        }
        chests.clear();
    }
}
