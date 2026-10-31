package com.packpilot.generator.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ApiActivities {
    BEACH,
    HIKING,
    BUSINESS,
    WINTER_SPORTS,
    FORMAL_EVENT;

    @JsonCreator
    public static ApiActivities fromValue(String raw) {
        try {
            return ApiActivities.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown activity '" + raw + "'. Choose from: beach, hiking, business, winter_sports, formal_event.");
        }
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
