package com.alkacode.alkaessentials.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Historico de snapshots de inventario pro /invrestore, no banco do AlkaCore (decisao
 * do projeto: punicoes e InvRestore sao dado transacional, vao em SQL). Guarda VARIOS
 * snapshots por jogador (nao so o ultimo) - id auto-incrementado, mais recente por
 * snapshot_time. O chamador (InvRestoreManager) e responsavel por podar snapshots
 * antigos alem do limite configurado.
 */
public final class InvSnapshotRepository extends AbstractRepository {

    private static final String TABLE = "alka_essentials_inv_snapshots";

    private final JavaPlugin plugin;

    public InvSnapshotRepository(DatabaseProvider db, JavaPlugin plugin) {
        super(db);
        this.plugin = plugin;
        createTable();
        migrateLegacySchema();
    }

    private void createTable() {
        String idColumn = db.isSQLite()
                ? "id INTEGER PRIMARY KEY AUTOINCREMENT"
                : "id BIGINT AUTO_INCREMENT PRIMARY KEY";
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE
                + " (" + idColumn + ", uuid VARCHAR(36), snapshot_time BIGINT, data TEXT)";
        try {
            execute(sql, ps -> {
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao criar tabela " + TABLE + ": " + e.getMessage());
        }
    }

    /** Servidores que ja rodavam a versao antiga (so "uuid" como PRIMARY KEY, 1 snapshot
     * por jogador) tem a tabela ja criada SEM a coluna "id" - CREATE TABLE IF NOT EXISTS
     * acima e um no-op nesse caso, entao migra na mao: renomeia, recria no esquema novo,
     * copia os dados, apaga a tabela antiga. Idempotente - so roda algo se "id" nao existir. */
    private void migrateLegacySchema() {
        if (hasIdColumn()) {
            return;
        }
        plugin.getLogger().warning("Migrando " + TABLE + " pro esquema com historico (versao antiga so guardava 1 snapshot por jogador)...");
        String legacy = TABLE + "_legacy";
        try {
            execute("DROP TABLE IF EXISTS " + legacy, ps -> {
            });
            execute("ALTER TABLE " + TABLE + " RENAME TO " + legacy, ps -> {
            });
            String idColumn = db.isSQLite()
                    ? "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    : "id BIGINT AUTO_INCREMENT PRIMARY KEY";
            execute("CREATE TABLE " + TABLE + " (" + idColumn + ", uuid VARCHAR(36), snapshot_time BIGINT, data TEXT)", ps -> {
            });
            execute("INSERT INTO " + TABLE + " (uuid, snapshot_time, data) SELECT uuid, snapshot_time, data FROM " + legacy, ps -> {
            });
            execute("DROP TABLE " + legacy, ps -> {
            });
            plugin.getLogger().info("Migracao de " + TABLE + " concluida.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao migrar " + TABLE + ": " + e.getMessage());
        }
    }

    private boolean hasIdColumn() {
        try (Connection conn = db.getConnection()) {
            if (db.isSQLite()) {
                try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + TABLE + ")");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if ("id".equalsIgnoreCase(rs.getString("name"))) {
                            return true;
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = 'id'")) {
                    ps.setString(1, TABLE);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() && rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao checar esquema de " + TABLE + ": " + e.getMessage());
        }
        return false;
    }

    /** Insere um novo snapshot (nunca sobrescreve os anteriores). */
    public void insertSnapshot(UUID uuid, String data) {
        try {
            execute("INSERT INTO " + TABLE + " (uuid, snapshot_time, data) VALUES (?, ?, ?)", ps -> {
                ps.setString(1, uuid.toString());
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, data);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao salvar snapshot de " + uuid + ": " + e.getMessage());
        }
    }

    public record Snapshot(long id, long time, String data) {
    }

    /** Mais recentes primeiro, ate `limit` snapshots. */
    public List<Snapshot> loadHistory(UUID uuid, int limit) {
        List<Snapshot> out = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, snapshot_time, data FROM " + TABLE
                             + " WHERE uuid = ? ORDER BY snapshot_time DESC LIMIT ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Snapshot(rs.getLong("id"), rs.getLong("snapshot_time"), rs.getString("data")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar historico de " + uuid + ": " + e.getMessage());
        }
        return out;
    }

    /** Snapshot especifico (validado contra o dono, pra um id de outro jogador nunca vazar). */
    public String loadSnapshotData(UUID uuid, long id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT data FROM " + TABLE + " WHERE id = ? AND uuid = ?")) {
            ps.setLong(1, id);
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("data");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar snapshot " + id + " de " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    /** Remove os snapshots mais antigos alem de `keep`, mantendo os mais recentes. */
    public void pruneOld(UUID uuid, int keep) {
        String sub = "SELECT id FROM " + TABLE + " WHERE uuid = ? ORDER BY snapshot_time DESC LIMIT ?";
        String sql = "DELETE FROM " + TABLE + " WHERE uuid = ? AND id NOT IN (" + sub + ")";
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, uuid.toString());
                ps.setInt(3, keep);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao podar historico de " + uuid + ": " + e.getMessage());
        }
    }
}
