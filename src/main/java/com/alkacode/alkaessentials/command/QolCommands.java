package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.gui.TrashInventory;
import com.alkacode.alkaessentials.manager.NightVisionManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Comandos de qualidade de vida: /craft, /wb, /lixo, /trash, /nv, /luz, /ping. */
public final class QolCommands extends BaseCommand {

    private final NightVisionManager nightVision;

    public QolCommands(JavaPlugin plugin, NightVisionManager nightVision) {
        super(plugin);
        this.nightVision = nightVision;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "craft":
            case "wb":
                return craft(player);
            case "lixo":
            case "trash":
                return trash(player);
            case "nv":
            case "luz":
                return nightVision(player);
            case "ping":
                return ping(player, args);
            case "condense":
                return condense(player);
            case "smelt":
                return smelt(player);
            case "skull":
                return skull(player, args);
            default:
                return false;
        }
    }

    // ---------- condense ----------

    private static final Map<Material, Material> CONDENSE = new HashMap<>();

    static {
        CONDENSE.put(Material.COAL, Material.COAL_BLOCK);
        CONDENSE.put(Material.IRON_INGOT, Material.IRON_BLOCK);
        CONDENSE.put(Material.GOLD_INGOT, Material.GOLD_BLOCK);
        CONDENSE.put(Material.REDSTONE, Material.REDSTONE_BLOCK);
        CONDENSE.put(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK);
        CONDENSE.put(Material.DIAMOND, Material.DIAMOND_BLOCK);
        CONDENSE.put(Material.EMERALD, Material.EMERALD_BLOCK);
        CONDENSE.put(Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK);
        CONDENSE.put(Material.COPPER_INGOT, Material.COPPER_BLOCK);
        CONDENSE.put(Material.QUARTZ, Material.QUARTZ_BLOCK);
        CONDENSE.put(Material.RAW_IRON, Material.RAW_IRON_BLOCK);
        CONDENSE.put(Material.RAW_GOLD, Material.RAW_GOLD_BLOCK);
        CONDENSE.put(Material.RAW_COPPER, Material.RAW_COPPER_BLOCK);
        CONDENSE.put(Material.WHEAT, Material.HAY_BLOCK);
        CONDENSE.put(Material.SLIME_BALL, Material.SLIME_BLOCK);
    }

    private boolean condense(Player player) {
        if (!requirePerm(player, "alkassentials.qol.condense")) return true;
        int converted = 0;
        for (Map.Entry<Material, Material> entry : CONDENSE.entrySet()) {
            Material from = entry.getKey();
            Material to = entry.getValue();
            int count = countMaterial(player, from);
            int blocks = count / 9;
            int remainder = count % 9;
            if (blocks > 0) {
                removeMaterial(player, from);
                giveOrDrop(player, new ItemStack(from, remainder));
                giveOrDrop(player, new ItemStack(to, blocks));
                converted += blocks;
            }
        }
        if (converted > 0) {
            ChatUtil.send(player, "<green>Compactado <yellow>" + converted + " <green>bloco(s).");
        } else {
            ChatUtil.send(player, "<red>Nada para compactar.");
        }
        return true;
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count;
    }

    private void removeMaterial(Player player, Material material) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == material) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item.getAmount() == 0) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    // ---------- smelt ----------

    private static final Map<Material, Material> SMELT = new HashMap<>();

    static {
        SMELT.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELT.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        SMELT.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELT.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELT.put(Material.COBBLESTONE, Material.STONE);
        SMELT.put(Material.SAND, Material.GLASS);
        SMELT.put(Material.RED_SAND, Material.GLASS);
        SMELT.put(Material.CLAY_BALL, Material.BRICK);
        for (Material m : Material.values()) {
            if (m.name().endsWith("_LOG") || m.name().endsWith("_WOOD")) {
                SMELT.put(m, Material.CHARCOAL);
            }
        }
        SMELT.put(Material.WET_SPONGE, Material.SPONGE);
        SMELT.put(Material.KELP, Material.DRIED_KELP);
    }

    private boolean smelt(Player player) {
        if (!requirePerm(player, "alkassentials.qol.smelt")) return true;
        int smelted = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            Material result = SMELT.get(item.getType());
            if (result != null) {
                player.getInventory().setItem(i, new ItemStack(result, item.getAmount()));
                smelted += item.getAmount();
            }
        }
        if (smelted > 0) {
            ChatUtil.send(player, "<green>Fundido <yellow>" + smelted + " <green>item(ns).");
        } else {
            ChatUtil.send(player, "<red>Nada para fundir.");
        }
        return true;
    }

    // ---------- skull ----------

    private boolean skull(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.qol.skull")) return true;
        if (args.length == 0) {
            ChatUtil.send(player, "<red>Uso: /skull <jogador>");
            return true;
        }
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(args[0]));
        skull.setItemMeta(meta);
        giveOrDrop(player, skull);
        ChatUtil.send(player, "<green>Cabeça de <yellow>" + args[0] + " <green>adicionada.");
        return true;
    }

    private boolean craft(Player player) {
        if (!requirePerm(player, "alkassentials.qol.craft")) {
            return true;
        }
        String title = MessagesConfig.getInstance().getRaw("qol-craft-title");
        player.openInventory(Bukkit.createInventory(null, InventoryType.WORKBENCH,
                MiniMessage.miniMessage().deserialize(title)));
        ChatUtil.sendKey(player, "qol-craft");
        return true;
    }

    private boolean trash(Player player) {
        if (!requirePerm(player, "alkassentials.qol.trash")) {
            return true;
        }
        player.openInventory(new TrashInventory(player).getInventory());
        ChatUtil.sendKey(player, "qol-trash-open");
        return true;
    }

    private boolean nightVision(Player player) {
        if (!requirePerm(player, "alkassentials.qol.nv")) {
            return true;
        }
        boolean nowEnabled = !nightVision.isEnabled(player.getUniqueId());
        if (nowEnabled) {
            nightVision.enable(player);
        } else {
            nightVision.disable(player);
        }
        ChatUtil.sendKey(player, nowEnabled ? "qol-nv-on" : "qol-nv-off");
        return true;
    }

    private boolean ping(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.qol.ping")) {
            return true;
        }
        if (args.length == 0) {
            ChatUtil.sendKey(player, "qol-ping", Map.of("ping", String.valueOf(player.getPing())));
            return true;
        }
        Player target = matchPlayer(args[0]);
        if (target == null) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        ChatUtil.sendKey(player, "qol-ping-other", Map.of(
                "player", target.getName(),
                "ping", String.valueOf(target.getPing())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("ping") && args.length == 1) {
            List<String> out = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                out.add(online.getName());
            }
            return out;
        }
        return List.of();
    }
}
