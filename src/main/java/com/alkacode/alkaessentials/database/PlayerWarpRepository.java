package com.alkacode.alkaessentials.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.alkaessentials.model.PlayerWarp;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Warps criados por jogadores (nao confundir com os warps globais de admin, guardados
 * em YAML - warps de jogador tem favoritos/avaliacao/whitelist por jogador, dado
 * relacional de verdade, entao vai pro banco do Core igual punicoes/InvRestore).
 */
public final class PlayerWarpRepository extends AbstractRepository {

    private static final String T_WARPS = "alka_essentials_player_warps";
    private static final String T_WHITELIST = "alka_essentials_player_warp_whitelist";
    private static final String T_FAVORITES = "alka_essentials_player_warp_favorites";
    private static final String T_RATINGS = "alka_essentials_player_warp_ratings";

    private final JavaPlugin plugin;

    public PlayerWarpRepository(DatabaseProvider db, JavaPlugin plugin) {
        super(db);
        this.plugin = plugin;
        createTables();
    }

    private void createTables() {
        String idColumn = db.isSQLite()
                ? "id INTEGER PRIMARY KEY AUTOINCREMENT"
                : "id BIGINT AUTO_INCREMENT PRIMARY KEY";
        runDdl("CREATE TABLE IF NOT EXISTS " + T_WARPS + " (" + idColumn
                + ", owner_uuid VARCHAR(36), name VARCHAR(32), world VARCHAR(64),"
                + " x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT,"
                + " description VARCHAR(255), category VARCHAR(32), visibility VARCHAR(16),"
                + " price DOUBLE, currency VARCHAR(32), material VARCHAR(64), created_at BIGINT)");
        runDdl("CREATE TABLE IF NOT EXISTS " + T_WHITELIST
                + " (warp_id BIGINT, uuid VARCHAR(36))");
        runDdl("CREATE TABLE IF NOT EXISTS " + T_FAVORITES
                + " (uuid VARCHAR(36), warp_id BIGINT)");
        runDdl("CREATE TABLE IF NOT EXISTS " + T_RATINGS
                + " (warp_id BIGINT, uuid VARCHAR(36), stars INT)");
    }

