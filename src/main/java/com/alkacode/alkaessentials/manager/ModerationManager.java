package com.alkacode.alkaessentials.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Estados de moderacao por jogador: freeze, vanish, socialspy, commandspy e god. */
public final class ModerationManager {

    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> socialSpy = new HashSet<>();
    private final Set<UUID> commandSpy = new HashSet<>();
    private final Set<UUID> god = new HashSet<>();

    // freeze
    public boolean isFrozen(UUID uuid) { return frozen.contains(uuid); }
    public void setFrozen(UUID uuid, boolean value) { if (value) frozen.add(uuid); else frozen.remove(uuid); }
    public boolean toggleFrozen(UUID uuid) { if (frozen.add(uuid)) return true; frozen.remove(uuid); return false; }

    // vanish
    public boolean isVanished(UUID uuid) { return vanished.contains(uuid); }
    public void setVanished(UUID uuid, boolean value) { if (value) vanished.add(uuid); else vanished.remove(uuid); }
    public boolean toggleVanished(UUID uuid) { if (vanished.add(uuid)) return true; vanished.remove(uuid); return false; }

    // socialspy
    public boolean isSocialSpy(UUID uuid) { return socialSpy.contains(uuid); }
    public boolean toggleSocialSpy(UUID uuid) { if (socialSpy.add(uuid)) return true; socialSpy.remove(uuid); return false; }

    // commandspy
    public boolean isCommandSpy(UUID uuid) { return commandSpy.contains(uuid); }
    public boolean toggleCommandSpy(UUID uuid) { if (commandSpy.add(uuid)) return true; commandSpy.remove(uuid); return false; }

    // god
    public boolean isGod(UUID uuid) { return god.contains(uuid); }
    public void setGod(UUID uuid, boolean value) { if (value) god.add(uuid); else god.remove(uuid); }

    public void handleQuit(UUID uuid) {
        frozen.remove(uuid);
        vanished.remove(uuid);
        socialSpy.remove(uuid);
        commandSpy.remove(uuid);
        god.remove(uuid);
    }
}
