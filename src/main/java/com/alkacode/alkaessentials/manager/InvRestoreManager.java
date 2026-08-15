package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.database.InvSnapshotRepository;
import com.alkacode.alkaessentials.util.InventoryCodec;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Historico de snapshots de inventario a cada morte pro /invrestore. Guarda inventario
 * principal (36) + armadura (4) + offhand (1) no banco do AlkaCore. Mantem os N
 * snapshots mais recentes por jogador (invrestore.history-size no config.yml) - o
 * admin escolhe QUAL restaurar via GUI (InvRestoreGui), nunca so "o ultimo" cego.
 */
public final class InvRestoreManager {

    private final InvSnapshotRepository repository;
    private final JavaPlugin plugin;

    public InvRestoreManager(InvSnapshotRepository repository, JavaPlugin plugin) {
        this.repository = repository;
        this.plugin = plugin;
    }

    /** Salva um novo snapshot (usado na morte) - nao apaga os anteriores, so poda o
     * historico alem do limite configurado. */
    public void save(Player player) {
        repository.insertSnapshot(player.getUniqueId(), InventoryCodec.encode(capture(player)));
        int keep = Math.max(1, plugin.getConfig().getInt("invrestore.history-size", 5));
        repository.pruneOld(player.getUniqueId(), keep);
    }

    /** Historico completo (mais recente primeiro) de um jogador, ate o limite configurado. */
    public List<InvSnapshotRepository.Snapshot> history(UUID uuid) {
        int keep = Math.max(1, plugin.getConfig().getInt("invrestore.history-size", 5));
        return repository.loadHistory(uuid, keep);
    }

    /** Aplica um snapshot especifico (por id) - true se encontrado e restaurado. */
    public boolean restore(Player player, long snapshotId) {
        String data = repository.loadSnapshotData(player.getUniqueId(), snapshotId);
        if (data == null || data.isEmpty()) {
            return false;
        }
        ItemStack[] items = InventoryCodec.decode(data);
        if (items.length == 0) {
            return false;
        }
        apply(player, items);
        return true;
    }

    /** Decodifica um snapshot em itens, sem aplicar - usado pela GUI de preview. */
    public ItemStack[] preview(String data) {
        return InventoryCodec.decode(data);
    }

    private ItemStack[] capture(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] main = inv.getContents();
        ItemStack[] armor = inv.getArmorContents();
        ItemStack[] all = new ItemStack[41];
        System.arraycopy(main, 0, all, 0, Math.min(36, main.length));
        System.arraycopy(armor, 0, all, 36, Math.min(4, armor.length));
        all[40] = inv.getItemInOffHand();
        return all;
    }

    private void apply(Player player, ItemStack[] all) {
        PlayerInventory inv = player.getInventory();
        if (all.length != 41) {
            inv.setContents(all);
            return;
        }
        ItemStack[] main = new ItemStack[36];
        System.arraycopy(all, 0, main, 0, 36);
        inv.setContents(main);
        ItemStack[] armor = new ItemStack[4];
        System.arraycopy(all, 36, armor, 0, 4);
        inv.setArmorContents(armor);
        inv.setItemInOffHand(all[40]);
    }
}
