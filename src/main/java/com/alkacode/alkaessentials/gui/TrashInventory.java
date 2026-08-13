package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MessagesConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Lixeira portatil: inventario de 3 linhas que apaga tudo ao fechar (ver QolListener). */
public final class TrashInventory implements InventoryHolder {

    private final Inventory inventory;

    public TrashInventory(Player player) {
        String title = MessagesConfig.getInstance().getRaw("qol-trash-title");
        this.inventory = Bukkit.createInventory(this, 27, MiniMessage.miniMessage().deserialize(title));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
