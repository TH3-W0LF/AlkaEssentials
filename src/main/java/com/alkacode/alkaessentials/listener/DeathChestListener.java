package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.DeathChestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/** Protege os tumulos de morte: nao podem ser quebrados nem explodir antes do tempo. */
public final class DeathChestListener implements Listener {

    private final DeathChestManager deathChest;

    public DeathChestListener(DeathChestManager deathChest) {
        this.deathChest = deathChest;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (deathChest.isDeathChest(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(deathChest::isDeathChest);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(deathChest::isDeathChest);
    }
}
