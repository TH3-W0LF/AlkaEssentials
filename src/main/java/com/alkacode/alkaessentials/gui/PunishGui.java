package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.config.ReasonsConfig;
import com.alkacode.alkaessentials.gui.layout.GuiLayoutLoader;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.model.Punishment;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
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
        super(plugin, staff, MenuConfig.getInstance().title("punish.title", Map.of("player", target.getName())),
                3, "alkaessentials_punish");
        this.target = target;
        this.punishments = punishments;
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("punish.glass", null));

        switch (step) {
            case TYPE -> renderTypes();
            case REASON -> renderReasons();
            case DURATION -> renderDurations();
            case CONFIRM -> renderConfirm();
        }
    }

    private void renderTypes() {
        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_punish-types");
        List<Integer> slots = layout.findSlots('0');
        List<String> types = ReasonsConfig.getInstance().getTypes();
        for (int i = 0; i < slots.size() && i < types.size(); i++) {
            String type = types.get(i);
            String name = ReasonsConfig.getInstance().getTypeName(type);
            setItem(slots.get(i), MenuConfig.getInstance().item("punish.type-icon", Map.of("type", name)), event -> {
                selectedType = type;
                step = Step.REASON;
                refresh();
            });
        }
    }

    private void renderReasons() {
        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_punish-reasons");
        List<Integer> slots = layout.findSlots('0');
        List<String> reasons = ReasonsConfig.getInstance().getReasons(selectedType);
        setItem(layout.firstSlot('V'), MenuConfig.getInstance().item("punish.back", null), event -> {
            step = Step.TYPE;
            refresh();
        });
        for (int i = 0; i < slots.size() && i < reasons.size(); i++) {
            String reason = reasons.get(i);
            setItem(slots.get(i), MenuConfig.getInstance().item("punish.reason-icon", Map.of("reason", reason)), event -> {
                selectedReason = reason;
                step = selectedType.toLowerCase().contains("temp") ? Step.DURATION : Step.CONFIRM;
                refresh();
            });
        }
    }

    private void renderDurations() {
        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_punish-durations");
        List<Integer> slots = layout.findSlots('0');
        setItem(layout.firstSlot('V'), MenuConfig.getInstance().item("punish.back", null), event -> {
            step = Step.REASON;
            refresh();
        });
        List<String> durations = ReasonsConfig.getInstance().getDurations();
        for (int i = 0; i < slots.size() && i < durations.size(); i++) {
            String duration = durations.get(i);
            setItem(slots.get(i), MenuConfig.getInstance().item("punish.duration-icon", Map.of("duration", duration)), event -> {
                selectedDuration = duration;
                step = Step.CONFIRM;
                refresh();
            });
        }
        // Permanente + cancelar (sobrescrevem slots do grid, igual o comportamento original)
        setItem(layout.firstSlot('P'), MenuConfig.getInstance().item("punish.permanent", null), event -> {
            selectedDuration = null;
            step = Step.CONFIRM;
            refresh();
        });
        setItem(layout.firstSlot('X'), MenuConfig.getInstance().item("punish.cancel", null),
                event -> player.closeInventory());
    }

    private void renderConfirm() {
        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_punish-confirm");
        ItemStack info = MenuConfig.getInstance().item("punish.info", Map.of(
                "player", target.getName(),
                "type", selectedType,
                "reason", selectedReason,
                "duration", selectedDuration != null ? "<gold>Duracao: " + selectedDuration : "<white>Duracao: Permanente"
        ));
        setItem(layout.firstSlot('I'), info);

        setItem(layout.firstSlot('C'), MenuConfig.getInstance().item("punish.confirm", null), event -> {
            apply();
            player.closeInventory();
        });
        setItem(layout.firstSlot('X'), MenuConfig.getInstance().item("punish.cancel", null),
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
