package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.database.InvSnapshotRepository;
import com.alkacode.alkaessentials.util.InventoryCodec;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Snapshot de inventario a cada morte pro /invrestore. Guarda inventario principal
 * (36) + armadura (4) + offhand (1) no banco do AlkaCore, e restaura tudo de volta.
 */
public final class InvRestoreManager {

    private final InvSnapshotRepository repository;

    public InvRestoreManager(InvSnapshotRepository repository) {
        this.repository = repository;
    }

    /** Salva o inventario atual do jogador (usado na morte). */
    public void save(Player player) {
        repository.saveSnapshot(player.getUniqueId(), InventoryCodec.encode(capture(player)));
    }

    /** Restaura o ultimo snapshot salvo. Retorna false se nao havia snapshot. */
    public boolean restore(Player player) {
        String data = repository.loadSnapshot(player.getUniqueId());
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
