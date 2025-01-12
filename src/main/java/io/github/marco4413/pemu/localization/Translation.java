package io.github.marco4413.pemu.localization;

import io.github.marco4413.pemu.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;

public final class Translation {

    protected final HashMap<String, String> MAP;

    public Translation() {
        this(new HashMap<>());
    }

    public Translation(@NotNull HashMap<String, String> translationMap) {
        MAP = translationMap;
    }

    public static @NotNull Translation mergeTranslations(@NotNull Translation... translations) {
        Translation mergedTranslation = new Translation();
        for (int i = translations.length - 1; i >= 0; i--)
            mergedTranslation.MAP.putAll(translations[i].MAP);
        return mergedTranslation;
    }

    public @NotNull Translation merge(@NotNull Translation... others) {
        Translation mergedOthers = mergeTranslations(others);
        mergedOthers.MAP.putAll(this.MAP);
        return mergedOthers;
    }

    public @NotNull String getName() {
        return getOrDefault("_longName", getOrDefault("_shortName"));
    }

    public @NotNull String getLongName() {
        return getOrDefault("_longName");
    }

    public @NotNull String getShortName() {
        return getOrDefault("_shortName");
    }

    public @Nullable String get(@NotNull String key) {
        return MAP.get(key);
    }

    public @NotNull String getOrDefault(@NotNull String key) {
        return MAP.getOrDefault(key, key);
    }

    public @NotNull String getOrDefault(@NotNull String key, @NotNull String defaultValue) {
        return MAP.getOrDefault(key, defaultValue);
    }

    public void translateFrame(@NotNull String key, @NotNull JFrame frame, @Nullable Object... formats) {
        frame.setTitle(StringUtils.format(
                getOrDefault(key + "._title"), formats
        ));
    }

    public void translateComponent(@NotNull String key, @NotNull JLabel component, @Nullable Object... formats) {
        component.setText(StringUtils.format(
                getOrDefault(key + "._text"), formats
        ));
    }

    public void translateComponent(@NotNull String key, @NotNull AbstractButton component, @Nullable Object... formats) {
        component.setText(StringUtils.format(
                getOrDefault(key + "._text"), formats
        ));
        component.setMnemonic(getOrDefault(key + "._mnemonic", String.valueOf(KeyEvent.CHAR_UNDEFINED)).charAt(0));
    }

    @Override
    public String toString() {
        return getName();
    }
}
