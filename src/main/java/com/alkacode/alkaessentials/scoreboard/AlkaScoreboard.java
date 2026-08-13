package com.alkacode.alkaessentials.scoreboard;

import java.util.List;

/** Uma scoreboard configurada em scoreboards.yml. */
public final class AlkaScoreboard {

    private final String id;
    private final int priority;
    private final List<String> worlds;
    private final List<String> regions;
    private final String permission;
    private final ScoreboardEntry title;
    private final List<ScoreboardEntry> lines;

    public AlkaScoreboard(String id, int priority, List<String> worlds, List<String> regions,
                          String permission, ScoreboardEntry title, List<ScoreboardEntry> lines) {
        this.id = id;
        this.priority = priority;
        this.worlds = worlds;
        this.regions = regions;
        this.permission = permission == null ? "" : permission;
        this.title = title;
        this.lines = lines;
    }

    public String getId() { return id; }
    public int getPriority() { return priority; }
    public List<String> getWorlds() { return worlds; }
    public List<String> getRegions() { return regions; }
    public String getPermission() { return permission; }
    public ScoreboardEntry getTitle() { return title; }
    public List<ScoreboardEntry> getLines() { return lines; }

    public boolean hasPermission() {
        return !permission.isBlank();
    }
}
