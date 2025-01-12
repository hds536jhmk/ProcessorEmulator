package io.github.marco4413.pemu.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;

public final class StringUtils {

    public static class SpecialCharacters {
        public static final char ESCAPE_CHARACTER = '\\';
        public static final String ESCAPE_CHARACTER_STR = String.valueOf(ESCAPE_CHARACTER);
        public static final HashMap<Character, Character> MAP = new HashMap<>();
        public static final HashMap<Character, Character> INVERSE_MAP = new HashMap<>();

        static {
            MAP.put('\'', '\''); // Quotes
            MAP.put('\"', '\"'); // Double quotes
            MAP.put('\\', '\\'); // Backslash
            MAP.put('t' , '\t'); // Tab character
            MAP.put('b' , '\b'); // Backspace
            MAP.put('r' , '\r'); // Carriage return
            MAP.put('f' , '\f'); // Form feed
            MAP.put('n' , '\n'); // Newline
            MAP.put('0' , '\0'); // NULL

            MAP.forEach((k, v) -> INVERSE_MAP.put(v, k));
        }

        public static boolean isSpecialCharacter(int codePoint) {
            return INVERSE_MAP.containsKey((char) codePoint);
        }

        public static boolean isSpecialCharacter(char c) {
            return INVERSE_MAP.containsKey(c);
        }

        public static @NotNull String escapeAll(@NotNull String str) {
            return escapeAll(str, ESCAPE_CHARACTER);
        }

        public static @NotNull String escapeAll(@NotNull String str, char escapeChar) {
            StringBuilder escapedStr = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char currentChar = str.charAt(i);
                if (INVERSE_MAP.containsKey(currentChar)) {
                    escapedStr.append(escapeChar).append(INVERSE_MAP.get(currentChar));
                } else escapedStr.append(currentChar);
            }
            return escapedStr.toString();
        }

        public static @NotNull String escapeAll(char character) {
            return escapeAll(character, ESCAPE_CHARACTER);
        }

