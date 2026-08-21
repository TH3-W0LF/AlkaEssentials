package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.database.InvSnapshotRepository;
import com.alkacode.alkaessentials.gui.layout.GuiLayoutLoader;
import com.alkacode.alkaessentials.manager.InvRestoreManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/** Mostra o conteudo REAL de um snapshot (36 slots principais + armadura + offhand)
 * antes de restaurar - so entao o admin confirma. Nenhum slot aqui e clicavel pra
 * pegar item (GuiListener do AlkaCore ja cancela todo clique numa BaseGui). */
public final class InvRestorePreviewGui extends BaseGui {

    private final InvRestoreManager invRestore;
    private final OfflinePlayer target;
    private final InvSnapshotRepository.Snapshot snapshot;
    private final String when;

    public InvRestorePreviewGui(JavaPlugin plugin, Player admin, InvRestoreManager invRestore,
                                 OfflinePlayer target, InvSnapshotRepository.Snapshot snapshot, String when) {
        super(plugin, admin, MenuConfig.getInstance().title("invrestore.preview-title",
                Map.of("player", target.getName() == null ? "?" : target.getName(), "when", when)),
                6, "alkaessentials_invrestore_preview");
        this.invRestore = invRestore;
        this.target = target;
        this.snapshot = snapshot;
        this.when = when;
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("invrestore.glass", null));

        ItemStack[] items = invRestore.preview(snapshot.data());
        // 0-35 = inventario principal, na mesma posicao relativa de uma inventory real.
        for (int i = 0; i < 36 && i < items.length; i++) {
            if (items[i] != null && !items[i].getType().isAir()) {
                setItem(i, items[i]);
            }
        }
        // 36-39 = armadura, 40 = offhand - linha 5 (slots 36-44), lado a lado.
        int[] gearSlots = {37, 38, 39, 40, 41};
        for (int i = 0; i < 5 && (36 + i) < items.length; i++) {
            ItemStack item = items[36 + i];
            if (item != null && !item.getType().isAir()) {
                setItem(gearSlots[i], item);
            }
        }

        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_invrestore_preview");
        setItem(layout.firstSlot('C'), MenuConfig.getInstance().item("invrestore.cancel", null),
                event -> new InvRestoreGui(plugin, player, invRestore, target).open());
        setItem(layout.firstSlot('K'), MenuConfig.getInstance().item("invrestore.confirm",
                        Map.of("player", target.getName() == null ? "?" : target.getName())),
                event -> confirm());
    }

    private void confirm() {
        Player online = target.getPlayer();
        if (online == null || !online.isOnline()) {
            ChatUtil.sendKey(player, "invrestore-offline", Map.of("player", target.getName()));
            player.closeInventory();
            return;
        }
        boolean ok = invRestore.restore(online, snapshot.id());
        if (ok) {
            ChatUtil.sendKey(player, "invrestore-success", Map.of("player", online.getName()));
            ChatUtil.sendKey(online, "invrestore-restored-on-you", Map.of("when", when));
        } else {
            ChatUtil.sendKey(player, "invrestore-none", Map.of("player", online.getName()));
        }
        player.closeInventory();
    }
}
