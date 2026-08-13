package com.alkacode.alkaessentials.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Visao noturna permanente (/nv): aplica o efeito e renova periodicamente, sem particulas. */
public final class NightVisionManager {

    private final JavaPlugin plugin;
    private final Set<UUID> enabled = new HashSet<>();

    public NightVisionManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(UUID uuid) {
        return enabled.contains(uuid);
    }

    public void toggle(Player player) {
        if (enabled.contains(player.getUniqueId())) {
            disable(player);
        } else {
            enable(player);
        }
    }

    public void enable(Player player) {
        enabled.add(player.getUniqueId());
        apply(player);
    }

    public void disable(Player player) {
        enabled.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    private void apply(Player player) {
        int duration = plugin.getConfig().getInt("qol.night-vision.duration-seconds", 300);
        int amplifier = plugin.getConfig().getInt("qol.night-vision.amplifier", 0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,
                duration * 20, amplifier, true, false, false));
    }

    /** Renova o efeito dos jogadores com /nv ativo - chamado pelo task periodico. */
    public void renew() {
        for (UUID uuid : enabled) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                apply(player);
            }
        }
    }

    public void handleQuit(UUID uuid) {
        enabled.remove(uuid);
    }
}
