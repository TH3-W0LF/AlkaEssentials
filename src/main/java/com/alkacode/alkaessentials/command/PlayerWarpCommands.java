package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.gui.PlayerWarpDetailMenu;
import com.alkacode.alkaessentials.gui.PlayerWarpsMenu;
import com.alkacode.alkaessentials.hook.AlkaEconomyHook;
import com.alkacode.alkaessentials.manager.PlayerWarpManager;
import com.alkacode.alkaessentials.model.PlayerWarp;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /pwarps - warps criados pelos proprios jogadores (distinto de /warp, do admin). */
public final class PlayerWarpCommands extends BaseCommand {

    private final PlayerWarpManager warps;
    private final TeleportService teleports;
    private final AlkaEconomyHook economy;

    public PlayerWarpCommands(JavaPlugin plugin, PlayerWarpManager warps, TeleportService teleports,
                               AlkaEconomyHook economy) {
        super(plugin);
        this.warps = warps;
        this.teleports = teleports;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            new PlayerWarpsMenu(plugin, player, warps, teleports, economy, PlayerWarpsMenu.Mode.BROWSE).open();
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "criar" -> create(player, args);
            case "deletar" -> delete(player, args);
            case "tp" -> teleport(player, args);
            case "favoritar" -> favorite(player, args);
            case "avaliar" -> rate(player, args);
            case "descricao" -> description(player, args);
            case "categoria" -> category(player, args);
            case "visibilidade" -> visibility(player, args);
            case "preco" -> price(player, args);
            case "whitelist" -> whitelist(player, args);
            case "buscar" -> search(player, args);
            case "meus" -> new PlayerWarpsMenu(plugin, player, warps, teleports, economy, PlayerWarpsMenu.Mode.MINE).open();
            case "favoritos" -> new PlayerWarpsMenu(plugin, player, warps, teleports, economy, PlayerWarpsMenu.Mode.FAVORITES).open();
            default -> ChatUtil.sendKey(player, "pwarp-usage");
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        String name = args[1];
        if (!warps.isValidName(name)) {
            ChatUtil.sendKey(player, "pwarp-name-invalid");
            return;
        }
        if (warps.nameTaken(player.getUniqueId(), name)) {
            ChatUtil.sendKey(player, "pwarp-name-taken", Map.of("warp", name));
            return;
        }
        int limit = warps.limitFor(player);
        if (warps.countOwnedBy(player.getUniqueId()) >= limit) {
            ChatUtil.sendKey(player, "pwarp-limit-reached", Map.of("limit", String.valueOf(limit)));
            return;
        }
        String description = args.length > 2 ? String.join(" ", List.of(args).subList(2, args.length)) : "";
        PlayerWarp warp = warps.create(player, name, description, "geral");
        if (warp == null) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        ChatUtil.sendKey(player, "pwarp-created", Map.of("warp", name));
    }

    private void delete(Player player, String[] args) {
        PlayerWarp warp = findOwnedOrAdmin(player, args, "deletar");
        if (warp == null) {
            return;
        }
        warps.delete(warp.id());
        ChatUtil.sendKey(player, "pwarp-deleted", Map.of("warp", warp.name()));
    }

    private void teleport(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        PlayerWarp warp = findAccessible(player, args[1]);
        if (warp == null) {
            return;
        }
        new PlayerWarpDetailMenu(plugin, player, warps, warp, null, teleports, economy).open();
    }

    private void favorite(Player player, String[] args) {
        PlayerWarp warp = findAccessible(player, args.length > 1 ? args[1] : "");
        if (warp == null) {
            return;
        }
        warps.toggleFavorite(player.getUniqueId(), warp.id());
        boolean now = warps.isFavorite(player.getUniqueId(), warp.id());
        ChatUtil.sendKey(player, now ? "pwarp-favorited" : "pwarp-unfavorited", Map.of("warp", warp.name()));
    }

    private void rate(Player player, String[] args) {
        if (args.length < 3) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        PlayerWarp warp = findAccessible(player, args[1]);
        if (warp == null) {
            return;
        }
        int stars;
        try {
            stars = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatUtil.sendKey(player, "pwarp-rate-invalid");
            return;
        }
        if (stars < 1 || stars > 5) {
            ChatUtil.sendKey(player, "pwarp-rate-invalid");
            return;
        }
        warps.rate(warp.id(), player.getUniqueId(), stars);
        ChatUtil.sendKey(player, "pwarp-rated", Map.of("warp", warp.name(), "stars", String.valueOf(stars)));
    }

    private void description(Player player, String[] args) {
        if (args.length < 3) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        PlayerWarp warp = findOwnedOrAdmin(player, args, "descricao");
        if (warp == null) {
            return;
        }
        warp.setDescription(String.join(" ", List.of(args).subList(2, args.length)));
        warps.save(warp);
        ChatUtil.sendKey(player, "pwarp-description-set", Map.of("warp", warp.name()));
    }

