package com.packpilot.trip;

import com.packpilot.trip.client.GeneratorClient;
import com.packpilot.trip.client.GeneratorItem;
import com.packpilot.trip.client.GeneratorResponse;
import com.packpilot.trip.controller.PackingTripController;
import com.packpilot.trip.dto.GenerateDraftRequest;
import com.packpilot.trip.dto.ItemInput;
import com.packpilot.trip.dto.SaveTripRequest;
import com.packpilot.trip.dto.UpdateItemRequest;
import com.packpilot.trip.dto.UpdateTripRequest;
import com.packpilot.trip.model.PackedItem;
import com.packpilot.trip.model.PackedTrip;
import com.packpilot.trip.repository.PackedTripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PackingTripControllerTest {

    private PackedTripRepository repository;
    private GeneratorClient generatorClient;
    private PackingTripController controller;

    @BeforeEach
    void setUp() {
        repository = mock(PackedTripRepository.class);
        generatorClient = mock(GeneratorClient.class);
        controller = new PackingTripController(repository, generatorClient);
    }

    @Test
    void generateDraftDelegatesToGeneratorClientWithoutPersisting() {
        GeneratorResponse response = new GeneratorResponse(
                "mild", 5, 1, 0, 0, List.of(), List.of(new GeneratorItem("CLOTHING", "Socks", 5, null)), "packing-generator-service");
        when(generatorClient.generate(any())).thenReturn(response);

        ResponseEntity<GeneratorResponse> result = controller.generateDraft(
                new GenerateDraftRequest("mild", 5, 1, 0, 0, List.of()));

        assertThat(result.getBody()).isEqualTo(response);
        verifyNoInteractions(repository);
    }

    @Test
    void savePersistsTripWithGeneratedItemIdsAndReturns201() {
        when(repository.save(any(PackedTrip.class))).thenAnswer(invocation -> {
            PackedTrip trip = invocation.getArgument(0);
            trip.setId("trip-1");
            return trip;
        });

        SaveTripRequest request = new SaveTripRequest(
                null, "mild", 5, 1, 0, 0, List.of(),
                List.of(new ItemInput("CLOTHING", "Socks", 5, null)));

        ResponseEntity<PackedTrip> result = controller.save(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getId()).isEqualTo("trip-1");
        assertThat(result.getBody().getTripName()).isEqualTo("mild trip");
        assertThat(result.getBody().getItems()).hasSize(1);
        assertThat(result.getBody().getItems().get(0).getId()).isNotBlank();
        assertThat(result.getBody().getItems().get(0).isChecked()).isFalse();
    }

    @Test
    void updateItemTogglesCheckedAndClampsQuantity() {
        PackedItem item = new PackedItem("item-1", "CLOTHING", "Socks", 5, null, false);
        PackedTrip trip = new PackedTrip();
        trip.setId("trip-1");
        trip.setItems(new ArrayList<>(List.of(item)));
        when(repository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(repository.save(any(PackedTrip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PackedTrip updated = controller.updateItem("trip-1", "item-1", new UpdateItemRequest(true, -5, "worn once"));

        PackedItem updatedItem = updated.getItems().get(0);
        assertThat(updatedItem.isChecked()).isTrue();
        assertThat(updatedItem.getQuantity()).isZero();
        assertThat(updatedItem.getNote()).isEqualTo("worn once");
    }

    @Test
    void removeItemDeletesItByIdAndThrowsWhenMissing() {
        PackedItem item = new PackedItem("item-1", "CLOTHING", "Socks", 5, null, false);
        PackedTrip trip = new PackedTrip();
        trip.setId("trip-1");
        trip.setItems(new ArrayList<>(List.of(item)));
        when(repository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(repository.save(any(PackedTrip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PackedTrip updated = controller.removeItem("trip-1", "item-1");
        assertThat(updated.getItems()).isEmpty();

        assertThatThrownBy(() -> controller.removeItem("trip-1", "item-1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void renameUpdatesTripName() {
        PackedTrip trip = new PackedTrip();
        trip.setId("trip-1");
        trip.setTripName("old name");
        when(repository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(repository.save(any(PackedTrip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PackedTrip renamed = controller.rename("trip-1", new UpdateTripRequest("Tokyo with the kids"));

        assertThat(renamed.getTripName()).isEqualTo("Tokyo with the kids");
    }

    @Test
    void getUnknownIdThrowsNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void listWithoutFiltersReturnsAll() {
        when(repository.findAll()).thenReturn(List.of(new PackedTrip()));

        List<PackedTrip> trips = controller.list(null, null);

        assertThat(trips).hasSize(1);
        verify(repository).findAll();
    }
}
