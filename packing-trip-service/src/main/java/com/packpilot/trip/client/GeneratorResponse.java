package com.packpilot.trip.client;

import java.util.List;

public record GeneratorResponse(
        String climate,
        int tripLengthDays,
        int adults,
        int children,
        int toddlers,
        List<String> activities,
        List<GeneratorItem> items,
        String generatedBy
) {
}
