package com.alkacode.alkaessentials.manager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras do mundo, configuráveis por mundo via GUI (/worldrules). Persistidas em
 * worldrules.yml: secao "default" e overrides em "worlds.<mundo>". Os valores default
 * sao semeados a partir do config.yml (secao world.*) na primeira execucao.
 */
public final class WorldRulesManager {

    public static final List<String> RULES = List.of(
            "mobs", "animals", "zombie-villagers", "villagers", "structures",
            "rain", "gravity", "fire", "leaves", "hunger", "creeper", "tnt", "mob-grief", "phantoms");

    public static final List<String> TIME_MODES = List.of("normal", "day", "day_locked", "night", "night_locked");

    /** Tipos de mobs hostis (cobrem slimes, magma, ghast etc., que nao implementam Monster).
     * Phantom fica de fora de proposito - tem regra propria ("phantoms"), independente do
     * toggle geral, porque tem mundo que so quer desligar phantom e manter o resto dos hostis. */
    public static final Set<EntityType> HOSTILE = EnumSet.of(
            EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
            EntityType.CAVE_SPIDER, EntityType.ENDERMAN, EntityType.WITCH, EntityType.SLIME,
            EntityType.MAGMA_CUBE, EntityType.GHAST, EntityType.BLAZE,
            EntityType.HUSK, EntityType.DROWNED, EntityType.STRAY, EntityType.WITHER_SKELETON,
            EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.HOGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.SHULKER, EntityType.VINDICATOR,
            EntityType.PILLAGER, EntityType.RAVAGER, EntityType.EVOKER, EntityType.VEX, EntityType.WARDEN,
            EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.ZOGLIN, EntityType.STRIDER,
            EntityType.ZOMBIE_VILLAGER);

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public WorldRulesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "worldrules.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            config = new YamlConfiguration();
            seedDefaults();
            save();
        } else {
            config = YamlConfiguration.loadConfiguration(file);
        }
    }

    private void seedDefaults() {
        config.set("default.mobs", true);
        config.set("default.rain", !plugin.getConfig().getBoolean("world.block-rain", false));
        config.set("default.gravity", !plugin.getConfig().getBoolean("world.block-gravity-fall", false));
        config.set("default.fire", !plugin.getConfig().getBoolean("world.block-fire-spread", true));
        config.set("default.leaves", !plugin.getConfig().getBoolean("world.block-leaf-decay", false));
        config.set("default.hunger", true);
        config.set("default.creeper", true);
        config.set("default.tnt", true);
        config.set("default.mob-grief", true);
        config.set("default.phantoms", true);
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao salvar worldrules.yml: " + e.getMessage());
        }
    }

    /** Valor da regra para o mundo (override local ou default). */
    public boolean getRule(World world, String rule) {
        if (config.contains("worlds." + world.getName() + "." + rule)) {
            return config.getBoolean("worlds." + world.getName() + "." + rule);
        }
        return config.getBoolean("default." + rule, true);
    }

    public void setRule(World world, String rule, boolean value) {
        config.set("worlds." + world.getName() + "." + rule, value);
        save();
        // efeito imediato ao desligar (sem precisar reiniciar)
        if (!value) {
            switch (rule) {
                case "rain":
                    world.setStorm(false);
                    world.setThundering(false);
                    break;
                case "mobs":
                    removeMonsters(world);
                    break;
                case "animals":
                    removeAnimals(world);
                    break;
                case "zombie-villagers":
                    removeZombieVillagers(world);
                    break;
                case "villagers":
                    removeVillagers(world);
                    break;
                case "structures":
                    removeStructures(world);
                    break;
                case "phantoms":
                    removePhantoms(world);
                    break;
                default:
                    break;
            }
        }
    }

    private void removeMonsters(World world) {
        for (Entity entity : world.getEntities()) {
            if (HOSTILE.contains(entity.getType()) && !(entity instanceof Player) && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    public void removeAnimals(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Animals
                    && !(entity instanceof Player) && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    /** Remove zumbis-vilager (mobs que nao implementam Monster). */
    public void removeZombieVillagers(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.ZombieVillager && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    /** Remove vilagers normais e comerciantes errantes. */
    public void removeVillagers(World world) {
        for (Entity entity : world.getEntities()) {
            if (!entity.isDead()
                    && (entity instanceof org.bukkit.entity.Villager
                        || entity instanceof org.bukkit.entity.WanderingTrader)) {
                entity.remove();
            }
        }
    }

    /** Remove guardioes de estruturas (golems de ferro/neve). */
    public void removeStructures(World world) {
        for (Entity entity : world.getEntities()) {
            if (!entity.isDead()
                    && (entity instanceof org.bukkit.entity.IronGolem
                        || entity instanceof org.bukkit.entity.Snowman)) {
                entity.remove();
            }
        }
    }

    /** Remove phantoms existentes (o toggle "mobs" tambem cobre phantoms, este e um botao dedicado). */
    public void removePhantoms(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.getType() == EntityType.PHANTOM && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    // ------------------------- modo de tempo -------------------------

    /** Modo de tempo do mundo: normal, day, day_locked, night ou night_locked. */
    public String getTimeMode(World world) {
        String mode = config.getString("worlds." + world.getName() + ".time");
        if (mode == null) {
            mode = config.getString("default.time", "normal");
        }
        return TIME_MODES.contains(mode) ? mode : "normal";
    }

    public void setTimeMode(World world, String mode) {
        if (!TIME_MODES.contains(mode)) {
            mode = "normal";
        }
        config.set("worlds." + world.getName() + ".time", mode);
        save();
        applyTime(world, mode);
    }

    /** Avanca para o proximo modo de tempo e retorna o novo modo. */
    public String nextTimeMode(World world) {
        String current = getTimeMode(world);
        int idx = TIME_MODES.indexOf(current);
        String next = TIME_MODES.get((idx + 1) % TIME_MODES.size());
        setTimeMode(world, next);
        return next;
    }

    /** Aplica o modo de tempo imediatamente (pulo de horario incluido - usar no clique do botao). */
    public void applyTime(World world, String mode) {
        switch (mode) {
            case "day":
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
                world.setTime(6000L);
                break;
            case "day_locked":
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setTime(6000L);
                break;
            case "night":
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
                world.setTime(18000L);
                break;
            case "night_locked":
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setTime(18000L);
                break;
            default:
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
                break;
        }
    }

    /** Reforca o modo de tempo de todos os mundos (chamado a cada segundo). So reaplica o
     * pulo de horario nos modos travados (day_locked/night_locked) - "day"/"night"/"normal"
     * so ajustam a gamerule pra nao ficar puxando o relogio de volta toda hora e impedir
     * o ciclo vanilla de rodar de verdade depois do pulo inicial. */
    public void applyAllTime() {
        for (World world : getWorlds().values()) {
            String mode = getTimeMode(world);
            if (mode.equals("day_locked") || mode.equals("night_locked")) {
                applyTime(world, mode);
            } else {
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
            }
        }
    }

    public void resetWorld(World world) {
        config.set("worlds." + world.getName(), null);
        save();
    }

    public Map<String, World> getWorlds() {
        Map<String, World> result = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            result.put(world.getName(), world);
        }
        return result;
    }
}
