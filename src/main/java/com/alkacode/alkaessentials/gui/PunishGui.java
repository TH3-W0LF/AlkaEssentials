package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.ReasonsConfig;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.model.Punishment;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
import com.alkacode.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/**
 * Menu premium de punicao (/punish <jogador>): fluxo Tipo -> Motivo -> Duracao -> Confirmar.
 * Cada passo re-renderiza a mesma GUI via refresh().
 */
public final class PunishGui extends BaseGui {

    private enum Step { TYPE, REASON, DURATION, CONFIRM }

    private final Player target;
    private final PunishmentManager punishments;

    private Step step = Step.TYPE;
    private String selectedType;
    private String selectedReason;
    private String selectedDuration;

    public PunishGui(JavaPlugin plugin, Player staff, Player target, PunishmentManager punishments) {
        super(plugin, staff, "<gold>Punir " + target.getName(), 3, "alkaessentials_punish");
        this.target = target;
        this.punishments = punishments;
    }

    @Override
    public void render() {
        ItemBuilder glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ");
        fillBorder(glass.build());

        switch (step) {
            case TYPE -> renderTypes();
            case REASON -> renderReasons();
            case DURATION -> renderDurations();
            case CONFIRM -> renderConfirm();
        }
    }

    private void renderTypes() {
        List<String> types = ReasonsConfig.getInstance().getTypes();
        int slot = 10;
        for (String type : types) {
            String name = ReasonsConfig.getInstance().getTypeName(type);
            setItem(slot, new ItemBuilder(Material.WRITABLE_BOOK).name(name).build(), event -> {
                selectedType = type;
                step = Step.REASON;
                refresh();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 27) break;
        }
    }

    private void renderReasons() {
        List<String> reasons = ReasonsConfig.getInstance().getReasons(selectedType);
        setItem(0, new ItemBuilder(Material.ARROW).name("<gray>Voltar").build(), event -> {
            step = Step.TYPE;
            refresh();
        });
        int slot = 10;
        for (String reason : reasons) {
            setItem(slot, new ItemBuilder(Material.PAPER).name("<white>" + reason).build(), event -> {
                selectedReason = reason;
                step = selectedType.toLowerCase().contains("temp") ? Step.DURATION : Step.CONFIRM;
                refresh();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 27) break;
        }
    }

    private void renderDurations() {
        setItem(0, new ItemBuilder(Material.ARROW).name("<gray>Voltar").build(), event -> {
            step = Step.REASON;
            refresh();
        });
        List<String> durations = ReasonsConfig.getInstance().getDurations();
        int slot = 10;
        for (String duration : durations) {
            setItem(slot, new ItemBuilder(Material.CLOCK).name("<gold>" + duration).build(), event -> {
                selectedDuration = duration;
                step = Step.CONFIRM;
                refresh();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 27) break;
        }
        // Permanente + cancelar
        setItem(22, new ItemBuilder(Material.NETHER_STAR).name("<white>Permanente").build(), event -> {
            selectedDuration = null;
            step = Step.CONFIRM;
            refresh();
        });
        setItem(4, new ItemBuilder(Material.BARRIER).name("<red>Cancelar").build(),
                event -> player.closeInventory());
    }

    private void renderConfirm() {
        ItemStack info = new ItemBuilder(Material.PAPER)
                .name("<gold>Confirmar punicao")
                .lore("<gray>Alvo: <white>" + target.getName())
                .lore("<red>Tipo: <white>" + selectedType)
                .lore("<white>Motivo: " + selectedReason)
                .lore(selectedDuration != null ? "<gold>Duracao: " + selectedDuration : "<white>Duracao: Permanente")
                .build();
        setItem(13, info);

        setItem(11, new ItemBuilder(Material.GREEN_DYE).name("<green>Confirmar").build(), event -> {
            apply();
            player.closeInventory();
        });
        setItem(15, new ItemBuilder(Material.BARRIER).name("<red>Cancelar").build(),
                event -> player.closeInventory());
    }

    private void apply() {
        String type = punishments.dbType(selectedType);
        String duration = selectedType.toLowerCase().contains("temp") ? selectedDuration : null;
        String reason = selectedReason == null ? "" : selectedReason;
        Punishment p = punishments.apply(target.getUniqueId(), target.getName(), type, reason,
                "", player.getName(), duration);
        punishments.applyImmediate(p);
        punishments.broadcast(p, selectedType, "punido", player.getName(), reason);
        ChatUtil.sendKey(player, "punish-applied", Map.of("player", target.getName()));
    }
}
