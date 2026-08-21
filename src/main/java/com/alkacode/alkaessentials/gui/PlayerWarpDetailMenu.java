package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.gui.layout.GuiLayoutLoader;
import com.alkacode.alkaessentials.hook.AlkaEconomyHook;
import com.alkacode.alkaessentials.manager.PlayerWarpManager;
import com.alkacode.alkaessentials.model.PlayerWarp;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Detalhe de um warp: teleportar (cobrando preco se houver), favoritar, avaliar
 * (1-5 estrelas). Voltar retorna pra lista que abriu essa tela. */
public final class PlayerWarpDetailMenu extends BaseGui {

    private final PlayerWarpManager warps;
    private final PlayerWarp warp;
    private final BaseGui previous;
    private final TeleportService teleports;
    private final AlkaEconomyHook economy;

    public PlayerWarpDetailMenu(JavaPlugin plugin, Player viewer, PlayerWarpManager warps, PlayerWarp warp,
                                 BaseGui previous, TeleportService teleports, AlkaEconomyHook economy) {
        super(plugin, viewer, MenuConfig.getInstance().title("pwarp-detail-title", Map.of("warp", warp.name())),
                4, "alkaessentials_pwarp_detail");
        this.warps = warps;
        this.warp = warp;
        this.previous = previous;
        this.teleports = teleports;
        this.economy = economy;
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("pwarps.glass", null));

        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_pwarp_detail");

        setItem(layout.firstSlot('T'), MenuConfig.getInstance().item("pwarps.detail.teleport",
                        Map.of("price", warp.price() > 0
                                ? warp.price() + " " + warp.currency() : "Gratis")),
                event -> teleport());

        boolean favorite = warps.isFavorite(player.getUniqueId(), warp.id());
        ItemStack favoriteItem = MenuConfig.getInstance().item("pwarps.detail.favorite", null);
        if (favorite) {
            favoriteItem = glow(favoriteItem);
        }
        setItem(layout.firstSlot('F'), favoriteItem, event -> {
            warps.toggleFavorite(player.getUniqueId(), warp.id());
            ChatUtil.sendKey(player, warps.isFavorite(player.getUniqueId(), warp.id())
                    ? "pwarp-favorited" : "pwarp-unfavorited", Map.of("warp", warp.name()));
            refresh();
        });

        Integer ownRating = warps.ownRating(warp.id(), player.getUniqueId());
        List<Integer> starSlots = layout.findSlots('0');
        for (int stars = 1; stars <= starSlots.size(); stars++) {
            ItemStack star = MenuConfig.getInstance().item("pwarps.detail.star",
                    Map.of("n", String.valueOf(stars)));
            if (ownRating != null && ownRating >= stars) {
                star = glow(star);
            }
            int finalStars = stars;
            setItem(starSlots.get(stars - 1), star, event -> {
                warps.rate(warp.id(), player.getUniqueId(), finalStars);
                ChatUtil.sendKey(player, "pwarp-rated", Map.of("warp", warp.name(), "stars", String.valueOf(finalStars)));
                refresh();
            });
        }

        setItem(layout.firstSlot('V'), MenuConfig.getInstance().item("pwarps.back", null),
                event -> {
                    if (previous != null) {
                        previous.open();
                    } else {
                        player.closeInventory();
                    }
                });

        if (warp.owner().equals(player.getUniqueId()) || player.hasPermission("alkaessentials.warps.admin")) {
            setItem(layout.firstSlot('D'), MenuConfig.getInstance().item("pwarps.delete", null), event -> {
                warps.delete(warp.id());
                ChatUtil.sendKey(player, "pwarp-deleted", Map.of("warp", warp.name()));
                if (previous != null) {
                    previous.open();
                } else {
                    player.closeInventory();
                }
            });
        }
    }

    private void teleport() {
        Location location = warp.toLocation();
        if (location == null) {
            ChatUtil.sendKey(player, "pwarp-not-found", Map.of("warp", warp.name()));
            return;
        }
        if (warp.price() > 0 && !warp.owner().equals(player.getUniqueId())) {
            if (economy == null || !economy.has(player.getUniqueId(), warp.currency(), warp.price())) {
                ChatUtil.sendKey(player, "pwarp-price-insufficient",
                        Map.of("price", warp.price() + " " + warp.currency()));
                return;
            }
            economy.withdraw(player.getUniqueId(), warp.currency(), warp.price());
            economy.deposit(warp.owner(), warp.currency(), warp.price());
            ChatUtil.sendKey(player, "pwarp-price-charged",
                    Map.of("price", economy.format(warp.price()) + " " + warp.currency()));
        }
        player.closeInventory();
        teleports.teleport(player, location, "playerwarp", true, "warp-teleported", Map.of("warp", warp.name()));
    }
}
