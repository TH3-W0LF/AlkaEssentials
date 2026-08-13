package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.manager.LocationStore;
import com.alkacode.alkaessentials.model.Warp;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Menu de warps (BaseGui do AlkaCore) - clicou, teleporta. */
public final class WarpGui extends BaseGui {

    private final LocationStore locations;
    private final TeleportService teleports;

    public WarpGui(JavaPlugin plugin, Player player, LocationStore locations, TeleportService teleports) {
        super(plugin, player, MenuConfig.getInstance().title("warps", null), 4, "alkaessentials_warps");
        this.locations = locations;
        this.teleports = teleports;
    }

    @Override
    public void render() {
        ItemStack glass = MenuConfig.getInstance().item("warps.glass", null);
        fillBorder(glass);

        List<Warp> accessible = locations.getWarps().values().stream()
                .filter(w -> !w.hasPermission() || player.hasPermission(w.getPermission()))
                .toList();

        if (accessible.isEmpty()) {
            setItem(13, MenuConfig.getInstance().item("warps.empty", null));
            return;
        }

        int slot = 10;
        for (Warp warp : accessible) {
            ItemStack item = MenuConfig.getInstance().item("warps.warp", Map.of("warp", warp.getName()));
            setItem(slot, item, event -> {
                if (warp.getLocation() != null) {
                    teleports.teleport(player, warp.getLocation(), "warp", true,
                            "warp-teleported", Map.of("warp", warp.getName()));
                }
            });
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
            if (slot >= 27) {
                break;
            }
        }
    }
}
