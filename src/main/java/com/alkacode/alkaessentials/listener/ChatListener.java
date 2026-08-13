package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.IgnoreManager;
import com.alkacode.alkaessentials.manager.NickManager;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Chat e social: formato com nick, filtro anti-swear/ad, @mencoes com som e /ignore. */
public final class ChatListener implements Listener {

    private final JavaPlugin plugin;
    private final NickManager nicks;
    private final IgnoreManager ignores;
    private final PunishmentManager punishments;
    private final com.alkacode.alkaessentials.hook.TabHook tabHook;
    private final boolean nChatPresent;

    public ChatListener(JavaPlugin plugin, NickManager nicks, IgnoreManager ignores, PunishmentManager punishments,
                        com.alkacode.alkaessentials.hook.TabHook tabHook) {
        this.plugin = plugin;
        this.nicks = nicks;
        this.ignores = ignores;
        this.punishments = punishments;
        this.tabHook = tabHook;
        this.nChatPresent = Bukkit.getPluginManager().getPlugin("nChat") != null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // reaplica o nick (display name) ao entrar
        String nick = nicks.getNick(event.getPlayer().getUniqueId());
        if (nick != null) {
            applyNick(event.getPlayer(), nick);
            tabHook.apply(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        // o nChat (premium) e dono do chat: cede a formatacao/reenvio pra ele
        if (nChatPresent) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        Player sender = event.getPlayer();
        if (punishments.hasActiveMute(sender.getUniqueId())) {
            return;
        }
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (isBlocked(text)) {
            event.setCancelled(true);
            ChatUtil.sendKey(sender, "chat-filtered");
            return;
        }

        event.setCancelled(true);
        String nick = nicks.getNick(sender.getUniqueId());
        String display = nick != null ? nick : sender.getName();

        // mensagem pro proprio remetente
        sender.sendMessage(buildLine(sender, display, text, sender));
        // mensagem para os demais (respeitando /ignore)
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.equals(sender)) {
                continue;
            }
            if (ignores.isIgnoring(recipient.getUniqueId(), sender.getUniqueId())) {
                continue;
            }
            recipient.sendMessage(buildLine(sender, display, text, recipient));
        }
    }

    private Component buildLine(Player sender, String nick, String text, Player recipient) {
        String format = plugin.getConfig().getString("chat.format",
                "<gray>{nick}<dark_gray>: <white>{message}");
        String messageText = applyMentions(text, recipient);
        String line = format
                .replace("{nick}", nick)
                .replace("{player}", sender.getName())
                .replace("{message}", messageText);
        return MiniMessage.miniMessage().deserialize(line);
    }

    /** Aplica destaque/som na mencao do proprio destinatario. */
    private String applyMentions(String text, Player recipient) {
        if (!plugin.getConfig().getBoolean("chat.mention.enabled", true)) {
            return text;
        }
        String mention = "@" + recipient.getName();
        if (containsIgnoreCase(text, mention)) {
            String highlight = plugin.getConfig().getString("chat.mention.highlight", "<gold><bold>");
            text = replaceIgnoreCase(text, mention, highlight + mention + "</bold></gold>");
            playMentionSound(recipient);
        }
        return text;
    }

    private void playMentionSound(Player player) {
        String soundName = plugin.getConfig().getString("chat.mention.sound", "BLOCK_NOTE_BLOCK_PLING");
        player.playSound(player.getLocation(), soundName, SoundCategory.MASTER, 1.0f, 1.0f);
    }

    private boolean isBlocked(String text) {
        if (!plugin.getConfig().getBoolean("chat.filter.enabled", true)) {
            return false;
        }
        String lower = text.toLowerCase();
        List<String> patterns = plugin.getConfig().getStringList("chat.filter.blocked-patterns");
        for (String pattern : patterns) {
            if (pattern != null && !pattern.isBlank() && lower.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        if (plugin.getConfig().getBoolean("chat.filter.block-links", true)) {
            String linkPattern = plugin.getConfig().getString("chat.filter.link-pattern", "(https?://|www\\.)\\S+");
            if (lower.matches(".*" + linkPattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    private void applyNick(Player player, String nick) {
        player.displayName(MiniMessage.miniMessage().deserialize(nick));
        player.customName(MiniMessage.miniMessage().deserialize(nick));
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private static String replaceIgnoreCase(String text, String target, String replacement) {
        return text.replaceAll("(?i)" + java.util.regex.Pattern.quote(target),
                java.util.regex.Matcher.quoteReplacement(replacement));
    }
}