        public static @NotNull String escapeAll(char character, char escapeChar) {
            if (INVERSE_MAP.containsKey(character))
                return String.valueOf(escapeChar) + INVERSE_MAP.get(character);
            return String.valueOf(character);
        }
    }

    public static int parseInt(@NotNull String str) {
        return (int) parseLong(str);
    }

    public static long parseLong(@NotNull String str) {
        int radix = 10;
        if (str.startsWith("0x")) radix = 16;
        else if (str.startsWith("0o")) radix =  8;
        else if (str.startsWith("0b")) radix =  2;
        if (radix != 10) str = str.substring(2);
        return Long.parseLong(str, radix);
    }

    public static int compareVersions(@NotNull String version1, @NotNull String version2) {
        int[] version1Components = Arrays.stream(version1.split("\\.")).mapToInt(StringUtils::parseInt).toArray();
        int[] version2Components = Arrays.stream(version2.split("\\.")).mapToInt(StringUtils::parseInt).toArray();

        for (int i = 0; i < Math.max(version1Components.length, version2Components.length); i++) {
            int v1Component = i < version1Components.length ? version1Components[i] : 0;
            int v2Component = i < version2Components.length ? version2Components[i] : 0;

            if (v1Component > v2Component) return 1;
            else if (v1Component < v2Component) return -1;
        }

        return 0;
    }

    public static @NotNull String format(@Nullable String str, @Nullable Object... formats) {
        if (str == null) return "";
        if (formats.length == 0) return str;

        final char BEGIN_FORMAT = '{';
        final char END_FORMAT = '}';

        StringBuilder result = new StringBuilder(str.length());
        boolean isGettingIndex = false;
        int formatIndex = -1;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // If we're getting a format Index
            if (isGettingIndex) {
                // If we've got to the end of the format Index
                if (c == END_FORMAT) {
                    // We're not getting the index anymore
                    isGettingIndex = false;
                    // If the Index is within the specified formats
                    if (formatIndex >= 0 && formatIndex < formats.length)
                        // Append the Format
                        result.append(formats[formatIndex]);
                    else {
                        // Else if it's not a valid Format we put the index back in the String
                        result
                            .append(BEGIN_FORMAT)
                            .append(formatIndex >= 0 ? formatIndex : "")
                            .append(END_FORMAT);
                    }
                // If the current char is a digit
                } else if (Character.isDigit(c)) {
                    // Update the Index
                    if (formatIndex < 0)
                        formatIndex = 0;
                    else formatIndex *= 10;
                    formatIndex += c - '0';
                } else {
                    // If it's not a valid digit and the format didn't end put everything back
                    isGettingIndex = false;
                    result
                        .append(BEGIN_FORMAT)
                        .append(formatIndex >= 0 ? formatIndex : "")
                        .append(c);
                }
            // If we're starting to get an Index
            } else if (c == BEGIN_FORMAT) {
                formatIndex = -1;
                isGettingIndex = true;
            // Else put the char in the String
            } else result.append(c);
        }

        return result.toString();
    }

    public static @NotNull String toShortName(@NotNull String name) {
        StringBuilder shortName = new StringBuilder();
        String[] words = name.split("\\s+");
        for (String word : words) {
            shortName.append( word.charAt(0) );
        }

        return shortName.toString().toUpperCase();
    }

    public static @NotNull String stackTraceAsString(@NotNull Throwable err) {
        StringWriter str = new StringWriter();
        err.printStackTrace(new PrintWriter(str));
        return str.toString().trim();
    }

    private static final DecimalFormat ENG_FORMAT = new DecimalFormat("##0E00");
    private static final String[] ENG_NOTATION_PREFIXES = new String[] {
            "E-24", "E-21", "E-18", "E-15", "E-12", "E-09", "E-06", "E-03", "E00", "E03", "E06", "E09", "E12", "E15", "E18", "E21", "E24",
            "y"   , "z"   , "a"   , "f"   , "p"   , "n"   , "u"   , "m"   , ""   , "k"  , "M"  , "G"  , "T"  , "P"  , "E"  , "Z"  , "Y"
    };
    private static final int ENG_NOTATION_PREFIXES_START = ENG_NOTATION_PREFIXES.length / 2;

    public static @NotNull String getEngNotation(@NotNull Number number) {
        return getEngNotation(number, "");
    }

    public static @NotNull String getEngNotation(@NotNull Number number, @NotNull String measureUnit) {
        String engineeringNumber = ENG_FORMAT.format(number);

        for (int i = 0; i < ENG_NOTATION_PREFIXES_START; i++) {
            if (engineeringNumber.contains(ENG_NOTATION_PREFIXES[i])) {
                return engineeringNumber.replace(
                        ENG_NOTATION_PREFIXES[i],
                        ENG_NOTATION_PREFIXES[i + ENG_NOTATION_PREFIXES_START]
                ) + measureUnit;
            }
        }

        return engineeringNumber + measureUnit;
    }

    private static final HashMap<Character, String> HTML_TAG_CHARACTERS = new HashMap<>();
    static {
        HTML_TAG_CHARACTERS.put('&', "&amp;");
        HTML_TAG_CHARACTERS.put('<', "&lt;");
        HTML_TAG_CHARACTERS.put('>', "&gt;");
    }

    /**
     * This method escapes the following HTML Characters: &amp;, &lt;, &gt;<br>
     * This was tested on an i7 9750H and got an average of 208.37ns for the following string: "&lt;html&gt;&lt;/html&gt;"
     * so performance shouldn't be a concern
     * @param str The String to escape
     * @return The escaped String
     */
    public static @NotNull String escapeHTML(@NotNull String str) {
        StringBuilder escapedString = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            escapedString.append(HTML_TAG_CHARACTERS.getOrDefault(c, String.valueOf(c)));
        }
        return escapedString.toString();
    }
}
