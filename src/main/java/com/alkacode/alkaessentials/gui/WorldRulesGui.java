package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.manager.WorldRulesManager;
import com.alkacode.core.gui.BaseGui;
import com.alkacode.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
        ItemBuilder glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ");
        fillBorder(glass.build());

        int slot = 10;
        for (World world : rules.getWorlds().values()) {
            setItem(slot, new ItemBuilder(Material.MAP).name("<gold>" + world.getName())
                    .lore("<gray>Clique para editar as regras").build(), event ->
                    new WorldRuleToggleGui(plugin, player, rules, world).open());
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 27) break;
        }
    }
}
