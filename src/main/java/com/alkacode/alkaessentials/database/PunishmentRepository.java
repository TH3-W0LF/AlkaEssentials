package com.alkacode.alkaessentials.database;

import com.alkacode.alkaessentials.model.Punishment;
import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Punições no banco do AlkaCore (decisao do projeto: punicao e dado transacional). */
public final class PunishmentRepository extends AbstractRepository {

    private static final String TABLE = "alka_essentials_punishments";

    private final JavaPlugin plugin;

    public PunishmentRepository(DatabaseProvider db, JavaPlugin plugin) {
        super(db);
        this.plugin = plugin;
        createTable();
    }

    private void createTable() {
        String autoincrement = db.isSQLite() ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "BIGINT AUTO_INCREMENT PRIMARY KEY";
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id " + autoincrement + ", " +
                "target VARCHAR(36) NOT NULL, " +
                "target_name VARCHAR(16), " +
                "type VARCHAR(16) NOT NULL, " +
                "reason TEXT, " +
                "proof TEXT, " +
                "issuer VARCHAR(16), " +
                "server VARCHAR(32), " +
                "start_time BIGINT, " +
                "end_time BIGINT, " +
                "active TINYINT DEFAULT 1)";
        try {
            execute(sql, ps -> {
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao criar tabela " + TABLE + ": " + e.getMessage());
        }
    }

    public Punishment insert(Punishment punishment) {
        String sql = "INSERT INTO " + TABLE +
                " (target, target_name, type, reason, proof, issuer, server, start_time, end_time, active) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, punishment.getTarget().toString());
            ps.setString(2, punishment.getTargetName());
            ps.setString(3, punishment.getType());
            ps.setString(4, punishment.getReason());
            ps.setString(5, punishment.getProof());
            ps.setString(6, punishment.getIssuer());
            ps.setString(7, punishment.getServer());
            ps.setLong(8, punishment.getStartTime());
            ps.setLong(9, punishment.getEndTime());
            ps.setInt(10, punishment.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    punishment.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao inserir punicao: " + e.getMessage());
        }
        return punishment;
    }

    /** Punições ativas e nao expiradas de um alvo (filtrando o tipo com LIKE se informado). */
    public List<Punishment> getActive(UUID target, String typeLike) {
        List<Punishment> result = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE target = ? AND active = 1";
        if (typeLike != null && !typeLike.isBlank()) {
            sql += " AND type LIKE ?";
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            if (typeLike != null && !typeLike.isBlank()) {
                ps.setString(2, "%" + typeLike + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Punishment p = map(rs);
                    if (!p.isExpired()) {
                        result.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao buscar punicoes: " + e.getMessage());
        }
        return result;
    }

    public List<Punishment> getHistory(UUID target, int limit) {
        List<Punishment> result = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE target = ? ORDER BY start_time DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, target.toString());
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao buscar historico: " + e.getMessage());
        }
        return result;
    }

    public void setActive(int id, boolean active) {
        String sql = "UPDATE " + TABLE + " SET active = ? WHERE id = ?";
        try {
            execute(sql, ps -> {
                ps.setInt(1, active ? 1 : 0);
                ps.setInt(2, id);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao atualizar punicao #" + id + ": " + e.getMessage());
        }
    }

    /** Desativa punicoes temporarias expiradas. */
    public void expireActive() {
        String sql = "UPDATE " + TABLE + " SET active = 0 WHERE active = 1 AND end_time <> ? AND end_time < ?";
        try {
            execute(sql, ps -> {
                ps.setLong(1, Punishment.PERMANENT);
                ps.setLong(2, System.currentTimeMillis());
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao expirar punicoes: " + e.getMessage());
        }
    }

    private Punishment map(ResultSet rs) throws SQLException {
        return new Punishment(
                rs.getInt("id"),
                UUID.fromString(rs.getString("target")),
                rs.getString("target_name"),
                rs.getString("type"),
                rs.getString("reason"),
                rs.getString("proof"),
                rs.getString("issuer"),
                rs.getString("server"),
                rs.getLong("start_time"),
                rs.getLong("end_time"),
                rs.getInt("active") == 1);
    }
}
