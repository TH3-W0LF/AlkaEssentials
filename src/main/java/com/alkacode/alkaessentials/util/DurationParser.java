package com.alkacode.alkaessentials.util;

/** Converte duracoes escritas (30s, 5m, 2h, 1d, 1w) em segundos. */
public final class DurationParser {

    private DurationParser() {
    }

    /** Retorna duracao em segundos, ou -1 se invalida/vazia. Suporta s/m/h/d/w (ex: "5h", "30m", "1d"). */
    public static long parse(String input) {
        if (input == null || input.isBlank()) {
            return -1;
        }
        String s = input.trim().toLowerCase();
        try {
            if (s.endsWith("s")) {
                return Long.parseLong(s.substring(0, s.length() - 1));
            }
            if (s.endsWith("m")) {
                return Long.parseLong(s.substring(0, s.length() - 1)) * 60L;
            }
            if (s.endsWith("h")) {
                return Long.parseLong(s.substring(0, s.length() - 1)) * 3600L;
            }
            if (s.endsWith("d")) {
                return Long.parseLong(s.substring(0, s.length() - 1)) * 86400L;
            }
            if (s.endsWith("w")) {
                return Long.parseLong(s.substring(0, s.length() - 1)) * 604800L;
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static boolean isPermanent(String input) {
        return input == null || input.isBlank();
    }
}
