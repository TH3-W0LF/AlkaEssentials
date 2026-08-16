package com.alkacode.alkaessentials.scoreboard;

import com.alkacode.alkaessentials.afk.AfkManager;
import com.alkacode.alkaessentials.hook.WorldGuardHook;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Motor de scoreboard/tablist do AlkaEssentials. A cada tick escolhe a scoreboard
 * contextual (mundo/permissao/prioridade, com painel AFK especial), monta titulo e
 * linhas animadas (frames + intervalo), aplica no jogador via API moderna de Score
 * (customName, sem limite de 16 chars) e envia a tablist. Tudo MiniMessage.
 */
public final class ScoreboardManager {

    private static final String OBJECTIVE = "alka_score";

    private final JavaPlugin plugin;
    private final ScoreboardConfig config;
    private final PlaceholderResolver resolver;
    private final AfkManager afkManager;
    private final WorldGuardHook worldGuard;

    private final Map<UUID, PlayerBoard> boards = new HashMap<>();
    private final Map<UUID, Component> nameOrigins = new HashMap<>();
    private final Set<UUID> toggledOff = new HashSet<>();

    private static final class PlayerBoard {
        final org.bukkit.scoreboard.Scoreboard board;
        Objective objective;
        long startedAt = System.currentTimeMillis();
        final List<String> entries = new ArrayList<>();
        PlayerBoard(org.bukkit.scoreboard.Scoreboard board) {
            this.board = board;
        }
    }

    public ScoreboardManager(JavaPlugin plugin, ScoreboardConfig config,
                             PlaceholderResolver resolver, AfkManager afkManager) {
        this.plugin = plugin;
        this.config = config;
        this.resolver = resolver;
        this.afkManager = afkManager;
        this.worldGuard = WorldGuardHook.resolve();
        afkManager.setExternalTabName(config.isPlayerNameEnabled());
    }

    /** Desregistra o objective de cada jogador rastreado ANTES de esquecer o PlayerBoard -
     * so limpar o mapa (boards.clear()) deixava o objective "alka_score" vivo no Scoreboard
     * real do jogador (o cache esquecia, mas o objeto do Bukkit continuava registrado), e o
     * proximo tick tentava registrar de novo -> IllegalArgumentException (objective ja
     * existe), sempre apos editar o config e dar /alkaessentials reload. */
    public void reload() {
        config.reload();
        afkManager.setExternalTabName(config.isPlayerNameEnabled());
        for (UUID uuid : new ArrayList<>(boards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeBoard(player);
            }
        }
        boards.clear();
        nameOrigins.clear();
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        boolean nowOff = !toggledOff.contains(uuid);
        if (nowOff) {
            toggledOff.add(uuid);
            removeBoard(player);
        } else {
            toggledOff.remove(uuid);
            refresh(player);
        }
        return nowOff;
    }

    public boolean isToggledOff(UUID uuid) {
        return toggledOff.contains(uuid);
    }

    public int getScoreboardCount() {
        return config.getScoreboards().size();
    }

    public int getTablistCount() {
        return config.getTablists().size();
    }

