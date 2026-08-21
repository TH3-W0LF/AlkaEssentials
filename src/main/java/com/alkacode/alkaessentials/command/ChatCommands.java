package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.manager.GenderManager;
import com.alkacode.alkaessentials.manager.IgnoreManager;
import com.alkacode.alkaessentials.manager.NickManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Comandos de chat/social: /nick, /realname, /ignore, /clearchat, /broadcast, /discord, /site, /loja, /regras. */
public final class ChatCommands extends BaseCommand {

    private final NickManager nicks;
    private final IgnoreManager ignores;
    private final GenderManager genders;
    private final com.alkacode.alkaessentials.hook.TabHook tabHook;

    public ChatCommands(JavaPlugin plugin, NickManager nicks, IgnoreManager ignores,
                        GenderManager genders, com.alkacode.alkaessentials.hook.TabHook tabHook) {
        super(plugin);
        this.nicks = nicks;
        this.ignores = ignores;
        this.genders = genders;
        this.tabHook = tabHook;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "nick": return nick(sender, args);
            case "color": return color(sender, args);
            case "namecolor": return nameColor(sender, args);
            case "gradient": return gradient(sender, args);
            case "genero": return genero(sender, args);
            case "realname":
            case "whois": return realname(sender, args);
            case "ignore": return ignore(sender, args);
            case "clearchat": return clearchat(sender);
            case "broadcast": return broadcast(sender, args);
            case "discord": return info(sender, "discord-msg");
            case "site": return info(sender, "site-msg");
            case "loja": return info(sender, "loja-msg");
            case "regras": return info(sender, "regras-msg");
            default: return false;
        }
    }

    private boolean nick(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        if (!plugin.getConfig().getBoolean("chat.nick.enabled", true)) {
            ChatUtil.sendKey(player, "nick-disabled");
            return true;
        }
        String permission = plugin.getConfig().getString("chat.nick.permission", "alkassentials.chat.nick");
        if (!requirePerm(player, permission)) return true;

        if (args.length == 0 || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("clear")) {
            nicks.clearNick(player.getUniqueId());
            player.displayName(player.name());
            player.customName(player.name());
            tabHook.clear(player);
            ChatUtil.sendKey(player, "nick-cleared");
            return true;
        }

        String nick = args[0];
        boolean allowColors = plugin.getConfig().getBoolean("chat.nick.allow-colors", true);
        String plain = MiniMessage.miniMessage().stripTags(nick);
        int maxLength = plugin.getConfig().getInt("chat.nick.max-length", 16);
        if (plain.length() > maxLength) {
            ChatUtil.sendKey(player, "nick-invalid", Map.of("max", String.valueOf(maxLength)));
            return true;
        }
        for (String blocked : plugin.getConfig().getStringList("chat.nick.blocked-nicks")) {
            if (plain.toLowerCase().contains(blocked.toLowerCase())) {
                ChatUtil.sendKey(player, "nick-blocked");
                return true;
            }
        }
        if (allowColors) {
            // garante que o nick e um MiniMessage valido antes de salvar
            try {
                MiniMessage.miniMessage().deserialize(nick);
            } catch (Exception e) {
                ChatUtil.sendKey(player, "nick-invalid", Map.of("max", String.valueOf(maxLength)));
                return true;
            }
        } else {
            nick = plain;
        }
        nicks.setNick(player.getUniqueId(), nick);
        player.displayName(MiniMessage.miniMessage().deserialize(nick));
        player.customName(MiniMessage.miniMessage().deserialize(nick));
        tabHook.apply(player);
        ChatUtil.sendKey(player, "nick-set", Map.of("nick", plain));
        return true;
    }

    /** /color <nickColorido>: so muda a cor do proprio nick. O texto digitado deve bater com o nick atual. */
    private boolean color(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        if (!plugin.getConfig().getBoolean("chat.color.enabled", true)) {
            ChatUtil.sendKey(player, "color-disabled");
            return true;
        }
        String permission = plugin.getConfig().getString("chat.color.permission", "alkassentials.chat.color");
        if (!requirePerm(player, permission)) return true;
        if (args.length < 1) return false;

        String input = String.join(" ", args);
        String mini;
        String plain;
        if (input.contains("<")) {
            // ja e MiniMessage
            mini = input;
            plain = MiniMessage.miniMessage().stripTags(input).trim();
        } else {
            // cor legado (&4Nick) -> MiniMessage
            Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(input);
            mini = MiniMessage.miniMessage().serialize(comp);
            plain = MiniMessage.miniMessage().stripTags(mini).trim();
        }

        // o texto deve ser o nick atual do proprio jogador (ou o nome real, se sem nick)
        String currentPlain = nicks.hasNick(player.getUniqueId())
                ? MiniMessage.miniMessage().stripTags(nicks.getNick(player.getUniqueId())).trim()
                : player.getName();
        if (!plain.equalsIgnoreCase(currentPlain)) {
            ChatUtil.sendKey(player, "color-not-your-nick", Map.of("nick", plain));
            return true;
        }
        try {
            MiniMessage.miniMessage().deserialize(mini);
        } catch (Exception e) {
            ChatUtil.sendKey(player, "nick-invalid", Map.of("max", String.valueOf(plugin.getConfig().getInt("chat.nick.max-length", 16))));
            return true;
        }
        nicks.setNick(player.getUniqueId(), mini);
        player.displayName(MiniMessage.miniMessage().deserialize(mini));
        player.customName(MiniMessage.miniMessage().deserialize(mini));
        tabHook.apply(player);
        ChatUtil.sendKey(player, "nick-set", Map.of("nick", plain));
        return true;
    }

    /** /namecolor - abre a GUI de cor/estilo do nick. */
    private boolean nameColor(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        String permission = plugin.getConfig().getString("chat.color.permission", "alkassentials.chat.color");
        if (!requirePerm(player, permission)) return true;
        new com.alkacode.alkaessentials.gui.NameColorGui(plugin, player, nicks, tabHook).open();
        return true;
    }

    /** /gradient <cor1> [cor2] - aplica gradiente ao proprio nick. */
    private boolean gradient(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        String permission = plugin.getConfig().getString("chat.color.permission", "alkassentials.chat.color");
        if (!requirePerm(player, permission)) return true;
        if (args.length < 1) return false;
        String c1 = args[0];
        String c2 = args.length > 1 ? args[1] : "white";
        String prefix = "<gradient:" + c1 + ":" + c2 + ">";
        nicks.applyColorToNick(player, prefix);
        tabHook.apply(player);
        ChatUtil.sendKey(player, "gradient-applied");
        return true;
    }

    /** /genero <M|F> - so o comando por enquanto (sem GUI, ver [[project-alkaflair]] pedido
     * do usuario 21/08 - GUI fica pra depois). Aplica na hora no TAB via tabHook.apply(). */
    private boolean genero(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        if (args.length < 1) return false;
        GenderManager.Gender gender;
        try {
            gender = GenderManager.Gender.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            ChatUtil.sendKey(player, "genero-invalido");
            return true;
        }
        genders.set(player.getUniqueId(), gender);
        tabHook.apply(player);
        ChatUtil.sendKey(player, "genero-definido", Map.of("genero", gender.name()));
        return true;
    }

    private boolean realname(CommandSender sender, String[] args) {
        if (!requirePerm(sender, "alkassentials.chat.realname")) return true;
        if (args.length < 1) return false;
        UUID uuid = nicks.findRealName(args[0]);
        if (uuid == null) {
            ChatUtil.sendKey(sender, "realname-none");
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName() != null ? offline.getName() : uuid.toString();
        ChatUtil.sendKey(sender, "realname-result", Map.of("nick", args[0], "player", name));
        return true;
    }

    private boolean ignore(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        if (!requirePerm(player, "alkassentials.chat.ignore")) return true;
        if (args.length < 1) return false;
        Player target = matchPlayer(args[0]);
        if (target == null) {
            ChatUtil.sendKey(player, "invalid-player", Map.of("player", args[0]));
            return true;
        }
        if (target.equals(player)) return true;
        boolean ignoring = ignores.toggle(player.getUniqueId(), target.getUniqueId());
        ChatUtil.sendKey(player, ignoring ? "ignore-on" : "ignore-off", Map.of("player", target.getName()));
        return true;
    }

    private boolean clearchat(CommandSender sender) {
        if (!requirePerm(sender, "alkassentials.chat.clearchat")) return true;
        Component empty = Component.empty();
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 150; i++) {
                online.sendMessage(empty);
            }
        }
        ChatUtil.sendKey(sender, "chat-cleared");
        return true;
    }

    private boolean broadcast(CommandSender sender, String[] args) {
        if (!requirePerm(sender, "alkassentials.chat.broadcast")) return true;
        if (args.length < 1) return false;
        String message = String.join(" ", args);
        String format = plugin.getConfig().getString("chat.broadcast-format", "<red>[AVISO] <white>{message}");
        String line = format.replace("{message}", message).replace("{player}", sender.getName());
        Component comp = MiniMessage.miniMessage().deserialize(line);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(comp);
        }
        plugin.getServer().getConsoleSender().sendMessage(comp);
        return true;
    }

    private boolean info(CommandSender sender, String key) {
        ChatUtil.sendKey(sender, key);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ((command.getName().equalsIgnoreCase("ignore") || command.getName().equalsIgnoreCase("realname"))
                && args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
