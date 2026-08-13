package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Warmup de teleporte: atrasa a execucao de um {@link Runnable} (o teleporte) por
 * N segundos e cancela se o jogador se mover (acima de um limiar) ou tomar dano
 * (decidido nos listeners). O callback faz tudo - teleporte, salvar back, aplicar
 * cooldown e mensagem - entao cancelar aqui desfaz a operacao inteira.
 */
public final class WarmupManager {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Map<UUID, Player> pending = new HashMap<>();

    public WarmupManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player player, long seconds, Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        cancel(uuid);
        pending.put(uuid, player);
        ChatUtil.sendKey(player, "warmup-start", Map.of("time", String.valueOf(seconds)));
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tasks.remove(uuid);
            pending.remove(uuid);
            onComplete.run();
        }, seconds * 20L);
        tasks.put(uuid, task);
    }

    /** Cancela o warmup do jogador. Se estava pendente, avisa que foi cancelado. */
    public void cancel(UUID uuid) {
        BukkitTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        Player player = pending.remove(uuid);
        if (player != null && player.isOnline()) {
            ChatUtil.sendKey(player, "warmup-cancelled");
        }
    }

    public void cancelAll() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        pending.clear();
    }

    /** Usado pelos listeners (move/dano) pra checar se o jogador tem warmup ativo sem cancelar. */
    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    /** Cancela o warmup de um jogador que saiu do jogo, sem mandar mensagem. */
    public void cancelIfPresent(UUID uuid) {
        BukkitTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        pending.remove(uuid);
    }
}
