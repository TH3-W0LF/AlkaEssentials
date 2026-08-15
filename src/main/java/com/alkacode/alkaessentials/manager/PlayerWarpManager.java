package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.database.PlayerWarpRepository;
import com.alkacode.alkaessentials.model.PlayerWarp;
import com.alkacode.core.api.AlkaAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Warps de jogador: cache em memoria carregado 1x no boot (recarrega no /alkaessentials
 * reload), escritas vao pro repositorio - cache atualizado direto (otimista) pra GUI
 * responder na hora, sem esperar o round-trip do banco.
 */
public final class PlayerWarpManager {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,24}$");

    private final JavaPlugin plugin;
    private final PlayerWarpRepository repository;
    private final Map<Long, PlayerWarp> warps = new ConcurrentHashMap<>();
    private final Map<Long, double[]> ratingSummary = new ConcurrentHashMap<>();

    public PlayerWarpManager(JavaPlugin plugin, PlayerWarpRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void load() {
        AlkaAPI.get().getScheduler().runAsync(() -> {
            List<PlayerWarp> loaded = repository.loadAll();
            Map<Long, double[]> summary = repository.loadRatingSummary();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                warps.clear();
                for (PlayerWarp warp : loaded) {
                    warps.put(warp.id(), warp);
                }
                ratingSummary.clear();
                ratingSummary.putAll(summary);
            });
        });
    }

    public boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    public boolean nameTaken(UUID owner, String name) {
        return warps.values().stream()
                .anyMatch(w -> w.owner().equals(owner) && w.name().equalsIgnoreCase(name));
    }

    public int limitFor(Player player) {
        int limit = plugin.getConfig().getInt("player-warps.default-limit", 1);
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String node = info.getPermission();
            if (info.getValue() && node.startsWith("alkaessentials.warps.")) {
                String suffix = node.substring("alkaessentials.warps.".length());
                try {
                    limit = Math.max(limit, Integer.parseInt(suffix));
                } catch (NumberFormatException ignored) {
                    // nao e um node numerico (ex: alkaessentials.warps.admin), ignora
                }
            }
        }
        return limit;
    }

    public int countOwnedBy(UUID owner) {
        return (int) warps.values().stream().filter(w -> w.owner().equals(owner)).count();
    }

    /** Cria e persiste - devolve o warp com o id ja atribuido, ou null se falhou. */
    public PlayerWarp create(Player owner, String name, String description, String category) {
        Location loc = owner.getLocation();
        PlayerWarp warp = new PlayerWarp(-1, owner.getUniqueId(), name, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                description == null ? "" : description, category == null ? "geral" : category,
                PlayerWarp.Visibility.PUBLIC, 0.0, "coins", "GRASS_BLOCK", System.currentTimeMillis());
        long id = repository.insert(warp);
        if (id < 0) {
            return null;
        }
        PlayerWarp saved = new PlayerWarp(id, warp.owner(), warp.name(), loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), warp.description(),
                warp.category(), warp.visibility(), warp.price(), warp.currency(), warp.material(), warp.createdAt());
        warps.put(id, saved);
        return saved;
    }

    public void delete(long id) {
        warps.remove(id);
        ratingSummary.remove(id);
        AlkaAPI.get().getScheduler().runAsync(() -> repository.delete(id));
    }

    public void save(PlayerWarp warp) {
        AlkaAPI.get().getScheduler().runAsync(() -> repository.update(warp));
    }

    public PlayerWarp get(long id) {
        return warps.get(id);
    }

    public PlayerWarp findByOwnerAndName(UUID owner, String name) {
        return warps.values().stream()
                .filter(w -> w.owner().equals(owner) && w.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public List<PlayerWarp> listOwnedBy(UUID owner) {
        return warps.values().stream()
                .filter(w -> w.owner().equals(owner))
                .sorted(Comparator.comparing(PlayerWarp::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Publicos + os que o jogador pode ver por dono/whitelist, ordenados por nota media
     * (desc) - mesma logica de "descoberta" que a GUI de navegacao usa. */
    public List<PlayerWarp> listVisibleTo(Player viewer) {
        List<PlayerWarp> out = new ArrayList<>();
        for (PlayerWarp warp : warps.values()) {
            if (canAccess(viewer, warp)) {
                out.add(warp);
            }
        }
        out.sort(Comparator.comparingDouble((PlayerWarp w) -> averageRating(w.id())).reversed());
        return out;
    }

    public List<PlayerWarp> search(Player viewer, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<PlayerWarp> out = new ArrayList<>();
        for (PlayerWarp warp : warps.values()) {
            if (!canAccess(viewer, warp)) {
                continue;
            }
            String ownerName = org.bukkit.Bukkit.getOfflinePlayer(warp.owner()).getName();
            boolean matches = warp.name().toLowerCase(Locale.ROOT).contains(needle)
                    || (ownerName != null && ownerName.toLowerCase(Locale.ROOT).contains(needle));
            if (matches) {
                out.add(warp);
            }
        }
        return out;
    }

    public boolean canAccess(Player viewer, PlayerWarp warp) {
        if (warp.owner().equals(viewer.getUniqueId()) || viewer.hasPermission("alkaessentials.warps.admin")) {
            return true;
        }
        return switch (warp.visibility()) {
            case PUBLIC -> true;
            case PRIVATE -> false;
            case WHITELIST -> repository.loadWhitelist(warp.id()).contains(viewer.getUniqueId());
        };
    }

    // ------------------------------ favoritos ------------------------------

    public List<PlayerWarp> favoritesOf(UUID uuid) {
        return repository.loadFavorites(uuid).stream()
                .map(warps::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public boolean isFavorite(UUID uuid, long warpId) {
        return repository.loadFavorites(uuid).contains(warpId);
    }

    public void toggleFavorite(UUID uuid, long warpId) {
        if (isFavorite(uuid, warpId)) {
            repository.favoriteRemove(uuid, warpId);
        } else {
            repository.favoriteAdd(uuid, warpId);
        }
    }

    // ------------------------------ avaliacoes ------------------------------

    public double averageRating(long warpId) {
        double[] summary = ratingSummary.get(warpId);
        return summary == null ? 0.0 : summary[0];
    }

    public int ratingCount(long warpId) {
        double[] summary = ratingSummary.get(warpId);
        return summary == null ? 0 : (int) summary[1];
    }

    public Integer ownRating(long warpId, UUID uuid) {
        return repository.loadOwnRating(warpId, uuid);
    }

    public void rate(long warpId, UUID uuid, int stars) {
        repository.rate(warpId, uuid, stars);
        Map<Long, double[]> refreshed = repository.loadRatingSummary();
        ratingSummary.put(warpId, refreshed.getOrDefault(warpId, new double[]{stars, 1}));
    }

    // ------------------------------ whitelist ------------------------------

    public List<UUID> whitelist(long warpId) {
        return repository.loadWhitelist(warpId);
    }

    public void whitelistAdd(long warpId, UUID uuid) {
        repository.whitelistAdd(warpId, uuid);
    }

    public void whitelistRemove(long warpId, UUID uuid) {
        repository.whitelistRemove(warpId, uuid);
    }
}
