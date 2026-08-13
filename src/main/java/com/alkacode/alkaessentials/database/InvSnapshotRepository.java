package com.alkacode.alkaessentials.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Snapshot de inventario pro /invrestore, no banco do AlkaCore (decisao do projeto:
 * punicoes e InvRestore sao dado transacional, vao em SQL). Guarda so o ultimo
 * snapshot por jogador (PRIMARY KEY uuid, upsert a cada morte).
 */
public final class InvSnapshotRepository extends AbstractRepository {

    private static final String TABLE = "alka_essentials_inv_snapshots";

    private final JavaPlugin plugin;

    public InvSnapshotRepository(DatabaseProvider db, JavaPlugin plugin) {
        super(db);
        this.plugin = plugin;
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE
                + " (uuid VARCHAR(36) PRIMARY KEY, snapshot_time BIGINT, data TEXT)";
        try {
            execute(sql, ps -> {
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao criar tabela " + TABLE + ": " + e.getMessage());
        }
    }

    public void saveSnapshot(UUID uuid, String data) {
        try {
            String sql = upsert(TABLE, new String[]{"uuid", "snapshot_time", "data"}, new String[]{"uuid"});
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, data);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao salvar snapshot de " + uuid + ": " + e.getMessage());
        }
    }

    public String loadSnapshot(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT data FROM " + TABLE + " WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("data");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar snapshot de " + uuid + ": " + e.getMessage());
        }
        return null;
    }
}
