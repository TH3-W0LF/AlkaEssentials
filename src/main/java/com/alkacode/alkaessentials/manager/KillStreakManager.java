package com.alkacode.alkaessentials.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sequencia de mortes por jogador, em memoria (zera quando o servidor reinicia - mesma
 * filosofia leve de plugins como AxKills, sem precisar de tabela no banco pra isso).
 * onKill incrementa e devolve a sequencia atual do assassino; onDeath zera a da vitima
 * e devolve o valor que ela tinha ANTES de zerar, pro DeathListener anunciar o fim de
 * uma sequencia relevante.
 */
public final class KillStreakManager {

    private final Map<UUID, Integer> streaks = new ConcurrentHashMap<>();

    public int onKill(UUID killer) {
        return streaks.merge(killer, 1, Integer::sum);
    }

    public int onDeath(UUID victim) {
        Integer previous = streaks.remove(victim);
        return previous == null ? 0 : previous;
    }

    public int get(UUID uuid) {
        return streaks.getOrDefault(uuid, 0);
    }
}
