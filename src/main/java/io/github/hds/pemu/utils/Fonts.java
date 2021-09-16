package io.github.hds.pemu.utils;

import io.github.hds.pemu.Main;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public final class Fonts {
    public static final String JetBrainsMono = importTTFont("/assets/JetBrainsMono.ttf");

    private static @NotNull String importTTFont(@NotNull String resourcePath) {
        InputStream fontStream = Main.class.getResourceAsStream(resourcePath);
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font.getFamily();
        } catch (Exception ignored) { }
        return new JLabel().getFont().getFamily();
    }
}
