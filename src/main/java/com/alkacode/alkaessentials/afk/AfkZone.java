package com.alkacode.alkaessentials.afk;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.util.TimeUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Uma zona AFK registrada em zones/<nome>.yml. A cada segundo (tick a cada 20 ticks),
 * acumula tempo dos jogadores dentro da regiao, rola recompensas por chance quando
 * {@code reward-time-seconds} e atingido e mostra Title/Actionbar/BossBar de progresso.
 * Jogadores dentro de uma zona sao marcados como AFK via {@link AfkManager}.
 */
public final class AfkZone {

    private final JavaPlugin plugin;
    private final AfkManager afkManager;
    private final String name;
    private final File file;
    private final YamlConfiguration config;

    private ZoneRegion region;
    private int rewardSeconds;
    private int rollAmount;
    private String permission;
    private boolean resetAfterReward;
    private final List<AfkReward> rewards = new ArrayList<>();

    private final Map<UUID, Integer> zonePlayers = new HashMap<>();
    private final Map<UUID, BossBar> bossbars = new HashMap<>();
    private int ticks;

    public AfkZone(JavaPlugin plugin, AfkManager afkManager, String name, File file, YamlConfiguration config) {
        this.plugin = plugin;
        this.afkManager = afkManager;
        this.name = name;
        this.file = file;
        this.config = config;
        reload();
    }

    public void reload() {
        this.region = new ZoneRegion(
                com.alkacode.alkaessentials.util.LocationUtil.deserialize(config.getString("zone.location1"), null),
                com.alkacode.alkaessentials.util.LocationUtil.deserialize(config.getString("zone.location2"), null));
        this.rewardSeconds = config.getInt("reward-time-seconds", 180);
        this.rollAmount = config.getInt("roll-amount", 1);
        this.permission = config.getString("permission", "");
        this.resetAfterReward = config.getBoolean("reset-after-reward", false);
        this.rewards.clear();
        int idx = 0;
        for (Map<?, ?> map : config.getMapList("rewards")) {
            String path = "__reward_" + idx++;
            org.bukkit.configuration.ConfigurationSection section = config.createSection(path);
            section.set("chance", map.get("chance"));
            section.set("permission", map.get("permission"));
            section.set("minimum-time", map.get("minimum-time"));
            section.set("maximum-time", map.get("maximum-time"));
            section.set("display", map.get("display"));
            section.set("commands", map.get("commands"));
            section.set("items", map.get("items"));
            rewards.add(new AfkReward(section));
            config.set(path, null);
        }
    }

