package com.alkacode.alkaessentials.service;

import com.alkacode.alkaessentials.manager.BackManager;
import com.alkacode.alkaessentials.manager.CooldownManager;
import com.alkacode.alkaessentials.manager.WarmupManager;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.core.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Logica central de teleporte: le cooldown/warmup do config.yml por acao, checa
 * cooldown (com bypass de staff), aplica warmup (com callback que faz teleporte +
 * salvar back + aplicar cooldown + mensagem). Todos os comandos de teleporte passam
 * por aqui pra nao duplicar a regra.
 */
public final class TeleportService {

    private static final String BYPASS = "alkassentials.teleport.bypass";
    private static final String CD_BYPASS = "alkassentials.teleport.cooldown.bypass";
    private static final String WARMUP_BYPASS = "alkassentials.teleport.warmup.bypass";

    private final JavaPlugin plugin;
    private final CooldownManager cooldowns;
    private final WarmupManager warmups;
    private final BackManager back;

    public TeleportService(JavaPlugin plugin, CooldownManager cooldowns, WarmupManager warmups, BackManager back) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.warmups = warmups;
        this.back = back;
    }

    public long cooldownSeconds(String action) {
        return plugin.getConfig().getLong("teleport.cooldowns." + action, 0);
    }

    public long warmupSeconds(String action) {
        return plugin.getConfig().getLong("teleport.warmups." + action, 0);
    }

    public boolean checkCooldown(Player player, String action) {
        if (player.hasPermission(BYPASS) || player.hasPermission(CD_BYPASS)) {
            return true;
        }
        long remaining = cooldowns.remainingSeconds(player.getUniqueId(), action);
        if (remaining > 0) {
            ChatUtil.sendKey(player, "cooldown", Map.of("time", TimeUtil.formatSeconds(remaining)));
            return false;
        }
        return true;
    }

    /**
     * Teleporta o jogador respeitando cooldown + warmup. Se {@code saveBack} for true,
     * salva a posicao atual no /back antes de teleportar (nao fazer isso pro proprio
     * /back, senao sobrescreveria o destino).
     */
    public void teleport(Player player, Location to, String action, boolean saveBack,
                         String doneKey, Map<String, String> placeholders) {
        if (to == null || to.getWorld() == null) {
            return;
        }
        if (!checkCooldown(player, action)) {
            return;
        }
        long warmup = warmupSeconds(action);
        boolean bypassWarmup = player.hasPermission(BYPASS) || player.hasPermission(WARMUP_BYPASS);

        Runnable complete = () -> {
            if (saveBack) {
                back.save(player.getUniqueId(), player.getLocation());
            }
            player.teleport(to);
            cooldowns.set(player.getUniqueId(), action, cooldownSeconds(action));
            if (doneKey != null) {
                ChatUtil.sendKey(player, doneKey, placeholders);
            }
        };

        if (warmup > 0 && !bypassWarmup) {
            warmups.start(player, warmup, complete);
        } else {
            complete.run();
        }
    }
}
