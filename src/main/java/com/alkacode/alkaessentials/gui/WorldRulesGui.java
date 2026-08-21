package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.gui.layout.GuiLayoutLoader;
import com.alkacode.alkaessentials.manager.WorldRulesManager;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Selecao do mundo pra editar as regras. */
public final class WorldRulesGui extends BaseGui {

    private final WorldRulesManager rules;

    public WorldRulesGui(JavaPlugin plugin, Player player, WorldRulesManager rules) {
        super(plugin, player, MenuConfig.getInstance().title("worldrules.select-title", null), 4,
                "alkaessentials_worldrules");
        this.rules = rules;
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("worldrules.glass", null));

        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_worldrules");
        List<Integer> slots = layout.findSlots('0');

        List<World> worlds = List.copyOf(rules.getWorlds().values());
        for (int i = 0; i < slots.size() && i < worlds.size(); i++) {
            World world = worlds.get(i);
            setItem(slots.get(i), MenuConfig.getInstance().item("worldrules.world", Map.of("world", world.getName())),
                    event -> new WorldRuleToggleGui(plugin, player, rules, world).open());
        }
    }
}