    private void category(Player player, String[] args) {
        if (args.length < 3) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        PlayerWarp warp = findOwnedOrAdmin(player, args, "categoria");
        if (warp == null) {
            return;
        }
        warp.setCategory(args[2]);
        warps.save(warp);
        ChatUtil.sendKey(player, "pwarp-category-set", Map.of("warp", warp.name(), "category", args[2]));
    }

    private void visibility(Player player, String[] args) {
        if (args.length < 3) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        PlayerWarp warp = findOwnedOrAdmin(player, args, "visibilidade");
        if (warp == null) {
            return;
        }
        PlayerWarp.Visibility visibility = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "publico", "public" -> PlayerWarp.Visibility.PUBLIC;
            case "privado", "private" -> PlayerWarp.Visibility.PRIVATE;
            case "whitelist" -> PlayerWarp.Visibility.WHITELIST;
            default -> null;
        };
        if (visibility == null) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        warp.setVisibility(visibility);
        warps.save(warp);
        ChatUtil.sendKey(player, "pwarp-visibility-set", Map.of("warp", warp.name(), "visibility", visibility.name()));
    }

    private void price(Player player, String[] args) {
        if (args.length < 3) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        PlayerWarp warp = findOwnedOrAdmin(player, args, "preco");
        if (warp == null) {
            return;
        }
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ChatUtil.sendKey(player, "invalid-number", Map.of("number", args[2]));
            return;
        }
        String currency = args.length > 3 ? args[3] : warp.currency();
        if (economy != null && value > 0 && !economy.isValidCurrency(currency)) {
            ChatUtil.sendKey(player, "invalid-number", Map.of("number", currency));
            return;
        }
        warp.setPrice(Math.max(0, value));
        warp.setCurrency(currency);
        warps.save(warp);
        ChatUtil.sendKey(player, "pwarp-price-set",
                Map.of("warp", warp.name(), "price", String.valueOf(warp.price()), "currency", currency));
    }

    private void whitelist(Player player, String[] args) {
        if (args.length < 4) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        PlayerWarp warp = findOwnedOrAdmin(player, args, "whitelist");
        if (warp == null) {
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        if (target.getName() == null) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[3]));
            return;
        }
        boolean add = args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("adicionar");
        if (add) {
            warps.whitelistAdd(warp.id(), target.getUniqueId());
            ChatUtil.sendKey(player, "pwarp-whitelist-added", Map.of("player", target.getName(), "warp", warp.name()));
        } else {
            warps.whitelistRemove(warp.id(), target.getUniqueId());
            ChatUtil.sendKey(player, "pwarp-whitelist-removed", Map.of("player", target.getName(), "warp", warp.name()));
        }
    }

    private void search(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return;
        }
        String query = String.join(" ", List.of(args).subList(1, args.length));
        new PlayerWarpsMenu(plugin, player, warps, teleports, economy, PlayerWarpsMenu.Mode.BROWSE, query, 0).open();
    }

    // ---------- helpers ----------

    private PlayerWarp findAccessible(Player player, String name) {
        PlayerWarp warp = findByAnyOwnerVisible(player, name);
        if (warp == null) {
            ChatUtil.sendKey(player, "pwarp-not-found", Map.of("warp", name));
            return null;
        }
        if (!warps.canAccess(player, warp)) {
            ChatUtil.sendKey(player, "pwarp-no-access");
            return null;
        }
        return warp;
    }

    private PlayerWarp findOwnedOrAdmin(Player player, String[] args, String usageArgName) {
        if (args.length < 2) {
            ChatUtil.sendKey(player, "pwarp-usage");
            return null;
        }
        PlayerWarp warp = findByAnyOwnerVisible(player, args[1]);
        if (warp == null) {
            ChatUtil.sendKey(player, "pwarp-not-found", Map.of("warp", args[1]));
            return null;
        }
        if (!warp.owner().equals(player.getUniqueId()) && !player.hasPermission("alkaessentials.warps.admin")) {
            ChatUtil.sendKey(player, "pwarp-not-owner");
            return null;
        }
        return warp;
    }

    /** Primeiro procura um warp DO PROPRIO jogador com esse nome (nomes so sao unicos
     * por dono); se nao achar, cai pra busca geral (warp publico/acessivel de qualquer
     * dono) - cobre tanto "meu warp" quanto "tp num warp de outro jogador". */
    private PlayerWarp findByAnyOwnerVisible(Player player, String name) {
        PlayerWarp own = warps.findByOwnerAndName(player.getUniqueId(), name);
        if (own != null) {
            return own;
        }
        return warps.listVisibleTo(player).stream()
                .filter(w -> w.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("criar", "deletar", "tp", "favoritar", "avaliar", "descricao",
                    "categoria", "visibilidade", "preco", "whitelist", "buscar", "meus", "favoritos"));
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("buscar") && sender instanceof Player p) {
            out.addAll(warps.listOwnedBy(p.getUniqueId()).stream().map(PlayerWarp::name).toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("visibilidade")) {
            out.addAll(List.of("publico", "privado", "whitelist"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            out.addAll(List.of("add", "remove"));
        }
        return out;
    }
}
