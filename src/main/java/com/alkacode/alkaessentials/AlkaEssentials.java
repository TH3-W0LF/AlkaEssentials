package com.alkacode.alkaessentials;

import com.alkacode.alkaessentials.afk.AfkManager;
import com.alkacode.alkaessentials.afk.AfkZoneManager;
import com.alkacode.alkaessentials.command.AfkCommands;
import com.alkacode.alkaessentials.command.AfkZoneCommands;
import com.alkacode.alkaessentials.command.BackCommand;
import com.alkacode.alkaessentials.command.ChatCommands;
import com.alkacode.alkaessentials.command.HomeCommands;
import com.alkacode.alkaessentials.command.InvRestoreCommand;
import com.alkacode.alkaessentials.command.ModerationCommands;
import com.alkacode.alkaessentials.command.PunishCommands;
import com.alkacode.alkaessentials.command.QolCommands;
import com.alkacode.alkaessentials.command.RideCommand;
import com.alkacode.alkaessentials.command.RtpCommand;
import com.alkacode.alkaessentials.command.ScoreCommand;
import com.alkacode.alkaessentials.command.SitCommand;
import com.alkacode.alkaessentials.command.SpawnCommands;
import com.alkacode.alkaessentials.command.StaffCommands;
import com.alkacode.alkaessentials.command.TpaCommands;
import com.alkacode.alkaessentials.command.WarpCommands;
import com.alkacode.alkaessentials.command.WorldRulesCommand;
import com.alkacode.alkaessentials.config.MenuConfig;
import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.config.EventsConfig;
import com.alkacode.alkaessentials.config.ReasonsConfig;
import com.alkacode.alkaessentials.database.InvSnapshotRepository;
import com.alkacode.alkaessentials.database.PunishmentRepository;
import com.alkacode.alkaessentials.listener.AfkActivityListener;
import com.alkacode.alkaessentials.listener.ChatListener;
import com.alkacode.alkaessentials.listener.CommandSecurityListener;
import com.alkacode.alkaessentials.listener.CommandSignListener;
import com.alkacode.alkaessentials.listener.DeathChestListener;
import com.alkacode.alkaessentials.listener.DeathListener;
import com.alkacode.alkaessentials.listener.ElevatorListener;
import com.alkacode.alkaessentials.listener.EnvironmentListener;
import com.alkacode.alkaessentials.listener.EventTriggerListener;
import com.alkacode.alkaessentials.listener.ItemProtectionListener;
import com.alkacode.alkaessentials.listener.ModerationListener;
import com.alkacode.alkaessentials.listener.PunishmentListener;
import com.alkacode.alkaessentials.listener.QolListener;
import com.alkacode.alkaessentials.listener.SignEditorListener;
import com.alkacode.alkaessentials.listener.SlotListener;
import com.alkacode.alkaessentials.listener.TeleportListener;
import com.alkacode.alkaessentials.listener.WandListener;
import com.alkacode.alkaessentials.listener.WarmupListener;
import com.alkacode.alkaessentials.listener.WorldRulesListener;
import com.alkacode.alkaessentials.manager.AutoBroadcastManager;
import com.alkacode.alkaessentials.manager.BackManager;
import com.alkacode.alkaessentials.manager.CooldownManager;
import com.alkacode.alkaessentials.manager.DeathChestManager;
import com.alkacode.alkaessentials.manager.HomeLimit;
import com.alkacode.alkaessentials.manager.HomeManager;
import com.alkacode.alkaessentials.manager.IgnoreManager;
import com.alkacode.alkaessentials.manager.InvRestoreManager;
import com.alkacode.alkaessentials.manager.LocationStore;
import com.alkacode.alkaessentials.manager.MaintenanceManager;
import com.alkacode.alkaessentials.manager.ModerationManager;
import com.alkacode.alkaessentials.manager.NightVisionManager;
import com.alkacode.alkaessentials.manager.NickManager;
import com.alkacode.alkaessentials.manager.PunishmentManager;
import com.alkacode.alkaessentials.manager.SeatManager;
import com.alkacode.alkaessentials.manager.TempCommandManager;
import com.alkacode.alkaessentials.manager.TpaManager;
import com.alkacode.alkaessentials.manager.WarmupManager;
import com.alkacode.alkaessentials.manager.WorldRulesManager;
import com.alkacode.alkaessentials.manager.AutoBroadcastManager;
import com.alkacode.alkaessentials.scoreboard.PlaceholderResolver;
import com.alkacode.alkaessentials.scoreboard.ScoreboardConfig;
import com.alkacode.alkaessentials.scoreboard.ScoreboardManager;
import com.alkacode.alkaessentials.service.TeleportService;
import com.alkacode.core.plugin.AlkaPlugin;

