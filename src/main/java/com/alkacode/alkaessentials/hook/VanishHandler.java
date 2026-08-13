package com.alkacode.alkaessentials.hook;

import com.alkacode.alkaessentials.manager.ModerationManager;
import com.alkacode.alkaessentials.manager.NickManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Vanish profundo: esconde o nome no TAB (via API do TAB) e suprime a animacao de
 * abertura de baús quando em vanish (via ProtocolLib). O ocultar do mundo fica no
 * comando /vanish (hidePlayer).
 */
public final class VanishHandler implements Listener {

    private final JavaPlugin plugin;
    private final ModerationManager moderation;
    private final NickManager nicks;
    private final Map<Long, Long> silentChests = new HashMap<>();

    public VanishHandler(JavaPlugin plugin, ModerationManager moderation, NickManager nicks) {
        this.plugin = plugin;
        this.moderation = moderation;
        this.nicks = nicks;
        // silent chest (ProtocolLib presente - so e construido quando instalado)
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.BLOCK_ACTION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    BlockPosition pos = event.getPacket().getBlockPositionModifier().read(0);
                    if (pos != null && isSilent(key(pos.getX(), pos.getY(), pos.getZ()))) {
                        event.setCancelled(true);
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    /** Vanish ligado: esconde o nome no TAB. */
    public void apply(Player player) {
        setTabName(player, " ");
    }

    /** Vanish desligado: restaura o nome (nick ou nome real) no TAB. */
    public void clear(Player player) {
        setTabName(player, tabName(player));
    }

    private String tabName(Player player) {
        String nick = nicks.getNick(player.getUniqueId());
        if (nick == null) {
            return player.getName();
        }
        try {
            return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(nick));
        } catch (Exception e) {
            return player.getName();
        }
    }

    private void setTabName(Player player, String name) {
        try {
            TabAPI tab = TabAPI.getInstance();
            TabPlayer tp = tab.getPlayer(player.getUniqueId());
            if (tp != null) {
                TabListFormatManager manager = tab.getTabListFormatManager();
                manager.setName(tp, name);
            }
        } catch (Throwable ignored) {
            // TAB nao presente ou API indisponivel - cai para o ocultar do mundo apenas
        }
    }

    // ---------- silent chest ----------

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!moderation.isVanished(player.getUniqueId())) return;
        Inventory inv = event.getInventory();
        if (inv.getType() == org.bukkit.event.inventory.InventoryType.PLAYER) return;
        if (!(inv.getHolder() instanceof BlockState state) || !(state instanceof Container)) return;
        Block block = state.getBlock();
        if (block.getType() == Material.ENDER_CHEST) return;
        silentChests.put(key(block.getX(), block.getY(), block.getZ()), System.currentTimeMillis() + 2000);
    }

    private boolean isSilent(long key) {
        Long expiry = silentChests.get(key);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            silentChests.remove(key);
            return false;
        }
        return true;
    }

    private long key(int x, int y, int z) {
        return (long) x * 16777216L + (long) y * 4096L + z;
    }
}
