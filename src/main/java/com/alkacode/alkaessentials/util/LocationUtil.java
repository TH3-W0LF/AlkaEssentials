package com.alkacode.alkaessentials.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Serializacao/deserializacao de {@link Location} em string (pro YAML de locais) e
 * algoritmo de ponto seguro pro RTP. Formato: "world;x;y;z;yaw;pitch".
 */
public final class LocationUtil {

    private static final Material[] UNSAFE = {
            Material.LAVA, Material.WATER, Material.MAGMA_BLOCK, Material.CACTUS, Material.FIRE
    };

    private LocationUtil() {
    }

    public static String serialize(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";"
                + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location deserialize(String data, World fallbackWorld) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        String[] parts = data.split(";");
        if (parts.length < 4) {
            return null;
        }
        World world = org.bukkit.Bukkit.getWorld(parts[0]);
        if (world == null) {
            world = fallbackWorld;
        }
        if (world == null) {
            return null;
        }
        double x, y, z;
        float yaw = 0f, pitch = 0f;
        try {
            x = Double.parseDouble(parts[1]);
            y = Double.parseDouble(parts[2]);
            z = Double.parseDouble(parts[3]);
            if (parts.length > 4) {
                yaw = Float.parseFloat(parts[4]);
            }
            if (parts.length > 5) {
                pitch = Float.parseFloat(parts[5]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Procura um ponto aleatorio e seguro num raio a partir do centro (0,0). Seguro =
     * o bloco alvo nao esta em ar/lava/agua e ha chao solido no bloco abaixo. Usa
     * chunks gerados (o mundo pode ser grande; perguntar chunk fora do loaded e caro,
     * por isso so rola dentro do raio em chunks ja gerados/forcados).
     */
    public static Location findSafeLocation(Player player, World world, int radius, int attempts) {
        for (int i = 0; i < attempts; i++) {
            int x = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight() - 1;

            for (int y = maxY; y > minY; y--) {
                Block block = world.getBlockAt(x, y, z);
                Block below = block.getRelative(BlockFace.DOWN);
                if (isSafeSurface(block.getType(), below.getType())) {
                    Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5, player.getLocation().getYaw(), 0f);
                    // desce ate o chao para o jogador nao spawnar flutuando
                    loc.setY(y + 1);
                    return loc;
                }
            }
        }
        return null;
    }

    private static boolean isSafeSurface(Material foot, Material support) {
        if (!support.isSolid() || isUnsafe(support)) {
            return false;
        }
        // o bloco do pe nao pode ser solido que prenda o jogador
        if (foot.isSolid() && !foot.name().endsWith("_PLATE") && !foot.name().contains("CARPET")) {
            return false;
        }
        return !isUnsafe(foot);
    }

    private static boolean isUnsafe(Material material) {
        for (Material unsafe : UNSAFE) {
            if (material == unsafe) {
                return true;
            }
        }
        return false;
    }
}
