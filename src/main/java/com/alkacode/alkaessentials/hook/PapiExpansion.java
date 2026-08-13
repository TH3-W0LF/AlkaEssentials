package com.alkacode.alkaessentials.hook;

import com.alkacode.alkaessentials.manager.NickManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Expansao PlaceholderAPI do AlkaEssentials. Expoe:
 *   %alkaessentials_nick%        -> nick do jogador (codigos legado &, para nChat/TAB)
 *   %alkaessentials_nick_plain%  -> nick sem cores
 *   %alkaessentials_realname%    -> nome real
 * Registrada apenas quando o PlaceholderAPI esta instalado.
 */
public final class PapiExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final NickManager nicks;

    public PapiExpansion(JavaPlugin plugin, NickManager nicks) {
        this.plugin = plugin;
        this.nicks = nicks;
    }

    @Override
    public String getIdentifier() {
        return "alkaessentials";
    }

    @Override
    public String getAuthor() {
        return "AlkaStudio";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        String nick = nicks.getNick(player.getUniqueId());
        if (nick == null) {
            return "nick".equalsIgnoreCase(params) || "nick_plain".equalsIgnoreCase(params)
                    ? player.getName() : null;
        }
        switch (params.toLowerCase()) {
            case "nick":
            case "nickname":
                // reset final (&r) para cor/estilo do nick nao vazarem pro suffix
                return LegacyComponentSerializer.legacyAmpersand()
                        .serialize(MiniMessage.miniMessage().deserialize(nick)) + "&r";
            case "nick_plain":
                return MiniMessage.miniMessage().stripTags(nick);
            case "realname":
                return player.getName();
            default:
                return null;
        }
    }
}
