package com.freebuff.signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Morse code utilities: text -> pattern, pattern symbol corruption.
 * Patterns use '.' and '-' with a single space between letters and " / " between words.
 */
public final class Morse {

    private static final Map<Character, String> ENCODE = new HashMap<>();
    private static final Map<String, Character> DECODE = new HashMap<>();

    static {
        String[][] table = {
                {"A", ".-"}, {"B", "-..."}, {"C", "-.-."}, {"D", "-.."}, {"E", "."}, {"F", "..-."},
                {"G", "--."}, {"H", "...."}, {"I", ".."}, {"J", ".---"}, {"K", "-.-"}, {"L", ".-.."},
                {"M", "--"}, {"N", "-."}, {"O", "---"}, {"P", ".--."}, {"Q", "--.-"}, {"R", ".-."},
                {"S", "..."}, {"T", "-"}, {"U", "..-"}, {"V", "...-"}, {"W", ".--"}, {"X", "-..-"},
                {"Y", "-.--"}, {"Z", "--.."},
                {"0", "-----"}, {"1", ".----"}, {"2", "..---"}, {"3", "...--"}, {"4", "....-"},
                {"5", "....."}, {"6", "-...."}, {"7", "--..."}, {"8", "---.."}, {"9", "----."},
                {".", ".-.-.-"}, {",", "--..--"}, {"?", "..--.."}, {"'", ".----."}, {"!", "-.-.--"},
                {"/", "-..-."}, {"(", "-.--."}, {")", "-.--.-"}, {"&", ".-..."}, {":", "---..."},
                {";", "-.-.-."}, {"=", "-...-"}, {"+", ".-.-."}, {"-", "-....-"}, {"_", "..--.-"},
                {"\"", ".-..-."}, {"$", "...-..-"}, {"@", ".--.-."}
        };
        for (String[] p : table) {
            ENCODE.put(p[0].charAt(0), p[1]);
            DECODE.put(p[1], p[0].charAt(0));
        }
    }

    private Morse() {}

    /** Encode text to a morse pattern. Words separated by " / ". */
    public static String patternFor(String text) {
        StringBuilder sb = new StringBuilder();
        String upper = text.toUpperCase(Locale.ROOT);
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c == ' ') {
                sb.append(" / ");
                continue;
            }
            String p = ENCODE.get(c);
            if (p == null) continue;
            String cur = sb.toString();
            if (cur.length() > 0 && !cur.endsWith(" / ") && !cur.endsWith(" ")) sb.append(' ');
            sb.append(p);
        }
        return sb.toString().trim();
    }

    /** Flip a single dot/dash symbol in the pattern (deterministic for a given seed). */
    public static String flipOneSymbol(String pattern, long seed) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '.' || c == '-') idx.add(i);
        }
        if (idx.isEmpty()) return pattern;
        Random r = new Random(seed);
        int pos = idx.get(Math.floorMod(r.nextInt(), idx.size()));
        char[] ch = pattern.toCharArray();
        ch[pos] = ch[pos] == '.' ? '-' : '.';
        return new String(ch);
    }

    /** Decode a morse pattern back to text (used for server logging/tests). */
    public static String patternToText(String pattern) {
        StringBuilder sb = new StringBuilder();
        String[] words = pattern.trim().split("\\s*/\\s*");
        boolean firstWord = true;
        for (String word : words) {
            if (!firstWord) sb.append(' ');
            firstWord = false;
            String[] letters = word.trim().split("\\s+");
            for (String letter : letters) {
                Character c = DECODE.get(letter);
                sb.append(c == null ? '?' : c);
            }
        }
        return sb.toString();
    }
}