/**
 * AlkaEssentials - utilitarios da network. Estende {@link AlkaPlugin} (herda depend
 * no AlkaCore, ordem de load e acesso ao {@code AlkaAPI}). Mensagens via messages.yml
 * (MessageProvider do Core), GUIs via BaseGui, locais (spawn/warps/homes) em YAML
 * proprio - decisao do projeto. Modulos futuros: QoL, morte/inventario, admin, ambiente, chat.
 */
public final class AlkaEssentials extends AlkaPlugin {

    @Override
    protected void onPluginEnable() {
        MessagesConfig.init(this);
        MenuConfig.init(this);
        ReasonsConfig.init(this);
        EventsConfig.init(this);

        boolean debug = getConfig().getBoolean("debug", false);
        if (debug) {
            getLogger().info("Modo debug ligado.");
        }

        // ----- managers e servicos (modulo 1: movimentacao/teleporte) -----
        LocationStore locations = new LocationStore(this);
        HomeManager homes = new HomeManager(this);
        HomeLimit homeLimit = new HomeLimit(this);

        CooldownManager cooldowns = new CooldownManager();
        WarmupManager warmups = new WarmupManager(this);
        BackManager back = new BackManager();
        TpaManager tpa = new TpaManager();
        TeleportService teleports = new TeleportService(this, cooldowns, warmups, back);

        // ----- listeners -----
        getServer().getPluginManager().registerEvents(
                new TeleportListener(this, locations::getSpawn, back, warmups), this);
        getServer().getPluginManager().registerEvents(new WarmupListener(this, warmups), this);
        getServer().getPluginManager().registerEvents(new ElevatorListener(this), this);

        // ----- modulo 2: AFK + zonas AFK -----
        AfkManager afkManager = new AfkManager(this);
        AfkZoneManager afkZoneManager = new AfkZoneManager(this, afkManager);
        getServer().getPluginManager().registerEvents(new AfkActivityListener(afkManager), this);
        getServer().getPluginManager().registerEvents(new WandListener(this, afkZoneManager), this);
        // ticker das zonas (1 tick) + auto-afk (1s)
        getServer().getScheduler().runTaskTimer(this, afkZoneManager::tick, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, afkManager::tickAuto, 20L, 20L);

        // ----- modulo 2: qualidade de vida -----
        NightVisionManager nightVision = new NightVisionManager(this);
        SeatManager seats = new SeatManager(this);
        getServer().getPluginManager().registerEvents(
                new QolListener(this, cooldowns, seats), this);
        long renewTicks = Math.max(1L, getConfig().getInt("qol.night-vision.renew-task-seconds", 60)) * 20L;
        getServer().getScheduler().runTaskTimer(this, nightVision::renew, renewTicks, renewTicks);

        // ----- modulo 3: morte e inventario -----
        InvSnapshotRepository snapshotRepo =
                new InvSnapshotRepository(getAlkaAPI().getDatabase(), this);
        InvRestoreManager invRestore = new InvRestoreManager(snapshotRepo);
        DeathChestManager deathChest = new DeathChestManager(this);
        getServer().getPluginManager().registerEvents(
                new DeathListener(this, invRestore, deathChest), this);
        getServer().getPluginManager().registerEvents(new DeathChestListener(deathChest), this);
        // remove tumulos expirados a cada segundo
        getServer().getScheduler().runTaskTimer(this, deathChest::tick, 20L, 20L);

        // ----- AlkaScore: scoreboard + tablist -----
        ScoreboardConfig scoreConfig = new ScoreboardConfig(this);
        NickManager nicks = new NickManager(this);
        PlaceholderResolver placeholders =
                new PlaceholderResolver(this, afkManager, homes, nicks);
        ScoreboardManager scoreboardManager =
                new ScoreboardManager(this, scoreConfig, placeholders, afkManager);
        // re-renderiza a cada 0.5s (10 ticks) - suficiente pra animacoes
        getServer().getScheduler().runTaskTimer(this, scoreboardManager::tick, 10L, 10L);

        // ----- modulo 4: admin, moderacao e punicoes -----
        PunishmentRepository punishmentRepo =
                new PunishmentRepository(getAlkaAPI().getDatabase(), this);
        PunishmentManager punishments = new PunishmentManager(this, punishmentRepo);
        ModerationManager moderation = new ModerationManager();
        MaintenanceManager maintenance = new MaintenanceManager(this);
        TempCommandManager tempCommands = new TempCommandManager(this, moderation);
        getServer().getPluginManager().registerEvents(
                new PunishmentListener(this, punishments), this);
        getServer().getPluginManager().registerEvents(
                new ModerationListener(this, moderation, maintenance), this);
        // expira punicoes temporarias a cada minuto
        getServer().getScheduler().runTaskTimer(this, punishments::expireAll, 1200L, 1200L);

        // ----- modulo 5: ambiente, protecao e eventos -----
        getServer().getPluginManager().registerEvents(new EnvironmentListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandSecurityListener(this), this);
        getServer().getPluginManager().registerEvents(new SlotListener(this), this);
        getServer().getPluginManager().registerEvents(new EventTriggerListener(this), this);
        getServer().getPluginManager().registerEvents(new SignEditorListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandSignListener(this), this);
        // regras do mundo configuráveis por mundo via GUI
        WorldRulesManager worldRules = new WorldRulesManager(this);
        getServer().getPluginManager().registerEvents(new WorldRulesListener(this, worldRules), this);
        // aplica o modo de tempo dos mundos periodicamente
        getServer().getScheduler().runTaskTimer(this, worldRules::applyAllTime, 20L, 20L);

        // ----- modulo 6: chat e social -----
        IgnoreManager ignores = new IgnoreManager(this);
        com.alkacode.alkaessentials.hook.TabHook tabHook = new com.alkacode.alkaessentials.hook.TabHook(nicks);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.alkacode.alkaessentials.hook.PapiExpansion(this, nicks).register();
        }
        getServer().getPluginManager().registerEvents(
                new ChatListener(this, nicks, ignores, punishments, tabHook), this);
        if (getConfig().getBoolean("chat.autobroadcast.enabled", true)) {
            AutoBroadcastManager autoBroadcast = new AutoBroadcastManager(this);
            long interval = Math.max(1L, getConfig().getLong("chat.autobroadcast.interval-seconds", 120)) * 20L;
            getServer().getScheduler().runTaskTimer(this, autoBroadcast::broadcastNext, interval, interval);
        }

