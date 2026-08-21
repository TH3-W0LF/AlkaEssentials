package com.alkacode.alkaessentials.hook;

import com.alkacode.alkaessentials.manager.NickManager;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.placeholder.PlayerPlaceholder;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Integracao com o TAB (NEZNAMY): aplica o nick no tab list (via API) e registra o
 * placeholder %alkaessentials_nick% para uso no config do TAB (nametag/tablist).
 * Roda so quando o TAB esta presente; silenciosamente ignora se nao.
 */
public final class TabHook {

    // ATENCAO - testado com programa Java isolado antes de escrever (nao confiar de
    // olho na API). strict(true) NAO serve aqui (rejeitaria nick com tag aberta ate o
    // fim tipo "<red>Nome", padrao normal/valido). O bug de verdade e outro: tag com
    // argumento invalido (ex: <gradient:NomeDeJogador:white> - "NomeDeJogador" nao e
    // cor) faz o parser tratar a coisa toda como texto literal desde o inicio, sem
    // lancar excecao nenhuma. Deteccao real: deserializa (lenient) e confere se sobrou
    // alguma coisa com cara de tag no texto plano. Ver ChatCommands#isValidMiniMessage
    // (mesmo padrao, duplicado aqui - plugins diferentes, sem classe compartilhada).
    private static final java.util.regex.Pattern LEAKED_TAG =
            java.util.regex.Pattern.compile("<[a-zA-Z_][a-zA-Z0-9_]*(:[^<>]*)?>");

    private static boolean isValidMiniMessage(String input) {
        net.kyori.adventure.text.Component parsed;
        try {
            parsed = MiniMessage.miniMessage().deserialize(input);
        } catch (Exception e) {
            return false;
        }
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(parsed);
        return !LEAKED_TAG.matcher(plain).find();
    }

    private final NickManager nicks;
    private final boolean present;
    private PlayerPlaceholder nickPlaceholder;

    public TabHook(NickManager nicks) {
        this.nicks = nicks;
        this.present = Bukkit.getPluginManager().getPlugin("TAB") != null;
        if (present) {
            try {
                nickPlaceholder = TabAPI.getInstance().getPlaceholderManager()
                        .registerPlayerPlaceholder("alkaessentials_nick", 1000, tp -> nickFor(Bukkit.getPlayer(tp.getUniqueId())));
            } catch (Throwable ignored) {
                // TAB presente mas API indisponivel
            }
        }
    }

    /** Aplica o nick (ou nome) no tab list do jogador. */
    public void apply(Player player) {
        if (!present || player == null) {
            return;
        }
        try {
            TabPlayer tp = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if (tp != null) {
                TabListFormatManager manager = TabAPI.getInstance().getTabListFormatManager();
                manager.setName(tp, legacyNick(player));
            }
        } catch (Throwable ignored) {
        }
    }

    /** Reseta o nome do tab list para o original (quando o nick e removido). */
    public void clear(Player player) {
        if (!present || player == null) {
            return;
        }
        try {
            TabPlayer tp = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if (tp != null) {
                TabAPI.getInstance().getTabListFormatManager().setName(tp, player.getName());
            }
        } catch (Throwable ignored) {
        }
    }

    private String nickFor(Player player) {
        if (player == null) {
            return "";
        }
        return legacyNick(player);
    }

    /** Nick renderizado em codigos legado (&/§) - o que o TAB entende. O reset final evita que cor/estilo vazem pro suffix. */
    private String legacyNick(Player player) {
        String nick = nicks.getNick(player.getUniqueId());
        if (nick == null) {
            return player.getName();
        }
        if (!isValidMiniMessage(nick)) {
            return player.getName();
        }
        return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(nick)) + "§r";
    }

    public boolean isPresent() {
        return present;
    }
}
