package com.alkacode.alkaessentials.scoreboard;

import com.alkacode.alkaessentials.afk.AfkManager;
import com.alkacode.alkaessentials.manager.HomeManager;
import com.alkacode.alkaessentials.manager.NickManager;
import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.hooks.PapiHook;
import com.alkacode.core.util.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Map;

/**
 * Resolve os placeholders das scoreboards/tablists: placeholders proprios da rede
 * (%alka_*) sem depender de PlaceholderAPI + ponte PAPI quando o plugin estiver
 * instalado (via {@link PapiHook} do AlkaCore).
 */
public final class PlaceholderResolver {

    private final JavaPlugin plugin;
    private final AfkManager afkManager;
    private final HomeManager homes;
    private final NickManager nicks;
    private final long startedAt;

    public PlaceholderResolver(JavaPlugin plugin, AfkManager afkManager, HomeManager homes, NickManager nicks) {
        this.plugin = plugin;
        this.afkManager = afkManager;
        this.homes = homes;
        this.nicks = nicks;
        this.startedAt = System.currentTimeMillis();
    }

    public String resolve(Player player, String text) {
        String display = player.displayName() != null
                ? PlainTextComponentSerializer.plainText().serialize(player.displayName())
                : player.getName();
        String nick = nicks.getNick(player.getUniqueId());
        String nickPlain = nick != null
                ? MiniMessage.miniMessage().stripTags(nick)
                : player.getName();
        String out = text
                .replace("{player}", player.getName())
                .replace("{nick}", nickPlain)
                .replace("{display}", display);
        out = resolveAlka(player, out);
        out = PapiHook.parse(player, out);
        // A convencao do PAPI (fora do ecossistema Alka) e devolver codigo de cor legacy
        // (§), nao MiniMessage - e alguns hooks proprios (ex: AlkaRankUp#rank_tag,
        // AlkaFlair#tag/tag_prefix/tag_suffix/medals) tambem devolvem legacy de proposito,
        // pra consumidores como TAB/nChat. O MiniMessage (Adventure 4.24+) trata legacy
        // misturado numa string como ERRO FATAL de parse, nao mais como texto ignorado -
        // sem essa conversao, QUALQUER placeholder (Alka ou de terceiro, ex: LuckPerms/
        // mcMMO) que devolva legacy quebra a scoreboard inteira. Ver ScoreboardManager -
        // todo texto que passa por resolve() acaba em MiniMessage.deserialize().
        //
        // Camada extra: nem todo plugin de terceiro segue a convencao "§" - alguns PAPI
        // expansions fora do nosso controle devolvem '&' cru (padrao mais antigo/comum no
        // ecossistema Spigot em geral). translateAlternateColorCodes so converte '&' quando
        // seguido de um char de codigo valido (0-9a-fk-or ou 'x'+6 pares hex) - texto comum
        // com '&' solto (ex: "Tom & Jerry") nao e afetado. Depois disso ja e tudo '§' real,
        // caindo no mesmo pipeline de baixo.
        out = org.bukkit.ChatColor.translateAlternateColorCodes('&', out);
        return legacyToMiniMessage(out);
    }

    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"), Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"), Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"), Map.entry('7', "<gray>"), Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"), Map.entry('a', "<green>"), Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"), Map.entry('d', "<light_purple>"), Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"), Map.entry('k', "<obfuscated>"), Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"), Map.entry('n', "<underlined>"), Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>"));

    /** Converte codigo de cor legacy ('§' + char) pra tag MiniMessage equivalente - nao mexe
     * em nenhuma tag MiniMessage ja presente na string (alfabetos distintos, sem ambiguidade).
     * Trata tambem cor RGB legacy ('§x' + 6x '§<hex>', formato que LegacyComponentSerializer
     * usa pra aproximar gradient/cor hex - sem isso um nome de mina/rank com <gradient:...>
     * vira uma sequencia de '§x§_§_§_§_§_§_' que sobra sem converter e quebra o parse do
     * MiniMessage de novo, so que por causa do gradiente em vez do codigo classico). */
    private static String legacyToMiniMessage(String input) {
        if (input == null || input.indexOf('§') < 0) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '§' && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                if (code == 'x') {
                    String hex = readLegacyHex(input, i);
                    if (hex != null) {
                        out.append('<').append('#').append(hex).append('>');
                        i += 14; // '§x' + 6x '§<hex>' = 2 + 12 caracteres
                        continue;
                    }
                    i += 2; // '§x' malformado (sem os 6 pares seguintes) - so pula, nao propaga o '§'
                    continue;
                }
                String tag = LEGACY_TAGS.get(code);
                if (tag != null) {
                    out.append(tag);
                    i += 2;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** Le os 6 digits hex de um '§x§R§R§G§G§B§B' a partir do indice do '§' inicial (posicao de
     * 'x') - devolve "RRGGBB" ou null se nao houver os 6 pares '§<hex>' completos ali. */
    private static String readLegacyHex(String input, int xIndex) {
        StringBuilder hex = new StringBuilder(6);
        int pos = xIndex + 2;
        for (int n = 0; n < 6; n++) {
            if (pos + 1 >= input.length() || input.charAt(pos) != '§') {
                return null;
            }
            char digit = Character.toLowerCase(input.charAt(pos + 1));
            if (Character.digit(digit, 16) < 0) {
                return null;
            }
            hex.append(digit);
            pos += 2;
        }
        return hex.toString();
    }

    private String resolveAlka(Player player, String text) {
        String out = text;
        // %alka_currency_<id>% => saldo secundario do AlkaCore (opt-in; faz leitura no banco)
        java.util.regex.Matcher currency = java.util.regex.Pattern.compile("%alka_currency_([a-zA-Z0-9_]+)%")
                .matcher(out);
        StringBuffer sb = new StringBuffer();
        while (currency.find()) {
            int balance = AlkaAPI.get().getCurrency().getBalance(player.getUniqueId(), currency.group(1));
            currency.appendReplacement(sb, String.valueOf(balance));
        }
        currency.appendTail(sb);
        out = sb.toString();

        out = out.replace("%alka_online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%alka_max_online%", String.valueOf(plugin.getServer().getMaxPlayers()))
                .replace("%alka_ping%", String.valueOf(player.getPing()))
                .replace("%alka_world%", player.getWorld().getName())
                .replace("%alka_afk%", afkManager.isAfk(player.getUniqueId()) ? "Sim" : "Nao")
                .replace("%alka_homes%", String.valueOf(homes.getHomes(player.getUniqueId()).size()))
                .replace("%alka_tps%", formatTps())
                .replace("%alka_uptime%", TimeUtil.formatSeconds((System.currentTimeMillis() - startedAt) / 1000))
                .replace("%alka_time%", java.time.LocalTime.now().withNano(0).toString());
        return out;
    }

    private String formatTps() {
        double tps = Bukkit.getTPS()[0];
        return String.format(Locale.US, "%.1f", Math.min(20.0, tps));
    }
}
