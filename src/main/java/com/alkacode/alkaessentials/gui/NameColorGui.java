package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.manager.NickManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
import com.alkacode.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

/** GUI de cor do nick (/namecolor): escolhe cor e estilo, aplica no proprio nick. */
public final class NameColorGui extends BaseGui {

    private static final String[][] COLORS = {
            {"red", "<red>", "RED_DYE"}, {"dark_red", "<dark_red>", "BROWN_DYE"},
            {"gold", "<gold>", "ORANGE_DYE"}, {"yellow", "<yellow>", "YELLOW_DYE"},
            {"green", "<green>", "LIME_DYE"}, {"dark_green", "<dark_green>", "GREEN_DYE"},
            {"aqua", "<aqua>", "LIGHT_BLUE_DYE"}, {"dark_aqua", "<dark_aqua>", "CYAN_DYE"},
            {"blue", "<blue>", "BLUE_DYE"}, {"dark_blue", "<dark_blue>", "BLUE_DYE"},
            {"light_purple", "<light_purple>", "MAGENTA_DYE"}, {"dark_purple", "<dark_purple>", "PURPLE_DYE"},
            {"gray", "<gray>", "LIGHT_GRAY_DYE"}, {"dark_gray", "<dark_gray>", "GRAY_DYE"},
            {"white", "<white>", "WHITE_DYE"}, {"black", "<black>", "BLACK_DYE"}
    };

    private static final String[][] STYLES = {
            {"bold", "<bold>"}, {"italic", "<italic>"}, {"underline", "<underline>"},
            {"strikethrough", "<strikethrough>"}, {"obfuscated", "<obfuscated>"}
    };

    private final NickManager nicks;
    private final com.alkacode.alkaessentials.hook.TabHook tabHook;
    private String selectedColor = "<white>";
    private final Set<String> activeStyles = new HashSet<>();

    public NameColorGui(JavaPlugin plugin, Player player, NickManager nicks,
                        com.alkacode.alkaessentials.hook.TabHook tabHook) {
        super(plugin, player, "<gold>Cor do Nick", 5, "alkaessentials_namecolor");
        this.nicks = nicks;
        this.tabHook = tabHook;
    }

    @Override
    public void render() {
        ItemBuilder glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ");
        fillBorder(glass.build());

        // preview do nick
        String currentPlain = nicks.hasNick(player.getUniqueId())
                ? net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .stripTags(nicks.getNick(player.getUniqueId())).trim()
                : player.getName();
        String preview = selectedColor + String.join("", activeStyles) + currentPlain;
        setItem(4, new ItemBuilder(Material.NAME_TAG).name(preview).build());

        int slot = 10;
        for (String[] color : COLORS) {
            Material material = Material.matchMaterial(color[2]);
            boolean isSelected = selectedColor.equals(color[1]);
            ItemStack item = new ItemBuilder(material != null ? material : Material.PAPER)
                    .name(color[1] + color[0] + (isSelected ? " <green>✓" : ""))
                    .build();
            setItem(slot, item, event -> {
                selectedColor = color[1];
                apply();
                refresh();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // estilos
        for (String[] style : STYLES) {
            boolean active = activeStyles.contains(style[1]);
            ItemStack item = new ItemBuilder(Material.BOOK)
                    .name(style[1] + style[0] + (active ? " <green>✓" : ""))
                    .build();
            setItem(slot, item, event -> {
                if (!activeStyles.add(style[1])) {
                    activeStyles.remove(style[1]);
                }
                apply();
                refresh();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // limpar cor (e estilos)
        setItem(7, new ItemBuilder(Material.WHITE_DYE).name("<white>Resetar Estilo")
                .lore("<gray>Remove bold/italic/underline/etc. do seu nick").build(), event -> {
            activeStyles.clear();
            apply();
            refresh();
        });
        setItem(8, new ItemBuilder(Material.BARRIER).name("<red>Limpar cor")
                .lore("<gray>Remove a cor e o estilo do seu nick").build(), event -> {
            selectedColor = "";
            activeStyles.clear();
            apply();
            refresh();
        });
    }

    private void apply() {
        String prefix = selectedColor + String.join("", activeStyles);
        nicks.applyColorToNick(player, prefix);
        tabHook.apply(player);
        ChatUtil.sendKey(player, "namecolor-applied");
    }
}
