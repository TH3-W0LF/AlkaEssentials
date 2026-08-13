package com.alkacode.alkaessentials.afk;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Recompensa de uma zona AFK: chance, permissao opcional, tempo minimo/maximo, display,
 * comandos (rodados no console) e itens (add-ou-drop no inventario). */
public final class AfkReward {

    private static final Random RANDOM = new Random();

    private final double chance;
    private final String permission;
    private final int minimumTime;
    private final int maximumTime;
    private final String display;
    private final List<String> commands;
    private final List<ItemStack> items;

    public AfkReward(ConfigurationSection section) {
        this.chance = section.getDouble("chance", 10.0);
        this.permission = section.getString("permission", "");
        this.minimumTime = section.getInt("minimum-time", 0);
        this.maximumTime = section.getInt("maximum-time", Integer.MAX_VALUE);
        this.display = section.getString("display");
        this.commands = section.getStringList("commands");

        this.items = new ArrayList<>();
        for (Map<?, ?> entry : section.getMapList("items")) {
            Object materialValue = entry.get("material");
            String materialName = materialValue == null ? "STONE" : String.valueOf(materialValue);
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                material = Material.STONE;
            }
            int amount = entry.get("amount") instanceof Number n ? n.intValue() : 1;
            items.add(new ItemStack(material, Math.max(1, amount)));
        }
    }

    public double getChance() {
        return chance;
    }

    public String getPermission() {
        return permission;
    }

    public int getMinimumTime() {
        return minimumTime;
    }

    public int getMaximumTime() {
        return maximumTime;
    }

    public String getDisplay() {
        return display;
    }

    public boolean hasPermission(Player player) {
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    /** Roda a recompensa: comandos no console (console sender) e itens no inventario. */
    public void run(Player player) {
        for (String cmd : commands) {
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }
        for (ItemStack item : items) {
            ItemStack copy = item.clone();
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(copy);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    public static boolean roll(double chance) {
        return RANDOM.nextDouble() * 100.0 < chance;
    }
}
