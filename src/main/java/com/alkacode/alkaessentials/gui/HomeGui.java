package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.manager.HomeManager;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/** Menu de gerenciamento das homes - clique teleporta, shift+clique apaga. */
public final class HomeGui extends BaseGui {

    private final HomeManager homes;
    private final TeleportService teleports;

    public HomeGui(JavaPlugin plugin, Player player, HomeManager homes, TeleportService teleports) {
        super(plugin, player, MenuConfig.getInstance().title("homes", null), 4, "alkaessentials_homes");
        this.homes = homes;
        this.teleports = teleports;
    }

    @Override
    public void render() {
        ItemStack glass = MenuConfig.getInstance().item("homes.glass", null);
        fillBorder(glass);

        Map<String, Location> playerHomes = homes.getHomes(player.getUniqueId());
        if (playerHomes.isEmpty()) {
            setItem(13, MenuConfig.getInstance().item("homes.empty", null));
            return;
        }

        int slot = 10;
        for (Map.Entry<String, Location> entry : playerHomes.entrySet()) {
            String name = entry.getKey();
            Location loc = entry.getValue();
            ItemStack item = MenuConfig.getInstance().item("homes.home", Map.of("home", name));
            setItem(slot, item, event -> {
                if (event.isShiftClick()) {
                    homes.removeHome(player.getUniqueId(), name);
                    ChatUtil.sendKey(player, "home-del", Map.of("home", name));
                    refresh();
                } else if (loc != null) {
                    teleports.teleport(player, loc, "home", true, "home-teleported", Map.of("home", name));
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
