package com.packpilot.generator;

import com.packpilot.generator.dto.ApiActivities;
import com.packpilot.generator.dto.PackingItemDto;
import com.packpilot.generator.dto.PackingRequest;
import com.packpilot.generator.dto.PackingResponse;
import com.packpilot.generator.service.PackingGenerationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackingGenerationServiceTest {

    private final PackingGenerationService service = new PackingGenerationService();

    @Test
    void defaultsToOneAdultAndNoActivities() {
        PackingRequest request = new PackingRequest("mild", 5, null, null, null, null);
        PackingResponse result = service.generate(request);

        assertThat(result.adults()).isEqualTo(1);
        assertThat(result.children()).isZero();
        assertThat(result.toddlers()).isZero();
        assertThat(result.activities()).isEmpty();
        assertThat(result.items()).isNotEmpty();
        assertThat(findItem(result, "Light jacket")).isPresent();
    }

    @Test
    void toddlerAddsDiapersScaledByTripLength() {
        PackingRequest request = new PackingRequest("mild", 4, 1, 0, 1, null);
        PackingResponse result = service.generate(request);

        PackingItemDto diapers = findItem(result, "Diapers").orElseThrow();
        assertThat(diapers.quantity()).isEqualTo(4 * 6);
    }

    @Test
    void tropicalClimateAndBeachActivityMergeSwimwearQuantityInsteadOfDuplicating() {
        PackingRequest request = new PackingRequest("tropical", 7, 2, 0, 0, List.of(ApiActivities.BEACH));
        PackingResponse result = service.generate(request);

        long swimwearEntries = result.items().stream().filter(i -> i.name().equals("Swimwear")).count();
        PackingItemDto swimwear = findItem(result, "Swimwear").orElseThrow();

        assertThat(swimwearEntries).isEqualTo(1);
        assertThat(swimwear.quantity()).isEqualTo(2 + 2); // once from climate, once from activity, 2 travelers each
    }

    @Test
    void businessTravelOnlyPacksFormalWearForAdultsNotChildren() {
        PackingRequest request = new PackingRequest("mild", 3, 1, 1, 0, List.of(ApiActivities.BUSINESS));
        PackingResponse result = service.generate(request);

        PackingItemDto formal = findItem(result, "Formal outfit").orElseThrow();
        assertThat(formal.quantity()).isEqualTo(1); // only the one adult, not the child
    }

    @Test
    void coldClimateAndWinterSportsMergeThermalLayers() {
        PackingRequest request = new PackingRequest("cold", 5, 1, 0, 0, List.of(ApiActivities.WINTER_SPORTS));
        PackingResponse result = service.generate(request);

        long thermalEntries = result.items().stream().filter(i -> i.name().equals("Thermal base layers")).count();
        assertThat(thermalEntries).isEqualTo(1);
    }

    @Test
    void unknownClimateIsRejected() {
        PackingRequest request = new PackingRequest("arctic-ish", 3, 1, 0, 0, null);
        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownActivityValueIsRejectedAtTheApiBoundary() {
        assertThatThrownBy(() -> ApiActivities.fromValue("space travel")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activityValueParsingIsCaseAndSeparatorInsensitive() {
        assertThat(ApiActivities.fromValue("Winter-Sports")).isEqualTo(ApiActivities.WINTER_SPORTS);
        assertThat(ApiActivities.fromValue("formal event")).isEqualTo(ApiActivities.FORMAL_EVENT);
    }

    private Optional<PackingItemDto> findItem(PackingResponse response, String name) {
        return response.items().stream().filter(i -> i.name().equals(name)).findFirst();
    }
}
