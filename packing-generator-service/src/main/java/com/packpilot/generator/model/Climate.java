package com.packpilot.generator.model;

import java.util.Locale;

public enum Climate {
    COLD,
    MILD,
    WARM,
    TROPICAL,
    VARIABLE;

    public static Climate parse(String raw) {
        try {
            return Climate.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown climate '" + raw + "'. Choose one of: cold, mild, warm, tropical, variable.");
        }
    }
}
