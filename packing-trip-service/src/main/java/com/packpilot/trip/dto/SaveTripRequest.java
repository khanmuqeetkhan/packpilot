package com.packpilot.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveTripRequest(
        String tripName,
        @NotBlank String climate,
        int tripLengthDays,
        int adults,
        int children,
        int toddlers,
        List<String> activities,
        @NotEmpty List<ItemInput> items
) {
}
