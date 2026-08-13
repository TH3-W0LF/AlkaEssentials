package com.alkacode.alkaessentials.scoreboard;

import java.util.List;

/** Seleciona o frame atual de uma animacao (lista de frames + intervalo em segundos). */
public final class Animation {

    private Animation() {
    }

    public static String frame(List<String> frames, double interval, long elapsedMillis) {
        if (frames == null || frames.isEmpty()) {
            return "";
        }
        if (frames.size() == 1) {
            return frames.get(0);
        }
        double stepMs = Math.max(0.05, interval) * 1000.0;
        int index = (int) ((elapsedMillis / stepMs) % frames.size());
        return frames.get(Math.max(0, index));
    }
}
