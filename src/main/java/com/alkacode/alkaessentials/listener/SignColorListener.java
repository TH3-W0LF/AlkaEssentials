package com.alkacode.alkaessentials.listener;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.regex.Pattern;

/** Cores em placas: quem tem permissao pode usar codigos & nas placas. */
public final class SignColorListener implements Listener {

    private static final Pattern AMPERSAND = Pattern.compile("(?i)&[0-9a-fk-orx]");

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!event.getPlayer().hasPermission("alkassentials.signs.colors")) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            String plain = PlainTextComponentSerializer.plainText().serialize(event.line(i));
            if (plain.contains("&") && AMPERSAND.matcher(plain).find()) {
                event.line(i, LegacyComponentSerializer.legacySection().deserialize(translate(plain)));
            }
        }
    }

    private String translate(String line) {
        char[] chars = line.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length && "0123456789abcdefklmnorx".indexOf(chars[i + 1]) >= 0) {
                sb.append('\u00A7').append(chars[i + 1]);
                i++;
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }
}
