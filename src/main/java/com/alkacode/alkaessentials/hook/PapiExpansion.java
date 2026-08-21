package com.alkacode.alkaessentials.hook;

import com.alkacode.alkaessentials.manager.GenderManager;
import com.alkacode.alkaessentials.manager.NickManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Expansao PlaceholderAPI do AlkaEssentials. Expoe:
 *   %alkaessentials_nick%        -> nick do jogador (codigos legado § reais, para nChat/TAB/placar)
 *   %alkaessentials_nick_plain%  -> nick sem cores
 *   %alkaessentials_realname%    -> nome real
 *   %alkaessentials_genero%      -> "M"/"F" colorido (§9M/§dF), vazio se nunca setado (/genero)
 * Registrada apenas quando o PlaceholderAPI esta instalado.
 */
public final class PapiExpansion extends PlaceholderExpansion {

    // character(SECTION_CHAR) (nao legacyAmpersand()) + useUnusualXRepeatedCharacterHexFormat():
    // nosso proprio placar (PlaceholderResolver) so reconhece codigo real "§", nunca texto "&"
    // cru - texto "&" ja quebrou o placar de verdade uma vez (nome de mina com gradient no
    // AlkaMines, corrigido v1.0.83) - mesma classe de bug aqui, so que auto-infligida.
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final JavaPlugin plugin;
    private final NickManager nicks;
    private final GenderManager genders;

    public PapiExpansion(JavaPlugin plugin, NickManager nicks, GenderManager genders) {
        this.plugin = plugin;
        this.nicks = nicks;
        this.genders = genders;
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
        if ("genero".equalsIgnoreCase(params)) {
            return generoSymbol(genders.get(player.getUniqueId()));
        }

        String nick = nicks.getNick(player.getUniqueId());
        if (nick == null) {
            return "nick".equalsIgnoreCase(params) || "nick_plain".equalsIgnoreCase(params)
                    ? player.getName() : null;
        }
        switch (params.toLowerCase()) {
            case "nick":
            case "nickname":
                // reset final (§r) para cor/estilo do nick nao vazarem pro suffix
                return LEGACY.serialize(MiniMessage.miniMessage().deserialize(nick)) + "§r";
            case "nick_plain":
                return MiniMessage.miniMessage().stripTags(nick);
            case "realname":
                return player.getName();
            default:
                return null;
        }
    }

    /** Simbolo colorido pro TAB/chat - M azul, F rosa (recomendado 21/08, aprovado pelo
     * usuario). Vazio se o jogador nunca setou (/genero ainda nao tem GUI, so comando). */
    private String generoSymbol(GenderManager.Gender gender) {
        if (gender == null) {
            return "";
        }
        return switch (gender) {
            case M -> "§9M§r";
            case F -> "§dF§r";
        };
    }
}
