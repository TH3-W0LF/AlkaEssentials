package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.manager.ModerationManager;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
import com.alkacode.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/** Painel de staff pro /staff <jogador>: acoes rapidas (limpar inv, ping, freeze, mute, teleport). */
public final class StaffGui extends BaseGui {

    private final Player target;
    private final ModerationManager moderation;
    private final PunishmentManager punishments;

    public StaffGui(JavaPlugin plugin, Player staff, Player target,
                    ModerationManager moderation, PunishmentManager punishments) {
        super(plugin, staff, MenuConfig.getInstance().title("staff", Map.of("player", target.getName())),
                3, "alkaessentials_staff");
        this.target = target;
        this.moderation = moderation;
        this.punishments = punishments;
    }

    @Override
    public void render() {
        ItemBuilder glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ");
        fillBorder(glass.build());

        // slot 11: limpar inventario
        setItem(11, new ItemBuilder(Material.BARREL)
                .name("<gold>Limpar Inventario")
                .lore("<gray>Remove todos os itens de " + target.getName())
                .build(), event -> {
            target.getInventory().clear();
            ChatUtil.sendKey(player, "staff-action-cleared", Map.of("player", target.getName()));
        });

        // slot 12: ver ping
        setItem(12, new ItemBuilder(Material.CLOCK)
                .name("<gold>Ver Ping")
                .lore("<gray>Latencia de " + target.getName())
                .build(), event ->
                ChatUtil.sendKey(player, "staff-action-ping",
                        Map.of("player", target.getName(), "ping", String.valueOf(target.getPing()))));

        // slot 13: congelar
        setItem(13, new ItemBuilder(Material.ICE)
                .name("<aqua>Congelar")
                .lore("<gray>Congela/descongela " + target.getName())
                .build(), event -> {
            boolean frozen = moderation.toggleFrozen(target.getUniqueId());
            ChatUtil.sendKey(player, frozen ? "freeze-on" : "freeze-off", Map.of("player", target.getName()));
            if (frozen) {
                ChatUtil.sendKey(target, "freeze-you");
            }
        });

        // slot 14: teleportar ate ele
        setItem(14, new ItemBuilder(Material.ENDER_PEARL)
                .name("<gold>Teleportar")
                .lore("<gray>Vai ate " + target.getName())
                .build(), event -> player.teleport(target.getLocation()));

        // slot 15: mute 30m
        setItem(15, new ItemBuilder(Material.REDSTONE_TORCH)
                .name("<red>Mutar (30m)")
                .lore("<gray>Silencia " + target.getName() + " por 30 minutos")
                .build(), event -> {
            punishments.apply(target.getUniqueId(), target.getName(), "TEMPMUTE", "Punido pela staff",
                    "", player.getName(), "30m");
            ChatUtil.sendKey(player, "punish-applied", Map.of("player", target.getName()));
            ChatUtil.sendKey(target, "punish-mute-notify",
                    Map.of("reason", "Punido pela staff", "time", "30m"));
        });
    }
}
