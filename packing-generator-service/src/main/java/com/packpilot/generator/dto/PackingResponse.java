package com.packpilot.generator.dto;

import java.util.List;

public record PackingResponse(
        String climate,
        int tripLengthDays,
        int adults,
        int children,
        int toddlers,
        List<String> activities,
        List<PackingItemDto> items,
        String generatedBy
) {
}
