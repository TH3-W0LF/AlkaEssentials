package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.afk.AfkZone;
import com.alkacode.alkaessentials.afk.AfkZoneManager;
import com.alkacode.alkaessentials.afk.ZoneRegion;
import com.alkacode.alkaessentials.afk.ZoneSelection;
import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Comando /afkzone - cria/redefine/deleta/teleporta zonas AFK e da a varinha de selecao. */
public final class AfkZoneCommands extends BaseCommand {

    private final AfkZoneManager manager;
    private final NamespacedKey wandKey;

    public AfkZoneCommands(JavaPlugin plugin, AfkZoneManager manager) {
        super(plugin);
        this.manager = manager;
        this.wandKey = new NamespacedKey(plugin, "afk_wand");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            help(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "varinha":
            case "wand":
                return wand(player);
            case "criar":
            case "create":
                return create(player, args);
            case "deletar":
            case "delete":
            case "remover":
                return delete(player, args);
            case "redefinir":
            case "redefine":
                return redefine(player, args);
            case "tp":
            case "teleport":
                return tp(player, args);
            case "reload":
                return reload(player);
            default:
                help(player);
                return true;
        }
    }

    private void help(Player player) {
        for (String line : MessagesConfig.getInstance().getYaml().getStringList("afkzone-help")) {
            ChatUtil.send(player, line);
        }
    }

    private boolean wand(Player player) {
        if (!requirePerm(player, "alkassentials.afkzone.admin")) {
            return true;
        }
        YamlConfiguration yaml = MessagesConfig.getInstance().getYaml();
        Material material = Material.matchMaterial(yaml.getString("afkzone-wand.material", "GOLDEN_AXE"));
        if (material == null) {
            material = Material.GOLDEN_AXE;
        }
        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();
        meta.displayName(ChatUtil.parse(yaml.getString("afkzone-wand.name", "<gold>Varinha de Zona AFK")));
        List<Component> lore = new ArrayList<>();
        for (String line : yaml.getStringList("afkzone-wand.lore")) {
            lore.add(ChatUtil.parse(line));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, "true");
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        ChatUtil.sendKey(player, "afkzone-wand-given");
        return true;
    }

    private boolean create(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.afkzone.admin")) {
            return true;
        }
        if (args.length < 2 || args[1].contains(" ") || args[1].contains(";")) {
            ChatUtil.sendKey(player, "afkzone-invalid-name");
            return true;
        }
        ZoneSelection selection = manager.getSelection(player.getUniqueId());
        ZoneRegion region = selection.buildRegion();
        if (region == null) {
            ChatUtil.sendKey(player, "afkzone-no-selection");
            return true;
        }
        if (!manager.createZone(args[1], region)) {
            ChatUtil.sendKey(player, "afkzone-not-found", Map.of("zone", args[1]));
            return true;
        }
        manager.removeSelection(player.getUniqueId());
        ChatUtil.sendKey(player, "afkzone-created", Map.of("zone", args[1]));
        return true;
    }

    private boolean delete(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.afkzone.admin")) {
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        if (!manager.deleteZone(args[1])) {
            ChatUtil.sendKey(player, "afkzone-not-found", Map.of("zone", args[1]));
            return true;
        }
        ChatUtil.sendKey(player, "afkzone-deleted", Map.of("zone", args[1]));
        return true;
    }

    private boolean redefine(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.afkzone.admin")) {
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        ZoneSelection selection = manager.getSelection(player.getUniqueId());
        ZoneRegion region = selection.buildRegion();
        if (region == null) {
            ChatUtil.sendKey(player, "afkzone-no-selection");
            return true;
        }
        if (manager.getZone(args[1]) == null) {
            ChatUtil.sendKey(player, "afkzone-not-found", Map.of("zone", args[1]));
            return true;
        }
        manager.redefineZone(args[1], region);
        ChatUtil.sendKey(player, "afkzone-redefined", Map.of("zone", args[1]));
        return true;
    }

    private boolean tp(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.afkzone.tp")) {
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        AfkZone zone = manager.getZone(args[1]);
        if (zone == null) {
            ChatUtil.sendKey(player, "afkzone-not-found", Map.of("zone", args[1]));
            return true;
        }
        player.teleport(zone.getRegion().getCenter());
        ChatUtil.sendKey(player, "afkzone-teleported", Map.of("zone", zone.getName()));
        return true;
    }

    private boolean reload(Player player) {
        if (!requirePerm(player, "alkassentials.afkzone.admin")) {
            return true;
        }
        manager.reload();
        ChatUtil.sendKey(player, "afkzone-reloaded", Map.of("count", String.valueOf(manager.getZones().size())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("varinha", "criar", "deletar", "redefinir", "tp", "reload");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("delete")
                || args[0].equalsIgnoreCase("deletar") || args[0].equalsIgnoreCase("redefinir"))) {
            return List.copyOf(manager.getZones().keySet());
        }
        return List.of();
    }
}
