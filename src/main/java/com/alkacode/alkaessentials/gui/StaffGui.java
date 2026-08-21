package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.gui.layout.GuiLayoutLoader;
import com.alkacode.alkaessentials.manager.ModerationManager;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
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
        super(plugin, staff, MenuConfig.getInstance().title("staff.title", Map.of("player", target.getName())),
                3, "alkaessentials_staff");
        this.target = target;
        this.moderation = moderation;
        this.punishments = punishments;
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("staff.glass", null));

        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_staff");
        Map<String, String> ph = Map.of("player", target.getName());

        setItem(layout.firstSlot('C'), MenuConfig.getInstance().item("staff.clear", ph), event -> {
            target.getInventory().clear();
            ChatUtil.sendKey(player, "staff-action-cleared", Map.of("player", target.getName()));
        });

        setItem(layout.firstSlot('P'), MenuConfig.getInstance().item("staff.ping", ph), event ->
                ChatUtil.sendKey(player, "staff-action-ping",
                        Map.of("player", target.getName(), "ping", String.valueOf(target.getPing()))));

        setItem(layout.firstSlot('F'), MenuConfig.getInstance().item("staff.freeze", ph), event -> {
            boolean frozen = moderation.toggleFrozen(target.getUniqueId());
            ChatUtil.sendKey(player, frozen ? "freeze-on" : "freeze-off", Map.of("player", target.getName()));
            if (frozen) {
                ChatUtil.sendKey(target, "freeze-you");
            }
        });

        setItem(layout.firstSlot('T'), MenuConfig.getInstance().item("staff.teleport", ph),
                event -> player.teleport(target.getLocation()));

        setItem(layout.firstSlot('M'), MenuConfig.getInstance().item("staff.mute", ph), event -> {
            punishments.apply(target.getUniqueId(), target.getName(), "TEMPMUTE", "Punido pela staff",
                    "", player.getName(), "30m");
            ChatUtil.sendKey(player, "punish-applied", Map.of("player", target.getName()));
            ChatUtil.sendKey(target, "punish-mute-notify",
                    Map.of("reason", "Punido pela staff", "time", "30m"));
        });
    }
}
