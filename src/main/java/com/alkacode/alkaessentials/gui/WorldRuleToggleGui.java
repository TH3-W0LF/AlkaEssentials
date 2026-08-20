package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.manager.WorldRulesManager;
import com.alkacode.core.gui.BaseGui;
import com.alkacode.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Alterna as regras do mundo (ligar/desligar) e restaura o padrao. */
public final class WorldRuleToggleGui extends BaseGui {

    private final WorldRulesManager rules;
    private final World world;

    public WorldRuleToggleGui(JavaPlugin plugin, Player player, WorldRulesManager rules, World world) {
        super(plugin, player, MenuConfig.getInstance().title("worldrules.toggle-title",
                Map.of("world", world.getName())), 5, "alkaessentials_worldrules_toggle");
        this.rules = rules;
        this.world = world;
    }

    @Override
    public void render() {
        ItemBuilder glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ");
        fillBorder(glass.build());

        setItem(0, MenuConfig.getInstance().item("worldrules.back", null), event ->
                new WorldRulesGui(plugin, player, rules).open());
        setItem(8, MenuConfig.getInstance().item("worldrules.reset", null), event -> {
            rules.resetWorld(world);
            refresh();
        });

        ConfigurationSection section = MenuConfig.getInstance().getYaml().getConfigurationSection("worldrules.rules");
        int slot = 10;
        for (String rule : WorldRulesManager.RULES) {
            boolean enabled = rules.getRule(world, rule);
            ConfigurationSection ruleSection = section != null ? section.getConfigurationSection(rule) : null;
            String name = ruleSection != null ? ruleSection.getString("name", rule) : rule;
            Material icon = ruleSection != null && ruleSection.getString("icon") != null
                    ? Material.matchMaterial(ruleSection.getString("icon")) : null;
            Material material = icon != null ? icon : Material.PAPER;
            List<String> lore = enabled
                    ? MenuConfig.getInstance().getYaml().getStringList("worldrules.on-lore")
                    : MenuConfig.getInstance().getYaml().getStringList("worldrules.off-lore");
            ItemStack item = new ItemBuilder(material)
                    .name(name)
                    .lore(String.join("\n", lore))
                    .build();
            setItem(slot, item, event -> {
                rules.setRule(world, rule, !enabled);
                refresh();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 36) break;
        }

        // botao de tempo (cicla: normal -> dia -> travado(dia) -> noite -> travado(noite) -> normal)
        // fica isolado na borda de baixo pra nunca colidir com a grade de regras acima.
        String timeMode = rules.getTimeMode(world);
        String timeName = MenuConfig.getInstance().getYaml().getString("worldrules.time." + timeMode + ".name",
                "Tempo");
        Material timeMaterial = Material.matchMaterial(
                MenuConfig.getInstance().getYaml().getString("worldrules.time.material", "CLOCK"));
        List<String> timeLore = MenuConfig.getInstance().getYaml().getStringList("worldrules.time.lore");
        ItemStack timeItem = new ItemBuilder(timeMaterial != null ? timeMaterial : Material.CLOCK)
                .name(timeName)
                .lore(String.join("\n", timeLore))
                .build();
        setItem(40, timeItem, event -> {
            rules.nextTimeMode(world);
            refresh();
        });
    }
}
