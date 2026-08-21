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
 *   %alkaessentials_genero%        -> "M"/"F" colorido (config tablist-visual.genero), vazio se nunca setado (/genero)
 *   %alkaessentials_mcmmo_colored% -> power level do mcMMO colorido por faixa (config tablist-visual.mcmmo-tiers), vazio sem mcMMO
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

    // ATENCAO - testado com programa Java isolado (nao confiar de olho na API):
    // strict(true) NAO serve aqui, rejeitaria nick com tag aberta ate o fim tipo
    // "<red>Nome" (padrao normal/valido). O bug de verdade e tag com argumento invalido
    // (ex: <gradient:NomeDeJogador:white>) virando texto literal sem lancar excecao
    // nenhuma. Deteccao real: deserializa (lenient) e confere se sobrou cara de tag no
    // texto plano - autocorrige nick ja corrompido antes desse fix (bug 21/08).
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
        if ("mcmmo_colored".equalsIgnoreCase(params)) {
            return mcmmoColored(player);
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
                if (!isValidMiniMessage(nick)) {
                    return player.getName();
                }
                return LEGACY.serialize(MiniMessage.miniMessage().deserialize(nick)) + "§r";
            case "nick_plain":
                return MiniMessage.miniMessage().stripTags(nick);
            case "realname":
                return player.getName();
            default:
                return null;
        }
    }

    /** Simbolo colorido pro TAB/chat - cor vem de tablist-visual.genero.<m|f> no
     * config.yml (qualquer tag MiniMessage: solida, hex ou gradiente). Vazio se o
     * jogador nunca setou (/genero ainda nao tem GUI, so comando). */
    private String generoSymbol(GenderManager.Gender gender) {
        if (gender == null) {
            return "";
        }
        String path = "tablist-visual.genero." + gender.name().toLowerCase();
        String defaultColor = gender == GenderManager.Gender.M ? "<blue>" : "<light_purple>";
        String color = plugin.getConfig().getString(path, defaultColor);
        return colorize(color + gender.name());
    }

    /** %mcmmo_power_level% (placeholder oficial da expansion do proprio mcMMO) colorido
     * por faixa - tablist-visual.mcmmo-tiers no config.yml, chave = nivel MINIMO da
     * faixa, valor = tag MiniMessage. Usa o maior nivel-minimo <= power level do
     * jogador. Vazio se o mcMMO/sua expansion de PAPI nao estiver instalado (o
     * PlaceholderAPI devolve o texto do placeholder sem resolver nesse caso). */
    private String mcmmoColored(Player player) {
        String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%mcmmo_power_level%");
        int level;
        try {
            level = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return "";
        }
        org.bukkit.configuration.ConfigurationSection tiers =
                plugin.getConfig().getConfigurationSection("tablist-visual.mcmmo-tiers");
        String color = "<white>";
        if (tiers != null) {
            int bestThreshold = Integer.MIN_VALUE;
            for (String key : tiers.getKeys(false)) {
                try {
                    int threshold = Integer.parseInt(key);
                    if (threshold <= level && threshold > bestThreshold) {
                        bestThreshold = threshold;
                        color = tiers.getString(key, color);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return colorize(color + level);
    }

    private String colorize(String miniMessage) {
        return LEGACY.serialize(MiniMessage.miniMessage().deserialize("<!i>" + miniMessage)) + "§r";
    }
}
