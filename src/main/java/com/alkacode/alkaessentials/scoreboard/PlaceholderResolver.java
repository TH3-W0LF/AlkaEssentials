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
        return PapiHook.parse(player, out);
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