    public void tick() {
        boolean runChecks = ++ticks % 20 == 0;
        Set<Player> inZone = region.getPlayersInZone(permission);
        Iterator<Map.Entry<UUID, Integer>> it = zonePlayers.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                inZone.remove(player);
                leave(uuid, it, null);
            } else if (!inZone.contains(player)) {
                leave(uuid, it, player);
            } else {
                if (runChecks) {
                    int newTime = entry.getValue() + 1;
                    zonePlayers.put(uuid, newTime);
                    if (newTime % rewardSeconds == 0) {
                        giveRewards(player, newTime);
                        if (resetAfterReward) {
                            zonePlayers.put(uuid, 0);
                        }
                    }
                    sendTitle(player, newTime);
                    sendActionbar(player, newTime);
                    updateBossbar(player, newTime);
                }
                inZone.remove(player);
            }
        }

        int ipLimit = plugin.getConfig().getInt("afk-zone.zone-per-ip-limit", -1);
        for (Player player : inZone) {
            if (ipLimit != -1 && countIPAccounts(player) >= ipLimit) {
                ChatUtil.sendKey(player, "afkzone-ip-limit");
            } else {
                enter(player);
            }
        }
    }

    private int countIPAccounts(Player player) {
        if (player.getAddress() == null) {
            return 0;
        }
        String bypass = plugin.getConfig().getString("afk-zone.bypass-ip-limit-permission",
                "alkassentials.afkzone.bypass.iplimit");
        if (player.hasPermission(bypass)) {
            return 0;
        }
        int count = 0;
        for (UUID uuid : zonePlayers.keySet()) {
            Player other = Bukkit.getPlayer(uuid);
            if (other == null || other.hasPermission(bypass)) {
                continue;
            }
            if (other.getAddress() != null
                    && other.getAddress().getAddress().equals(player.getAddress().getAddress())) {
                count++;
            }
        }
        return count;
    }

    private void enter(Player player) {
        BossBar bar = bossbars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
        sendZoneMsg(player, config.getString("messages.entered"), remainingSeconds(0));
        zonePlayers.put(player.getUniqueId(), 0);
        BossBar bossBar = createBossBar(player.getUniqueId());
        if (bossBar != null) {
            player.showBossBar(bossBar);
            bossbars.put(player.getUniqueId(), bossBar);
        }
        sendTitle(player, 0);
        sendActionbar(player, 0);
        afkManager.setInZone(player.getUniqueId(), true);
    }

    private void leave(UUID uuid, Iterator<Map.Entry<UUID, Integer>> it, Player player) {
        if (player != null && player.isOnline()) {
            Integer time = zonePlayers.get(uuid);
            sendZoneMsg(player, config.getString("messages.left"), time == null ? 0 : time);
        }
        it.remove();
        BossBar bar = bossbars.remove(uuid);
        if (bar != null && player != null) {
            player.hideBossBar(bar);
        }
        afkManager.setInZone(uuid, false);
    }

    private void giveRewards(Player player, int newTime) {
        List<AfkReward> eligible = new ArrayList<>();
        for (AfkReward reward : rewards) {
            if (reward.hasPermission(player)
                    && newTime >= reward.getMinimumTime() && newTime <= reward.getMaximumTime()) {
                eligible.add(reward);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }
        List<String> given = new ArrayList<>();
        for (int i = 0; i < rollAmount; i++) {
            AfkReward reward = weightedPick(eligible);
            if (reward == null) {
                continue;
            }
            reward.run(player);
            if (reward.getDisplay() != null) {
                given.add(reward.getDisplay());
            }
        }
        sendRewardMessage(player, newTime, given);
    }

    private AfkReward weightedPick(List<AfkReward> eligible) {
        double total = 0;
        for (AfkReward reward : eligible) {
            total += reward.getChance();
        }
        if (total <= 0) {
            return eligible.isEmpty() ? null : eligible.get(0);
        }
        double roll = Math.random() * total;
        double acc = 0;
        for (AfkReward reward : eligible) {
            acc += reward.getChance();
            if (roll < acc) {
                return reward;
            }
        }
        return eligible.get(eligible.size() - 1);
    }

    private void sendRewardMessage(Player player, int newTime, List<String> given) {
        List<String> lines = config.getStringList("messages.reward");
        if (lines.isEmpty() || given.isEmpty()) {
            return;
        }
        for (String line : lines) {
            if (line.contains("%reward%")) {
                for (String display : given) {
                    sendZoneMsg(player, line.replace("%reward%", display), newTime);
                }
            } else {
                sendZoneMsg(player, line, newTime);
            }
        }
    }

    private void sendZoneMsg(Player player, String message, int elapsedSeconds) {
        if (message == null || message.isBlank()) {
            return;
        }
        String prefix = MessagesConfig.getInstance().get("prefix");
        String parsed = message.replace("%time%", TimeUtil.formatSeconds(remainingSeconds(elapsedSeconds)))
                .replace("%zone%", name)
                .replace("{prefix}", prefix);
        player.sendMessage(MiniMessage.miniMessage().deserialize(parsed));
    }

    private int remainingSeconds(int elapsed) {
        int remaining = rewardSeconds - (elapsed % rewardSeconds);
        return Math.max(0, remaining);
    }

    private void sendTitle(Player player, int elapsed) {
        String title = config.getString("in-zone.title");
        String subtitle = config.getString("in-zone.subtitle");
        if ((title == null || title.isBlank()) && (subtitle == null || subtitle.isBlank())) {
            return;
        }
        String time = TimeUtil.formatSeconds(remainingSeconds(elapsed));
        Component titleComp = title == null || title.isBlank()
                ? Component.empty()
                : MiniMessage.miniMessage().deserialize(title.replace("%time%", time));
        Component subComp = subtitle == null || subtitle.isBlank()
                ? Component.empty()
                : MiniMessage.miniMessage().deserialize(subtitle.replace("%time%", time));
        player.showTitle(Title.title(titleComp, subComp, Title.Times.times(
                java.time.Duration.ofMillis(0), java.time.Duration.ofMillis(1000), java.time.Duration.ofMillis(0))));
    }

    private void sendActionbar(Player player, int elapsed) {
        String actionbar = config.getString("in-zone.actionbar");
        if (actionbar == null || actionbar.isBlank()) {
            return;
        }
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                actionbar.replace("%time%", TimeUtil.formatSeconds(remainingSeconds(elapsed)))));
    }

    private BossBar createBossBar(UUID uuid) {
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("in-zone.bossbar");
        if (section == null) {
            return null;
        }
        String name = section.getString("name", "AFK");
        BossBar.Color color;
        BossBar.Overlay overlay;
        try {
            color = BossBar.Color.valueOf(section.getString("color", "RED").toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.RED;
        }
        try {
            overlay = BossBar.Overlay.valueOf(section.getString("style", "NOTCHED_20").toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.NOTCHED_20;
        }
        return BossBar.bossBar(MiniMessage.miniMessage().deserialize(name.replace("%time%", "")), 1.0f, color, overlay);
    }

    private void updateBossbar(Player player, int elapsed) {
        BossBar bar = bossbars.get(player.getUniqueId());
        if (bar == null) {
            return;
        }
        int direction = plugin.getConfig().getInt("afk-zone.bossbar-direction", 0);
        float calculated = (float) (elapsed % rewardSeconds) / Math.max(1, rewardSeconds);
        float progress = Math.max(0.0f, Math.min(1.0f, direction == 0 ? 1.0f - calculated : calculated));
        bar.progress(progress);
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("in-zone.bossbar");
        if (section != null && section.getString("name") != null) {
            bar.name(MiniMessage.miniMessage().deserialize(
                    section.getString("name").replace("%time%", TimeUtil.formatSeconds(remainingSeconds(elapsed)))));
        }
    }

    public void disable() {
        for (Map.Entry<UUID, BossBar> entry : bossbars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        for (UUID uuid : new HashSet<>(zonePlayers.keySet())) {
            afkManager.setInZone(uuid, false);
        }
        bossbars.clear();
        zonePlayers.clear();
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public ZoneRegion getRegion() {
        return region;
    }

    public void setRegion(ZoneRegion newRegion) {
        this.region = newRegion;
        config.set("zone.location1", com.alkacode.alkaessentials.util.LocationUtil.serialize(newRegion.getCorner1()));
        config.set("zone.location2", com.alkacode.alkaessentials.util.LocationUtil.serialize(newRegion.getCorner2()));
        save();
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void save() {
        try {
            config.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Falha ao salvar zones/" + name + ".yml: " + e.getMessage());
        }
    }
}
