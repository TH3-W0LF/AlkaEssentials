package com.alkacode.alkaessentials.manager;

import com.alkacode.alkaessentials.config.MessagesConfig;
import com.alkacode.alkaessentials.database.PunishmentRepository;
import com.alkacode.alkaessentials.model.Punishment;
import com.alkacode.alkaessentials.util.ChatUtil;
import com.alkacode.alkaessentials.util.DurationParser;
import com.alkacode.core.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Regras de negocio das punicoes: monta o modelo, persiste, consulta e revoga. */
public final class PunishmentManager {

    private final JavaPlugin plugin;
    private final PunishmentRepository repository;

    public PunishmentManager(JavaPlugin plugin, PunishmentRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public PunishmentRepository getRepository() {
        return repository;
    }

    /** Aplica uma punicao (banimento/silenciamento/aviso). duration: vazio/permanente => -1. */
    public Punishment apply(UUID target, String targetName, String type, String reason,
                            String proof, String issuer, String duration) {
        long endTime = DurationParser.isPermanent(duration)
                ? Punishment.PERMANENT
                : System.currentTimeMillis() + DurationParser.parse(duration) * 1000L;
        String server = plugin.getConfig().getString("punish.server", "Global");
        Punishment p = new Punishment(0, target, targetName, type, reason, proof, issuer, server,
                System.currentTimeMillis(), endTime, true);
        return repository.insert(p);
    }

    public boolean hasActiveBan(UUID target) {
        return !repository.getActive(target, "BAN").isEmpty();
    }

    public boolean hasActiveMute(UUID target) {
        return !repository.getActive(target, "MUTE").isEmpty();
    }

    public Punishment getActiveMute(UUID target) {
        List<Punishment> mutes = repository.getActive(target, "MUTE");
        return mutes.isEmpty() ? null : mutes.get(0);
    }

    /** Revoga o tipo (ban/mute) ativo do alvo. Retorna quantos foram revogados. */
    public int revoke(UUID target, String typeLike) {
        List<Punishment> active = repository.getActive(target, typeLike);
        for (Punishment p : active) {
            repository.setActive(p.getId(), false);
        }
        return active.size();
    }

    public void expireAll() {
        repository.expireActive();
    }

    public String consoleName() {
        return plugin.getConfig().getString("punish.console-name", "Servidor");
    }

    /** Aplica o efeito imediato de uma punicao (kick de ban/expulsao, aviso de mute). */
    public void applyImmediate(Punishment p) {
        Player online = Bukkit.getPlayer(p.getTarget());
        if (online == null) {
            return;
        }
        String reason = p.getReason() == null ? "" : p.getReason();
        if (p.isBan() || p.getType().equals("KICK")) {
            String time = p.isPermanent() ? "Permanente"
                    : TimeUtil.formatSeconds((p.getEndTime() - System.currentTimeMillis()) / 1000);
            String msg = p.isBan()
                    ? MessagesConfig.getInstance().get("punish-ban-kick", Map.of("reason", reason, "time", time))
                    : MessagesConfig.getInstance().get("punish-kick-reason", Map.of("reason", reason));
            online.kick(ChatUtil.parse(msg));
        } else if (p.isMute()) {
            String time = p.isPermanent() ? "Permanente"
                    : TimeUtil.formatSeconds((p.getEndTime() - System.currentTimeMillis()) / 1000);
            ChatUtil.sendKey(online, "punish-mute-notify", Map.of("reason", reason, "time", time));
        }
    }

    /** Anuncia a punicao no chat (respeitando a config de broadcasts). */
    public void broadcast(Punishment p, String typeName, String action, String issuer, String reason) {
        if (!plugin.getConfig().getBoolean("punish.broadcasts", true)) {
            return;
        }
        String permission = plugin.getConfig().getString("punish.broadcast-permission", "");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!permission.isBlank() && !online.hasPermission(permission)) {
                continue;
            }
            ChatUtil.sendKey(online, "punish-broadcast", Map.of(
                    "type", typeName, "player", p.getTargetName(),
                    "action", action, "issuer", issuer,
                    "reason", reason == null ? "" : reason));
        }
    }

    /** Conta warns ativos de um jogador (dentro da janela configurada). */
    public int countActiveWarns(UUID target) {
        return repository.getActive(target, "WARN").size();
    }

    /** Punição automatica apos exceder o maximo de avisos (warn.max-warns). Retorna a aplicada, ou null. */
    public Punishment autoPunishOnMaxWarns(UUID target, String targetName) {
        int max = plugin.getConfig().getInt("warn.max-warns", 0);
        if (max <= 0) {
            return null;
        }
        String type = plugin.getConfig().getString("warn.auto-type", "MUTE");
        String duration = plugin.getConfig().getString("warn.auto-duration", "");
        String reason = plugin.getConfig().getString("warn.auto-reason", "Excedeu o limite de avisos");
        Punishment p = apply(target, targetName, type, reason, "", "Sistema", duration);
        applyImmediate(p);
        return p;
    }

    /** Converte o nome de tipo (reasons.yml) para o tipo de banco. */
    public String dbType(String reasonType) {
        return reasonType == null ? "MUTE" : reasonType.toUpperCase();
    }
}
