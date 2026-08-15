package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.hook.AlkaEconomyHook;
import com.alkacode.alkaessentials.manager.PlayerWarpManager;
import com.alkacode.alkaessentials.model.PlayerWarp;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lista paginada de warps de jogador - 3 modos (navegar todos/meus/favoritos) e busca
 * opcional por nome/dono. Clicar num warp abre {@link PlayerWarpDetailMenu}. */
public final class PlayerWarpsMenu extends BaseGui {

    public enum Mode { BROWSE, MINE, FAVORITES }

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int PAGE_SIZE = SLOTS.length;

    private final PlayerWarpManager warps;
    private final TeleportService teleports;
    private final AlkaEconomyHook economy;
    private final Mode mode;
    private final String query;
    private final int page;

    public PlayerWarpsMenu(JavaPlugin plugin, Player viewer, PlayerWarpManager warps, TeleportService teleports,
                            AlkaEconomyHook economy, Mode mode) {
        this(plugin, viewer, warps, teleports, economy, mode, null, 0);
    }

    public PlayerWarpsMenu(JavaPlugin plugin, Player viewer, PlayerWarpManager warps, TeleportService teleports,
                            AlkaEconomyHook economy, Mode mode, String query, int page) {
        super(plugin, viewer, MenuConfig.getInstance().title(titleKey(mode), null), 6, "alkaessentials_pwarps");
        this.warps = warps;
        this.teleports = teleports;
        this.economy = economy;
        this.mode = mode;
        this.query = query;
        this.page = page;
    }

    private static String titleKey(Mode mode) {
        return switch (mode) {
            case BROWSE -> "pwarp-gui-title";
            case MINE -> "pwarp-mine-title";
            case FAVORITES -> "pwarp-favorites-title";
        };
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("pwarps.glass", null));

        List<PlayerWarp> list = switch (mode) {
            case BROWSE -> query != null && !query.isBlank()
                    ? warps.search(player, query) : warps.listVisibleTo(player);
            case MINE -> warps.listOwnedBy(player.getUniqueId());
            case FAVORITES -> warps.favoritesOf(player.getUniqueId());
        };

        if (list.isEmpty()) {
            setItem(22, MenuConfig.getInstance().item("pwarps.empty", null));
        }

        int from = page * PAGE_SIZE;
        for (int i = 0; i < SLOTS.length && (from + i) < list.size(); i++) {
            PlayerWarp warp = list.get(from + i);
            setItem(SLOTS[i], buildIcon(warp), event ->
                    new PlayerWarpDetailMenu(plugin, player, warps, warp, this, teleports, economy).open());
        }

        if (page > 0) {
            setItem(45, MenuConfig.getInstance().item("pwarps.prev-page", null),
                    event -> new PlayerWarpsMenu(plugin, player, warps, teleports, economy, mode, query, page - 1).open());
        }
        if (from + PAGE_SIZE < list.size()) {
            setItem(53, MenuConfig.getInstance().item("pwarps.next-page", null),
                    event -> new PlayerWarpsMenu(plugin, player, warps, teleports, economy, mode, query, page + 1).open());
        }

        setItem(48, MenuConfig.getInstance().item("pwarps.tab-browse", null),
                event -> new PlayerWarpsMenu(plugin, player, warps, teleports, economy, Mode.BROWSE).open());
        setItem(49, MenuConfig.getInstance().item("pwarps.tab-mine", null),
                event -> new PlayerWarpsMenu(plugin, player, warps, teleports, economy, Mode.MINE).open());
        setItem(50, MenuConfig.getInstance().item("pwarps.tab-favorites", null),
                event -> new PlayerWarpsMenu(plugin, player, warps, teleports, economy, Mode.FAVORITES).open());
    }

    private ItemStack buildIcon(PlayerWarp warp) {
        Material material = Material.matchMaterial(warp.material());
        ItemStack item = new ItemStack(material != null ? material : Material.GRASS_BLOCK);
        String ownerName = Bukkit.getOfflinePlayer(warp.owner()).getName();
        double avg = warps.averageRating(warp.id());
        Map<String, String> placeholders = Map.of(
                "warp", warp.name(),
                "owner", ownerName == null ? "?" : ownerName,
                "category", warp.category(),
                "stars", String.format(Locale.US, "%.1f", avg),
                "votes", String.valueOf(warps.ratingCount(warp.id())),
                "price", warp.price() > 0 ? String.format(Locale.US, "%.0f %s", warp.price(), warp.currency()) : "Gratis"
        );
        ItemMeta meta = item.getItemMeta();
        meta.displayName(com.alkacode.alkaessentials.util.ChatUtil.parse(
                MenuConfig.getInstance().name("pwarps.warp-icon", placeholders)));
        meta.lore(MenuConfig.getInstance().lore("pwarps.warp-icon", placeholders));
        item.setItemMeta(meta);
        return item;
    }
}
