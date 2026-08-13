package com.alkacode.alkaessentials.util;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.api.MessageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Mensagens do AlkaEssentials. parse/send delegam pro {@link MessageProvider} do
 * AlkaCore (AlkaAPI#getMessages) - padroniza prefixo/formatacao com o resto do
 * studio. sendKey resolve a mensagem via {@link MessagesConfig} (messages.yml) -
 * forma preferida pra mensagens de chat editaveis sem recompilar.
 */
public final class ChatUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ChatUtil() {
    }

    public static Component parse(String message) {
        return provider().parse(message);
    }

    public static void send(CommandSender sender, String message) {
        provider().send(sender, message);
    }

    public static void sendKey(CommandSender sender, String key) {
        sendKey(sender, key, null);
    }

    public static void sendKey(CommandSender sender, String key, Map<String, String> placeholders) {
        MessagesConfig cfg = MessagesConfig.getInstance();
        if (cfg != null) {
            sender.sendMessage(cfg.getComponent(key, placeholders));
        } else {
            send(sender, "<red>[messages.yml nao carregado] " + key);
        }
    }

    private static MessageProvider provider() {
        return AlkaAPI.get().getMessages();
    }

    /** Converte MiniMessage para codigos legacy (&) - usado onde o texto (ex: nome de
     * home/warp via placar ou placeholder) vai pra algo que so entende legacy. */
    public static String toLegacy(String text) {
        if (text == null) {
            return "";
        }
        return LEGACY.serialize(MM.deserialize(text));
    }
}
