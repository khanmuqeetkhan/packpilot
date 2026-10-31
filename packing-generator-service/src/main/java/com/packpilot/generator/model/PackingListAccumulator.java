package com.packpilot.generator.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Merges same category+name items instead of duplicating them.
public class PackingListAccumulator {

    private final Map<String, PackingItem> itemsByKey = new LinkedHashMap<>();

    public void add(Category category, String name, int quantity, String note) {
        String key = category + "::" + name;
        PackingItem existing = itemsByKey.get(key);
        if (existing == null) {
            itemsByKey.put(key, new PackingItem(category, name, quantity, note));
            return;
        }
        int mergedQuantity = existing.quantity() + quantity;
        String mergedNote = combineNotes(existing.note(), note);
        itemsByKey.put(key, new PackingItem(category, name, mergedQuantity, mergedNote));
    }

    private String combineNotes(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        if (first.equals(second)) {
            return first;
        }
        return first + "; " + second;
    }

    public List<PackingItem> items() {
        return List.copyOf(itemsByKey.values());
    }
}
