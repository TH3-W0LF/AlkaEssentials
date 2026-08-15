package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** ShowItem: exibe itens no chat com hover (passar o mouse mostra o item) e clique copia. */
public final class ShowItemCommand extends BaseCommand {

    public ShowItemCommand(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        switch (command.getName().toLowerCase()) {
            case "showitem":
                return showItem(player, args, true);
            case "showslot":
                return showSlot(player, args);
            case "showinv":
                return showInv(player, args, false);
            case "showender":
                return showInv(player, args, true);
            default:
                return false;
        }
    }

    private boolean showItem(Player player, String[] args, boolean allowRadius) {
        if (!requirePerm(player, "alkassentials.showitem.item")) return true;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            ChatUtil.sendKey(player, "showitem-no-item");
            return true;
        }
        Player target = args.length > 0 ? matchPlayer(args[0]) : null;
        if (args.length > 0 && target == null) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        if (target == null) {
            target = player;
        }
        Component line = buildShowLine(player, item);
        target.sendMessage(line);
        if (target != player) {
            ChatUtil.sendKey(player, "showitem-sent", Map.of("player", target.getName()));
        }
        return true;
    }

    private boolean showSlot(Player player, String[] args) {
        if (!requirePerm(player, "alkassentials.showitem.slot")) return true;
        if (args.length < 1) {
            ChatUtil.sendKey(player, "showitem-invalid-slot");
            return true;
        }
        int slot;
        try {
            slot = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            ChatUtil.sendKey(player, "showitem-invalid-slot");
            return true;
        }
        Player target = args.length > 1 ? matchPlayer(args[1]) : player;
        if (args.length > 1 && target == null) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[1]));
            return true;
        }
        ItemStack item = getSlotItem(target, slot);
        if (item == null || item.getType().isAir()) {
            ChatUtil.sendKey(player, "showitem-no-item");
            return true;
        }
        Component line = MiniMessage.miniMessage().deserialize(
                        MessagesConfig.getInstance().get("showitem-slot-prefix",
                                Map.of("player", player.getName())))
                .append(itemComponent(item));
        target.sendMessage(line);
        if (target != player) {
            ChatUtil.sendKey(player, "showitem-sent", Map.of("player", target.getName()));
        }
        return true;
    }

    private boolean showInv(Player player, String[] args, boolean ender) {
        if (!requirePerm(player, ender ? "alkassentials.showitem.ender" : "alkassentials.showitem.inv")) return true;
        Player target = args.length > 0 ? matchPlayer(args[0]) : player;
        if (args.length > 0 && target == null) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        List<ItemStack> items = ender ? getEnder(target) : getInventory(target);
        String title = MessagesConfig.getInstance().get(
                ender ? "showitem-ender-title" : "showitem-inv-title",
                Map.of("player", target.getName()));
        Component header = MiniMessage.miniMessage().deserialize(title);

        Component content = Component.empty();
        for (ItemStack item : items) {
            Component comp = itemComponent(item);
            if (comp != Component.empty()) {
                content = content.append(comp).append(Component.text(" "));
            }
        }
        target.sendMessage(header);
        target.sendMessage(content);
        if (target != player) {
            ChatUtil.sendKey(player, "showitem-sent", Map.of("player", target.getName()));
        }
        return true;
    }

    // ---------- helpers ----------

    private Component buildShowLine(Player player, ItemStack item) {
        String format = MessagesConfig.getInstance().get("showitem-msg", Map.of("player", player.getName()));
        String before = format.substring(0, format.indexOf("{item}"));
        String after = format.substring(format.indexOf("{item}") + "{item}".length());
        return MiniMessage.miniMessage().deserialize(before)
                .append(itemComponent(item))
                .append(MiniMessage.miniMessage().deserialize(after));
    }

    /** Componente do item: nome + hover com o TOOLTIP REAL do cliente (via
     * ItemStack#asHoverEvent - mesma renderizacao nativa que passar o mouse no
     * inventario mostra: encantamentos, atributos, durabilidade, tudo - nao so
     * nome+lore reconstruidos manualmente) + clique copia. */
    private Component itemComponent(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Component.empty();
        }
        Component name = item.displayName();
        return name
                .hoverEvent(item.asHoverEvent())
                .clickEvent(ClickEvent.copyToClipboard(item.toString()));
    }

    private ItemStack getSlotItem(Player player, int slot) {
        if (slot >= 0 && slot < 36) return player.getInventory().getItem(slot);
        if (slot >= 36 && slot < 40) {
            ItemStack[] armor = player.getInventory().getArmorContents();
            return armor[slot - 36];
        }
        if (slot == 40) return player.getInventory().getItemInOffHand();
        return null;
    }

    private List<ItemStack> getInventory(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            items.add(item);
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            items.add(armor);
        }
        items.add(player.getInventory().getItemInOffHand());
        return items;
    }

    private List<ItemStack> getEnder(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getEnderChest().getContents()) {
            items.add(item);
        }
        return items;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && !command.getName().equalsIgnoreCase("showslot")) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (command.getName().equalsIgnoreCase("showslot") && args.length == 2) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
