package com.alkacode.alkaessentials.scoreboard;

import java.util.List;

/** Uma tablist (header/footer) configurada em tablists.yml. */
public final class AlkaTablist {

    private final String id;
    private final List<String> worlds;
    private final String permission;
    private final List<String> header;
    private final List<String> footer;

    public AlkaTablist(String id, List<String> worlds, String permission, List<String> header, List<String> footer) {
        this.id = id;
        this.worlds = worlds;
        this.permission = permission == null ? "" : permission;
        this.header = header;
        this.footer = footer;
    }

    public String getId() { return id; }
    public List<String> getWorlds() { return worlds; }
    public String getPermission() { return permission; }
    public List<String> getHeader() { return header; }
    public List<String> getFooter() { return footer; }

    public boolean hasPermission() {
        return !permission.isBlank();
    }
}
