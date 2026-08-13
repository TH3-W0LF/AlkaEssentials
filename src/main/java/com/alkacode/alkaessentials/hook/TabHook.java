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
        try {
            return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(nick)) + "§r";
        } catch (Exception e) {
            return player.getName();
        }
    }

    public boolean isPresent() {
        return present;
    }
}
