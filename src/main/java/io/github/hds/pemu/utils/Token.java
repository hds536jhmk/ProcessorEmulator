package io.github.hds.pemu.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class Token {

    private final boolean SHOULD_QUOTE;
    public final String PATTERN;

    public Token(@NotNull String pattern) {
        this(pattern, false);
    }

    public Token(@NotNull String pattern, boolean quotePattern) {
        SHOULD_QUOTE = quotePattern;
        PATTERN = pattern;
    }

    public @NotNull String getPattern() {
        if (SHOULD_QUOTE) return Pattern.quote(PATTERN);
        else return PATTERN;
    }

    public boolean equals(@Nullable String str) {
        if (str == null) return false;
        return str.matches("[" + getPattern() + "]+");
    }

}
