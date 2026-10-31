package com.packpilot.trip.client;

import java.util.List;

public record GeneratorRequest(
        String climate, int tripLengthDays, Integer adults, Integer children, Integer toddlers, List<String> activities
) {
}
