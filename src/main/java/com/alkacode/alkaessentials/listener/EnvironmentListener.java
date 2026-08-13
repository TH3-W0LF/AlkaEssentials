package com.alkacode.alkaessentials.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** World rules: chuva, propagacao de fogo, decaimento de folhas e gravidade de blocos. */
public final class EnvironmentListener implements Listener {

    private static final Set<Material> GRAVITY = new HashSet<>();

    static {
        GRAVITY.add(Material.SAND);
        GRAVITY.add(Material.RED_SAND);
        GRAVITY.add(Material.GRAVEL);
        GRAVITY.add(Material.ANVIL);
        GRAVITY.add(Material.CHIPPED_ANVIL);
        GRAVITY.add(Material.DAMAGED_ANVIL);
        for (Material m : Material.values()) {
            if (m.name().endsWith("CONCRETE_POWDER")) {
                GRAVITY.add(m);
            }
        }
    }

    private final JavaPlugin plugin;

    public EnvironmentListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        if (plugin.getConfig().getBoolean("world.block-rain", false) && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        if (plugin.getConfig().getBoolean("world.block-fire-spread", true)
                && event.getNewState().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        if (plugin.getConfig().getBoolean("world.block-leaf-decay", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGravity(BlockPhysicsEvent event) {
        if (!plugin.getConfig().getBoolean("world.block-gravity-fall", false)) {
            return;
        }
        Block block = event.getBlock();
        if (GRAVITY.contains(block.getType())) {
            event.setCancelled(true);
        }
    }
}