        // ----- comandos -----
        register(new SpawnCommands(this, locations, teleports), "spawn", "setspawn", "delspawn");
        register(new WarpCommands(this, locations, teleports), "warp", "setwarp", "delwarp");
        register(new HomeCommands(this, homes, homeLimit, teleports), "home", "sethome", "delhome", "homes");
        register(new TpaCommands(this, tpa, teleports, ignores), "tpa", "tpahere", "tpaccept", "tpdeny", "tptoggle");
        register(new RtpCommand(this, teleports), "rtp");
        register(new BackCommand(this, back, teleports), "back");
        register(new AfkCommands(this, afkManager), "afk");
        register(new AfkZoneCommands(this, afkZoneManager), "afkzone");
        register(new QolCommands(this, nightVision), "craft", "wb", "lixo", "trash", "nv", "luz", "ping");
        register(new SitCommand(this, seats), "sit");
        register(new RideCommand(this), "ride");
        register(new InvRestoreCommand(this, invRestore), "invrestore");
        register(new ScoreCommand(this, scoreboardManager), "scoreboard", "score");
        register(new PunishCommands(this, punishments),
                "warn", "kick", "ban", "tempban", "mute", "tempmute", "unban", "unmute", "punishinfo", "punish");
        register(new StaffCommands(this, moderation, tempCommands),
                "clear", "heal", "feed", "fly", "god", "freeze", "invsee", "ptime", "pweather");
        register(new ModerationCommands(this, moderation, maintenance, punishments),
                "vanish", "socialspy", "commandspy", "maintenance", "staff");
        register(new ChatCommands(this, nicks, ignores, tabHook),
                "nick", "color", "namecolor", "gradient", "realname", "whois",
                "ignore", "clearchat", "broadcast", "discord", "site", "loja", "regras");
        register(new WorldRulesCommand(this, worldRules), "worldrules");

        getLogger().info("AlkaEssentials habilitado (" + locations.getWarps().size() + " warp(s), spawn "
                + (locations.hasSpawn() ? "definido" : "nao definido")
                + ", " + afkZoneManager.getZones().size() + " zona(s) AFK).");
    }

    private void register(org.bukkit.command.CommandExecutor executor, String... commands) {
        for (String name : commands) {
            org.bukkit.command.PluginCommand cmd = getCommand(name);
            if (cmd == null) {
                getLogger().warning("Comando nao registrado no plugin.yml: /" + name);
                continue;
            }
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
                cmd.setTabCompleter(tabCompleter);
            }
        }
    }

    @Override
    protected void onPluginDisable() {
        // YAML de locais ja salvo a cada mudanca; zonas AFK limpam bossbars/tags no disable.
    }
}
