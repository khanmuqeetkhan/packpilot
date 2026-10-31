package com.packpilot.generator.model;

public record Travelers(int adults, int children, int toddlers) {

    public int total() {
        return adults + children + toddlers;
    }

    public boolean hasNonAdult() {
        return children > 0 || toddlers > 0;
    }
}
