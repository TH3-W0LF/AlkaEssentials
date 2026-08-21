package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.database.InvSnapshotRepository;
import com.alkacode.alkaessentials.gui.layout.GuiLayoutLoader;
import com.alkacode.alkaessentials.manager.InvRestoreManager;
import com.alkacode.core.gui.BaseGui;
import com.alkacode.core.util.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Lista os snapshots de inventario de um jogador (mais recente primeiro) - clicar
 * abre {@link InvRestorePreviewGui} pra ver o conteudo antes de restaurar de verdade. */
public final class InvRestoreGui extends BaseGui {

    private final InvRestoreManager invRestore;
    private final OfflinePlayer target;

    public InvRestoreGui(JavaPlugin plugin, Player admin, InvRestoreManager invRestore, OfflinePlayer target) {
        super(plugin, admin, MenuConfig.getInstance().title("invrestore.history-title",
                Map.of("player", target.getName() == null ? "?" : target.getName())), 4, "alkaessentials_invrestore");
        this.invRestore = invRestore;
        this.target = target;
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("invrestore.glass", null));

        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_invrestore");
        List<Integer> slots = layout.findSlots('0');

        List<InvSnapshotRepository.Snapshot> history = invRestore.history(target.getUniqueId());
        if (history.isEmpty()) {
            setItem(slots.get(slots.size() / 2), MenuConfig.getInstance().item("invrestore.empty",
                    Map.of("player", target.getName())));
            return;
        }

        long now = System.currentTimeMillis();
        for (int i = 0; i < slots.size() && i < history.size(); i++) {
            InvSnapshotRepository.Snapshot snapshot = history.get(i);
            String when = TimeUtil.formatSeconds((now - snapshot.time()) / 1000) + " atras";
            ItemStack icon = MenuConfig.getInstance().item("invrestore.snapshot", Map.of("when", when));
            setItem(slots.get(i), icon, event ->
                    new InvRestorePreviewGui(plugin, player, invRestore, target, snapshot, when).open());
        }
    }
}
