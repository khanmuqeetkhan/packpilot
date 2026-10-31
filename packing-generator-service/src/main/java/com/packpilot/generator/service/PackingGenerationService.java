package com.packpilot.generator.service;

import com.packpilot.generator.dto.ApiActivities;
import com.packpilot.generator.dto.PackingItemDto;
import com.packpilot.generator.dto.PackingRequest;
import com.packpilot.generator.dto.PackingResponse;
import com.packpilot.generator.model.Activity;
import com.packpilot.generator.model.Category;
import com.packpilot.generator.model.Climate;
import com.packpilot.generator.model.PackingListAccumulator;
import com.packpilot.generator.model.Travelers;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PackingGenerationService {

    public PackingResponse generate(PackingRequest request) {
        Climate climate = Climate.parse(request.climate());
        int tripLengthDays = request.tripLengthDays();
        Travelers travelers = new Travelers(
                orDefault(request.adults(), 1),
                orDefault(request.children(), 0),
                orDefault(request.toddlers(), 0)
        );
        Set<Activity> activities = toModelActivities(request.activities());

        PackingListAccumulator acc = new PackingListAccumulator();

        addBaseEssentials(acc, tripLengthDays, travelers);
        addClimateItems(acc, climate, travelers);
        activities.forEach(activity -> addActivityItems(acc, activity, travelers));

        List<PackingItemDto> items = acc.items().stream()
                .map(item -> new PackingItemDto(item.category().name(), item.name(), item.quantity(), item.note()))
                .toList();

        return new PackingResponse(
                climate.name().toLowerCase(),
                tripLengthDays,
                travelers.adults(),
                travelers.children(),
                travelers.toddlers(),
                activities.stream().map(a -> a.name().toLowerCase()).sorted().toList(),
                items,
                "packing-generator-service"
        );
    }

    private int orDefault(Integer value, int fallback) {
        return value != null && value >= 0 ? value : fallback;
    }

    private Set<Activity> toModelActivities(List<ApiActivities> raw) {
        if (raw == null) {
            return Set.of();
        }
        return raw.stream()
                .map(apiActivity -> Activity.valueOf(apiActivity.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void addBaseEssentials(PackingListAccumulator acc, int tripLengthDays, Travelers travelers) {
        int cappedDays = Math.min(tripLengthDays, 7);
        int grownUps = travelers.adults() + travelers.children();
        int everyone = travelers.total();

        if (grownUps > 0) {
            acc.add(Category.CLOTHING, "Underwear", cappedDays * grownUps, "per adult/child, laundry assumed after day 7");
            acc.add(Category.CLOTHING, "Socks", cappedDays * grownUps, null);
            acc.add(Category.CLOTHING, "T-shirts/tops", (ceilDiv(cappedDays, 2) + 1) * grownUps, null);
            acc.add(Category.CLOTHING, "Trousers/shorts", (ceilDiv(cappedDays, 3) + 1) * grownUps, null);
            acc.add(Category.CLOTHING, "Sleepwear", (tripLengthDays <= 3 ? 1 : 2) * grownUps, null);
        }

        if (everyone > 0) {
            acc.add(Category.TOILETRIES, "Toothbrush and toothpaste", everyone, null);
            acc.add(Category.TOILETRIES, "Travel-size toiletries kit", everyone, "shampoo, soap, deodorant");
            acc.add(Category.DOCUMENTS, "Passport or ID", everyone, null);
            acc.add(Category.ELECTRONICS, "Phone charger", everyone, null);
            acc.add(Category.MISC, "Reusable water bottle", everyone, null);
        }

        acc.add(Category.DOCUMENTS, "Travel insurance documents", 1, "per trip");
        acc.add(Category.DOCUMENTS, "Tickets/boarding passes", 1, "per trip");
        acc.add(Category.ELECTRONICS, "Power bank", 1, "per trip");

        if (travelers.children() > 0) {
            acc.add(Category.MISC, "Comfort item/favorite toy", travelers.children(), "per child");
            acc.add(Category.ELECTRONICS, "Kids' entertainment (tablet/books)", travelers.children(), null);
        }

        if (travelers.toddlers() > 0) {
            acc.add(Category.MISC, "Comfort item/favorite toy", travelers.toddlers(), "per toddler");
            acc.add(Category.TOILETRIES, "Diapers", tripLengthDays * 6 * travelers.toddlers(), null);
            acc.add(Category.TOILETRIES, "Baby wipes", ceilDiv(tripLengthDays, 3) * travelers.toddlers(), null);
            acc.add(Category.CLOTHING, "Spare outfits for accidents", 2 * travelers.toddlers(), "per toddler");
        }
    }

    private void addClimateItems(PackingListAccumulator acc, Climate climate, Travelers travelers) {
        int everyone = travelers.total();
        if (everyone == 0) {
            return;
        }
        switch (climate) {
            case COLD -> {
                acc.add(Category.CLOTHING, "Winter coat", everyone, null);
                acc.add(Category.CLOTHING, "Thermal base layers", everyone * 2, "for cold climate");
                acc.add(Category.CLOTHING, "Hat, gloves, and scarf", everyone, null);
                acc.add(Category.CLOTHING, "Warm socks", everyone * 2, null);
            }
            case MILD -> {
                acc.add(Category.CLOTHING, "Light jacket", everyone, null);
                acc.add(Category.MISC, "Umbrella", 1, "per trip");
            }
            case WARM -> {
                acc.add(Category.TOILETRIES, "Sunscreen", 1, "per trip");
                acc.add(Category.CLOTHING, "Sunglasses", everyone, null);
                acc.add(Category.CLOTHING, "Sun hat", everyone, null);
            }
            case TROPICAL -> {
                acc.add(Category.TOILETRIES, "Sunscreen (high SPF)", 1, "per trip");
                acc.add(Category.TOILETRIES, "Insect repellent", 1, "per trip");
                acc.add(Category.CLOTHING, "Swimwear", everyone, "for tropical climate");
                acc.add(Category.MISC, "Rain poncho", 1, "per trip, tropical climates are often rainy");
            }
            case VARIABLE -> {
                acc.add(Category.CLOTHING, "Light jacket", everyone, null);
                acc.add(Category.CLOTHING, "Light sweater", everyone, null);
                acc.add(Category.MISC, "Umbrella", 1, "per trip, mixed/variable weather");
            }
        }
    }

    private void addActivityItems(PackingListAccumulator acc, Activity activity, Travelers travelers) {
        int everyone = travelers.total();
        int adults = travelers.adults();
        if (everyone == 0) {
            return;
        }
        switch (activity) {
            case BEACH -> {
                acc.add(Category.CLOTHING, "Swimwear", everyone, "for beach activities");
                acc.add(Category.MISC, "Beach towel", everyone, null);
                acc.add(Category.CLOTHING, "Flip-flops/sandals", everyone, null);
                acc.add(Category.ELECTRONICS, "Waterproof phone pouch", 1, "per trip");
            }
            case HIKING -> {
                acc.add(Category.CLOTHING, "Hiking boots", everyone, null);
                acc.add(Category.MISC, "Daypack", everyone, null);
                acc.add(Category.MISC, "First-aid kit", 1, "per trip");
                acc.add(Category.TOILETRIES, "Insect repellent", 1, "per trip, for hiking");
            }
            case BUSINESS -> {
                if (adults > 0) {
                    acc.add(Category.CLOTHING, "Formal outfit", adults, null);
                    acc.add(Category.CLOTHING, "Dress shoes", adults, null);
                    acc.add(Category.ELECTRONICS, "Laptop and charger", adults, null);
                }
                acc.add(Category.MISC, "Notebook and pen", 1, "per trip");
            }
            case WINTER_SPORTS -> {
                acc.add(Category.CLOTHING, "Ski/snow gloves", everyone, null);
                acc.add(Category.CLOTHING, "Ski goggles", everyone, null);
                acc.add(Category.CLOTHING, "Winter boots", everyone, null);
                acc.add(Category.CLOTHING, "Thermal base layers", everyone, "for winter sports");
            }
            case FORMAL_EVENT -> {
                if (adults > 0) {
                    acc.add(Category.CLOTHING, "Formal outfit", adults, "for a formal event");
                    acc.add(Category.CLOTHING, "Dress shoes", adults, "for a formal event");
                    acc.add(Category.MISC, "Accessories (jewelry/tie/belt)", adults, null);
                }
            }
        }
    }

    private int ceilDiv(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }
}