    private void runDdl(String sql) {
        try {
            execute(sql, ps -> {
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao criar tabela de player warps: " + e.getMessage());
        }
    }

    // ------------------------------- warps -------------------------------

    public List<PlayerWarp> loadAll() {
        List<PlayerWarp> out = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + T_WARPS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar player warps: " + e.getMessage());
        }
        return out;
    }

    private PlayerWarp map(ResultSet rs) throws SQLException {
        return new PlayerWarp(rs.getLong("id"), UUID.fromString(rs.getString("owner_uuid")), rs.getString("name"),
                rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getFloat("yaw"), rs.getFloat("pitch"), rs.getString("description"), rs.getString("category"),
                PlayerWarp.Visibility.valueOf(rs.getString("visibility")), rs.getDouble("price"),
                rs.getString("currency"), rs.getString("material"), rs.getLong("created_at"));
    }

    /** Insere e devolve o id gerado, ou -1 se falhou. */
    public long insert(PlayerWarp warp) {
        String sql = "INSERT INTO " + T_WARPS + " (owner_uuid, name, world, x, y, z, yaw, pitch,"
                + " description, category, visibility, price, currency, material, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            Location location = warp.toLocation();
            ps.setString(1, warp.owner().toString());
            ps.setString(2, warp.name());
            ps.setString(3, location != null ? location.getWorld().getName() : "");
            ps.setDouble(4, location != null ? location.getX() : 0);
            ps.setDouble(5, location != null ? location.getY() : 0);
            ps.setDouble(6, location != null ? location.getZ() : 0);
            ps.setFloat(7, location != null ? location.getYaw() : 0f);
            ps.setFloat(8, location != null ? location.getPitch() : 0f);
            ps.setString(9, warp.description());
            ps.setString(10, warp.category());
            ps.setString(11, warp.visibility().name());
            ps.setDouble(12, warp.price());
            ps.setString(13, warp.currency());
            ps.setString(14, warp.material());
            ps.setLong(15, warp.createdAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao criar player warp: " + e.getMessage());
            return -1;
        }
    }

    public void update(PlayerWarp warp) {
        String sql = "UPDATE " + T_WARPS + " SET description = ?, category = ?, visibility = ?,"
                + " price = ?, currency = ?, material = ? WHERE id = ?";
        try {
            execute(sql, ps -> {
                ps.setString(1, warp.description());
                ps.setString(2, warp.category());
                ps.setString(3, warp.visibility().name());
                ps.setDouble(4, warp.price());
                ps.setString(5, warp.currency());
                ps.setString(6, warp.material());
                ps.setLong(7, warp.id());
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao atualizar player warp " + warp.id() + ": " + e.getMessage());
        }
    }

    public void delete(long id) {
        try {
            execute("DELETE FROM " + T_WARPS + " WHERE id = ?", ps -> ps.setLong(1, id));
            execute("DELETE FROM " + T_WHITELIST + " WHERE warp_id = ?", ps -> ps.setLong(1, id));
            execute("DELETE FROM " + T_FAVORITES + " WHERE warp_id = ?", ps -> ps.setLong(1, id));
            execute("DELETE FROM " + T_RATINGS + " WHERE warp_id = ?", ps -> ps.setLong(1, id));
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao apagar player warp " + id + ": " + e.getMessage());
        }
    }

    // ------------------------------ whitelist ------------------------------

    public List<UUID> loadWhitelist(long warpId) {
        List<UUID> out = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid FROM " + T_WHITELIST + " WHERE warp_id = ?")) {
            ps.setLong(1, warpId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(UUID.fromString(rs.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar whitelist do warp " + warpId + ": " + e.getMessage());
        }
        return out;
    }

    public void whitelistAdd(long warpId, UUID uuid) {
        try {
            execute("INSERT INTO " + T_WHITELIST + " (warp_id, uuid) VALUES (?, ?)", ps -> {
                ps.setLong(1, warpId);
                ps.setString(2, uuid.toString());
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao adicionar na whitelist: " + e.getMessage());
        }
    }

    public void whitelistRemove(long warpId, UUID uuid) {
        try {
            execute("DELETE FROM " + T_WHITELIST + " WHERE warp_id = ? AND uuid = ?", ps -> {
                ps.setLong(1, warpId);
                ps.setString(2, uuid.toString());
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao remover da whitelist: " + e.getMessage());
        }
    }

    // ------------------------------ favoritos ------------------------------

    public List<Long> loadFavorites(UUID uuid) {
        List<Long> out = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT warp_id FROM " + T_FAVORITES + " WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getLong("warp_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar favoritos de " + uuid + ": " + e.getMessage());
        }
        return out;
    }

    public void favoriteAdd(UUID uuid, long warpId) {
        try {
            execute("INSERT INTO " + T_FAVORITES + " (uuid, warp_id) VALUES (?, ?)", ps -> {
                ps.setString(1, uuid.toString());
                ps.setLong(2, warpId);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao favoritar: " + e.getMessage());
        }
    }

    public void favoriteRemove(UUID uuid, long warpId) {
        try {
            execute("DELETE FROM " + T_FAVORITES + " WHERE uuid = ? AND warp_id = ?", ps -> {
                ps.setString(1, uuid.toString());
                ps.setLong(2, warpId);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao desfavoritar: " + e.getMessage());
        }
    }

    // ------------------------------ avaliacoes ------------------------------

    /** warp_id -> {media, quantidade} - carregado tudo de uma vez no boot pra nao
     * bater no banco toda vez que uma GUI renderiza a lista. */
    public java.util.Map<Long, double[]> loadRatingSummary() {
        java.util.Map<Long, double[]> sums = new java.util.HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT warp_id, AVG(stars) as avg_stars, COUNT(*) as total FROM " + T_RATINGS
                             + " GROUP BY warp_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sums.put(rs.getLong("warp_id"), new double[]{rs.getDouble("avg_stars"), rs.getDouble("total")});
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar resumo de avaliacoes: " + e.getMessage());
        }
        return sums;
    }

    public Integer loadOwnRating(long warpId, UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT stars FROM " + T_RATINGS + " WHERE warp_id = ? AND uuid = ?")) {
            ps.setLong(1, warpId);
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("stars") : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar avaliacao propria: " + e.getMessage());
            return null;
        }
    }

    public void rate(long warpId, UUID uuid, int stars) {
        try {
            String sql = upsert(T_RATINGS, new String[]{"warp_id", "uuid", "stars"}, new String[]{"warp_id", "uuid"});
            execute(sql, ps -> {
                ps.setLong(1, warpId);
                ps.setString(2, uuid.toString());
                ps.setInt(3, stars);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao avaliar warp " + warpId + ": " + e.getMessage());
        }
    }
}
