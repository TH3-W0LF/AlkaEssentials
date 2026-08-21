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

    // strict(true) SO pra validar - MiniMessage.miniMessage() normal e permissivo (tag
    // com argumento invalido nao lanca excecao), entao um nick ja salvo malformado (ver
    // ChatCommands#gradient, agora validado no save) continuava vazando o texto cru
    // pro nChat/placar pra sempre - bug 21/08, print do usuario. Autocorrige aqui.
    private static final MiniMessage STRICT = MiniMessage.builder().strict(true).build();

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
                try {
                    STRICT.deserialize(nick);
                    return LEGACY.serialize(MiniMessage.miniMessage().deserialize(nick)) + "§r";
                } catch (Exception e) {
                    return player.getName();
                }
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
