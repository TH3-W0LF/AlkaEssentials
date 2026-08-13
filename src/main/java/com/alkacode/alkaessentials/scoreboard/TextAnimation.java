package com.alkacode.alkaessentials.scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processa tags de animacao em uma string resolvida e expande em varios frames:
 * <ul>
 *   <li>&lt;rainbow:N&gt;texto&lt;/rainbow&gt; - N frames, cada um um gradiente MiniMessage
 *       entre duas cores do arco-iris (anima com o interval da linha).</li>
 *   <li>&lt;scroll:left|right&gt;texto&lt;/scroll&gt; - frames revelando o texto aos poucos.</li>
 *   <li>&lt;centralize&gt;texto&lt;/centralize&gt; - centraliza o texto com espacos.</li>
 * </ul>
 * O resultado e sempre MiniMessage valido (o rainbow vira &lt;gradient:#C1:#C2&gt;).
 */
public final class TextAnimation {

    private static final String[] LIGHT_COLORS = {
            "FF0000", "FF4500", "FFA500", "FFFF00", "ADFF2F",
            "00FF00", "00CED1", "4682B4", "4169E1", "800080"};

    private static final Pattern RAINBOW = Pattern.compile("<rainbow:(\\d+)>(.*?)</rainbow>", Pattern.DOTALL);
    private static final Pattern SCROLL = Pattern.compile("<scroll:(\\w+)>(.*?)</scroll>", Pattern.DOTALL);
    private static final Pattern CENTRALIZE = Pattern.compile("<centralize>(.*?)</centralize>", Pattern.DOTALL);

    private TextAnimation() {
    }

    /** Expande tags de animacao em uma lista de frames. Se nao houver tag, retorna [texto]. */
    public static List<String> expand(String text) {
        List<String> frames = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            frames.add("");
            return frames;
        }
        Matcher rainbow = RAINBOW.matcher(text);
        if (rainbow.find()) {
            int count = Integer.parseInt(rainbow.group(1));
            String content = rainbow.group(2);
            for (String frame : rainbowFrames(content, count)) {
                frames.add(text.replaceFirst(Pattern.quote(rainbow.group()), Matcher.quoteReplacement(frame)));
            }
            return frames;
        }
        Matcher scroll = SCROLL.matcher(text);
        if (scroll.find()) {
            boolean left = scroll.group(1).equalsIgnoreCase("left");
            String content = scroll.group(2);
            for (String frame : scrollFrames(text, scroll.group(), content, left)) {
                frames.add(frame);
            }
            return frames;
        }
        Matcher centralize = CENTRALIZE.matcher(text);
        if (centralize.find()) {
            frames.add(text.replaceFirst(Pattern.quote(centralize.group()),
                    Matcher.quoteReplacement(center(centralize.group(2)))));
            return frames;
        }
        frames.add(text);
        return frames;
    }

    private static List<String> rainbowFrames(String text, int frames) {
        List<String> result = new ArrayList<>();
        int count = Math.max(1, frames);
        for (int i = 0; i < count; i++) {
            String c1 = LIGHT_COLORS[i % LIGHT_COLORS.length];
            String c2 = LIGHT_COLORS[(i + 1) % LIGHT_COLORS.length];
            result.add("<gradient:#" + c1.toLowerCase(Locale.ROOT) + ":#" + c2.toLowerCase(Locale.ROOT) + ">"
                    + text + "</gradient>");
        }
        return result;
    }

    private static List<String> scrollFrames(String line, String fullTag, String content, boolean left) {
        List<String> result = new ArrayList<>();
        int length = content.length();
        for (int i = 1; i <= length; i++) {
            String partial = content.substring(0, i);
            result.add(line.replace(fullTag, partial));
        }
        if (!left) {
            java.util.Collections.reverse(result);
        }
        return result;
    }

    private static String center(String text) {
        int width = 32;
        int spaces = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(spaces) + text;
    }
}
