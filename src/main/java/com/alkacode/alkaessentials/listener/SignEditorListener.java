package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Editor rapido de placas: staff segura shift e clica direito para abrir o editor nativo. */
public final class SignEditorListener implements Listener {

    private final JavaPlugin plugin;

    public SignEditorListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("signs.editor-enabled", true)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.getPlayer().isSneaking()) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }
        Player player = event.getPlayer();
        String permission = plugin.getConfig().getString("signs.editor-permission", "alkassentials.signs.edit");
        if (!player.hasPermission(permission)) {
            ChatUtil.sendKey(player, "signs-no-permission");
            return;
        }
        event.setCancelled(true);
        player.openSign(sign, Side.FRONT);
        ChatUtil.sendKey(player, "sign-edit-open");
    }
}