    /** Roda periodicamente - re-renderiza a scoreboard/tablist de todos os online. Cada
     * jogador e isolado num try/catch: uma excecao nao pega aqui (ex: MiniMessage rejeitando
     * um placeholder mal formatado) sobe pro Bukkit e CANCELA A REPEATING TASK INTEIRA (
     * comportamento padrao do scheduler pra excecao nao tratada) - sem isolamento, um unico
     * jogador com uma linha quebrada derruba a scoreboard de TODO MUNDO ate o proximo restart
     * (reload sozinho nao resolve: ele so recarrega a config, a task ja cancelada continua
     * cancelada). */
    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                refresh(player);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Falha ao atualizar scoreboard/tablist de " + player.getName()
                                + " - pulando esse jogador neste tick.", e);
            }
        }
    }

    public void refresh(Player player) {
        UUID uuid = player.getUniqueId();
        if (toggledOff.contains(uuid) || isWorldDisabled(player)) {
            removeBoard(player);
            return;
        }
        AlkaScoreboard sb = pick(player);
        if (sb == null) {
            removeBoard(player);
            return;
        }
        applyBoard(player, sb);
        applyTablist(player);
        applyPlayerName(player);
    }

    private boolean isWorldDisabled(Player player) {
        List<String> blocked = plugin.getConfig().getStringList("scoreboard.blocked-worlds");
        return !blocked.isEmpty() && blocked.contains(player.getWorld().getName());
    }

    private AlkaScoreboard pick(Player player) {
        boolean afk = afkManager.isAfk(player.getUniqueId());
        Set<String> regionIds = null; // consultado ao WorldGuard so na 1a scoreboard que precisar (lazy)
        AlkaScoreboard best = null;
        for (AlkaScoreboard sb : config.getScoreboards().values()) {
            if (sb.getId().equalsIgnoreCase("afk") && !afk) {
                continue;
            }
            if (!sb.getWorlds().isEmpty() && !sb.getWorlds().contains(player.getWorld().getName())) {
                continue;
            }
            if (!sb.getRegions().isEmpty()) {
                if (!worldGuard.isAvailable()) {
                    continue;
                }
                if (regionIds == null) {
                    regionIds = worldGuard.getRegionIdsAt(player.getLocation());
                }
                if (regionIds.isEmpty() || sb.getRegions().stream().noneMatch(regionIds::contains)) {
                    continue;
                }
            }
            if (sb.hasPermission() && !player.hasPermission(sb.getPermission())) {
                continue;
            }
            if (best == null || sb.getPriority() > best.getPriority()) {
                best = sb;
            }
        }
        return best != null ? best : config.getScoreboard("default");
    }

    private void applyBoard(Player player, AlkaScoreboard sb) {
        PlayerBoard pb = boards.computeIfAbsent(player.getUniqueId(), k -> {
            org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
            if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();
            }
            return new PlayerBoard(board);
        });

        Objective obj = pb.objective;
        if (obj == null) {
            obj = pb.board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, Component.empty());
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            pb.objective = obj;
        }

        // limpa entradas antigas
        for (String entry : pb.entries) {
            obj.getScore(entry).resetScore();
        }
        pb.entries.clear();

        String titleFrame = resolveFrame(player, sb.getTitle());
        obj.displayName(MiniMessage.miniMessage().deserialize(titleFrame));

        List<ScoreboardEntry> visibleLines = new ArrayList<>();
        for (ScoreboardEntry line : sb.getLines()) {
            if (line.hasCondition() && !conditionPass(player, line.getCondition())) {
                continue;
            }
            visibleLines.add(line);
        }

        int size = visibleLines.size();
        for (int i = 0; i < size; i++) {
            ScoreboardEntry line = visibleLines.get(i);
            String frame = resolveFrame(player, line);
            String entryName = "\u00A7r\u00A7" + i;
            Score score = obj.getScore(entryName);
            score.customName(MiniMessage.miniMessage().deserialize(frame));
            score.numberFormat(NumberFormat.blank());
            score.setScore(size - i);
            pb.entries.add(entryName);
        }

        player.setScoreboard(pb.board);
    }

    /** Resolve placeholders e expande tags de animacao (rainbow/scroll/centralize) num frame. */
    private String resolveFrame(Player player, ScoreboardEntry entry) {
        long elapsed = System.currentTimeMillis();
        List<String> effective = new ArrayList<>();
        for (String base : entry.getFrames()) {
            String resolved = resolver.resolve(player, base);
            effective.addAll(TextAnimation.expand(resolved));
        }
        return Animation.frame(effective, entry.getInterval(), elapsed);
    }

    private boolean conditionPass(Player player, String condition) {
        String result = resolver.resolve(player, condition);
        if (result == null || result.isBlank()) {
            return false;
        }
        String lower = result.toLowerCase();
        return !(lower.equals("false") || lower.equals("0") || lower.equals("no") || lower.equals("null"));
    }

    private void applyTablist(Player player) {
        AlkaTablist tab = null;
        for (AlkaTablist t : config.getTablists().values()) {
            if (!t.getWorlds().isEmpty() && !t.getWorlds().contains(player.getWorld().getName())) {
                continue;
            }
            if (t.hasPermission() && !player.hasPermission(t.getPermission())) {
                continue;
            }
            tab = t;
            break;
        }
        if (tab == null) {
            tab = config.getTablist("default");
        }
        if (tab == null) {
            return;
        }
        Component header = componentOf(tab.getHeader(), player);
        Component footer = componentOf(tab.getFooter(), player);
        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private Component componentOf(List<String> lines, Player player) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(resolver.resolve(player, line));
        }
        return MiniMessage.miniMessage().deserialize(sb.toString());
    }

    private void applyPlayerName(Player player) {
        if (!config.isPlayerNameEnabled()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!nameOrigins.containsKey(uuid)) {
            nameOrigins.put(uuid, player.playerListName());
        }
        String format = resolver.resolve(player, config.getPlayerNameFormat());
        String afkTag = afkManager.isAfk(uuid) && plugin.getConfig().getBoolean("afk.tab-tag", true)
                ? plugin.getConfig().getString("afk.tab-tag-format", "<gold>[AFK]<reset> ") : "";
        player.playerListName(MiniMessage.miniMessage().deserialize(afkTag + format));
    }

    private void removeBoard(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerBoard pb = boards.remove(uuid);
        if (pb != null) {
            try {
                pb.board.getObjective(OBJECTIVE).unregister();
            } catch (Exception ignored) {
                // objetivo ja desregistrado
            }
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        Component original = nameOrigins.remove(uuid);
        if (original != null) {
            player.playerListName(original);
        }
    }

    public void handleQuit(UUID uuid) {
        boards.remove(uuid);
        toggledOff.remove(uuid);
        nameOrigins.remove(uuid);
    }
}
