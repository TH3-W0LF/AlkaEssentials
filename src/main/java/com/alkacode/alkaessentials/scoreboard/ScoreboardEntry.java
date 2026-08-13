package com.alkacode.alkaessentials.scoreboard;

import java.util.List;

/** Modelo de uma linha (ou titulo) de scoreboard: frames animados + intervalo + condicao. */
public final class ScoreboardEntry {

    private final List<String> frames;
    private final double interval;
    private final String condition;

    public ScoreboardEntry(List<String> frames, double interval, String condition) {
        this.frames = frames;
        this.interval = interval;
        this.condition = condition == null ? "" : condition;
    }

    public List<String> getFrames() {
        return frames;
    }

    public double getInterval() {
        return interval;
    }

    public String getCondition() {
        return condition;
    }

    public boolean hasCondition() {
        return !condition.isBlank();
    }
}
