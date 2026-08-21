package com.alkacode.alkaessentials.gui;

import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.gui.layout.GuiLayoutLoader;
import com.alkacode.alkaessentials.manager.NickManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.gui.BaseGui;
import com.alkacode.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** GUI de cor do nick (/namecolor): escolhe cor e estilo, aplica no proprio nick.
 * Paleta de cores/estilos (id/tag MiniMessage/material) vem de menus.yml.namecolor. */
public final class NameColorGui extends BaseGui {

    private record ColorOption(String id, String tag, String material) {}
    private record StyleOption(String id, String tag) {}

    private final NickManager nicks;
    private final com.alkacode.alkaessentials.hook.TabHook tabHook;
    private String selectedColor = "<white>";
    private final Set<String> activeStyles = new HashSet<>();

    public NameColorGui(JavaPlugin plugin, Player player, NickManager nicks,
                        com.alkacode.alkaessentials.hook.TabHook tabHook) {
        super(plugin, player, MenuConfig.getInstance().title("namecolor.title", null), 5, "alkaessentials_namecolor");
        this.nicks = nicks;
        this.tabHook = tabHook;
    }

    private static List<ColorOption> colors() {
        YamlConfiguration yaml = MenuConfig.getInstance().getYaml();
        List<ColorOption> list = new java.util.ArrayList<>();
        for (Map<?, ?> entry : yaml.getMapList("namecolor.colors")) {
            list.add(new ColorOption(String.valueOf(entry.get("id")), String.valueOf(entry.get("tag")),
                    String.valueOf(entry.get("material"))));
        }
        return list;
    }

    private static List<StyleOption> styles() {
        YamlConfiguration yaml = MenuConfig.getInstance().getYaml();
        List<StyleOption> list = new java.util.ArrayList<>();
        for (Map<?, ?> entry : yaml.getMapList("namecolor.styles")) {
            list.add(new StyleOption(String.valueOf(entry.get("id")), String.valueOf(entry.get("tag"))));
        }
        return list;
    }

    @Override
    public void render() {
        fillBorder(MenuConfig.getInstance().item("namecolor.glass", null));

        GuiLayoutLoader.GuiLayout layout = GuiLayoutLoader.getInstance().getLayout("alkaessentials_namecolor");

        String currentPlain = nicks.hasNick(player.getUniqueId())
                ? net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .stripTags(nicks.getNick(player.getUniqueId())).trim()
                : player.getName();
        String preview = selectedColor + String.join("", activeStyles) + currentPlain;
        ItemStack previewItem = MenuConfig.getInstance().item("namecolor.preview", null);
        ItemMeta previewMeta = previewItem.getItemMeta();
        previewMeta.displayName(ChatUtil.parse(preview));
        previewItem.setItemMeta(previewMeta);
        setItem(layout.firstSlot('V'), previewItem);

        List<Integer> colorSlots = layout.findSlots('0');
        List<ColorOption> colors = colors();
        for (int i = 0; i < colorSlots.size() && i < colors.size(); i++) {
            ColorOption color = colors.get(i);
            Material material = Material.matchMaterial(color.material());
            boolean isSelected = selectedColor.equals(color.tag());
            ItemStack item = new ItemBuilder(material != null ? material : Material.PAPER)
                    .name(color.tag() + color.id() + (isSelected ? " <green>✓" : ""))
                    .build();
            setItem(colorSlots.get(i), item, event -> {
                selectedColor = color.tag();
                apply();
                refresh();
            });
        }

        List<Integer> styleSlots = layout.findSlots('1');
        Material styleMaterial = Material.matchMaterial(
                MenuConfig.getInstance().getYaml().getString("namecolor.style-material", "BOOK"));
        List<StyleOption> styles = styles();
        for (int i = 0; i < styleSlots.size() && i < styles.size(); i++) {
            StyleOption style = styles.get(i);
            boolean active = activeStyles.contains(style.tag());
            ItemStack item = new ItemBuilder(styleMaterial != null ? styleMaterial : Material.BOOK)
                    .name(style.tag() + style.id() + (active ? " <green>✓" : ""))
                    .build();
            setItem(styleSlots.get(i), item, event -> {
                if (!activeStyles.add(style.tag())) {
                    activeStyles.remove(style.tag());
                }
                apply();
                refresh();
            });
        }

        setItem(layout.firstSlot('R'), MenuConfig.getInstance().item("namecolor.reset-style", null), event -> {
            activeStyles.clear();
            apply();
            refresh();
        });
        setItem(layout.firstSlot('L'), MenuConfig.getInstance().item("namecolor.clear-color", null), event -> {
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
