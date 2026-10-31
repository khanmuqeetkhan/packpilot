package com.packpilot.trip.dto;

import java.util.List;

public record GenerateDraftRequest(
        String climate, Integer tripLengthDays, Integer adults, Integer children, Integer toddlers, List<String> activities
) {
}
