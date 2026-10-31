package com.packpilot.generator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PackingRequest(
        @NotBlank String climate,
        @Min(1) @Max(60) int tripLengthDays,
        Integer adults,
        Integer children,
        Integer toddlers,
        List<ApiActivities> activities
) {
}
