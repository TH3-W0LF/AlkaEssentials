package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.manager.WorldRulesManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

/** Aplica as regras por mundo (spawn de mobs, chuva, gravidade, fogo, folhas, fome, explosoes, mob-grief). */
public final class WorldRulesListener implements Listener {

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
    private final WorldRulesManager rules;

    public WorldRulesListener(JavaPlugin plugin, WorldRulesManager rules) {
        this.plugin = plugin;
        this.rules = rules;
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        if (event.isCancelled()) return;
        World world = event.getLocation().getWorld();
        if (world == null) return;
        if (event.getEntity() instanceof Player) return;
        if (!rules.getRule(world, "mobs") && WorldRulesManager.HOSTILE.contains(event.getEntityType())) {
            event.setCancelled(true);
            return;
        }
        if (!rules.getRule(world, "animals") && event.getEntity() instanceof org.bukkit.entity.Animals) {
            event.setCancelled(true);
            return;
        }
        if (!rules.getRule(world, "zombie-villagers")
                && event.getEntity() instanceof org.bukkit.entity.ZombieVillager) {
            event.setCancelled(true);
            return;
        }
        if (!rules.getRule(world, "villagers")
                && (event.getEntity() instanceof org.bukkit.entity.Villager
                    || event.getEntity() instanceof org.bukkit.entity.WanderingTrader)) {
            event.setCancelled(true);
            return;
        }
        if (!rules.getRule(world, "structures")
                && (event.getEntity() instanceof org.bukkit.entity.IronGolem
                    || event.getEntity() instanceof org.bukkit.entity.Snowman)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        World world = event.getWorld();
        if (!rules.getRule(world, "rain") && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGravity(BlockPhysicsEvent event) {
        World world = event.getBlock().getWorld();
        if (!rules.getRule(world, "gravity") && GRAVITY.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        World world = event.getBlock().getWorld();
        if (!rules.getRule(world, "fire") && event.getNewState().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        World world = event.getBlock().getWorld();
        if (!rules.getRule(world, "leaves")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        World world = player.getWorld();
        if (!rules.getRule(world, "hunger") && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        World world = event.getLocation().getWorld();
        if (world == null) return;
        if (event.getEntity() instanceof Creeper && !rules.getRule(world, "creeper")) {
            event.setCancelled(true);
        } else if (event.getEntity() instanceof TNTPrimed && !rules.getRule(world, "tnt")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobGrief(EntityChangeBlockEvent event) {
        World world = event.getBlock().getWorld();
        if (world == null) return;
        if (!rules.getRule(world, "mob-grief") && event.getEntity() instanceof Mob) {
            event.setCancelled(true);
        }
    }
}
