package com.packpilot.trip.controller;

import com.packpilot.trip.client.GeneratorClient;
import com.packpilot.trip.client.GeneratorRequest;
import com.packpilot.trip.client.GeneratorResponse;
import com.packpilot.trip.dto.GenerateDraftRequest;
import com.packpilot.trip.dto.ItemInput;
import com.packpilot.trip.dto.SaveTripRequest;
import com.packpilot.trip.dto.UpdateItemRequest;
import com.packpilot.trip.dto.UpdateTripRequest;
import com.packpilot.trip.model.PackedItem;
import com.packpilot.trip.model.PackedTrip;
import com.packpilot.trip.repository.PackedTripRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/packing-lists")
public class PackingTripController {

    private final PackedTripRepository repository;
    private final GeneratorClient generatorClient;

    public PackingTripController(PackedTripRepository repository, GeneratorClient generatorClient) {
        this.repository = repository;
        this.generatorClient = generatorClient;
    }

    @PostMapping("/generate")
    public ResponseEntity<GeneratorResponse> generateDraft(@RequestBody GenerateDraftRequest request) {
        GeneratorResponse response = generatorClient.generate(new GeneratorRequest(
                request.climate(),
                request.tripLengthDays() != null ? request.tripLengthDays() : 1,
                request.adults(), request.children(), request.toddlers(), request.activities()
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PackedTrip> save(@Valid @RequestBody SaveTripRequest request) {
        PackedTrip trip = new PackedTrip();
        trip.setTripName(blankToDefault(request.tripName(), request.climate()));
        trip.setClimate(request.climate());
        trip.setTripLengthDays(request.tripLengthDays());
        trip.setAdults(request.adults());
        trip.setChildren(request.children());
        trip.setToddlers(request.toddlers());
        trip.setActivities(request.activities() == null ? List.of() : request.activities());
        trip.setItems(request.items().stream().map(this::toPackedItem).toList());
        trip.setCreatedAt(Instant.now());
        PackedTrip saved = repository.save(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<PackedTrip> list(
            @RequestParam(required = false) String climate,
            @RequestParam(required = false) String search) {
        if (climate != null && !climate.isBlank()) {
            return repository.findByClimateIgnoreCase(climate);
        }
        if (search != null && !search.isBlank()) {
            return repository.findByTripNameContainingIgnoreCase(search);
        }
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public PackedTrip get(@PathVariable String id) {
        return findTripOrThrow(id);
    }

    @PutMapping("/{id}")
    public PackedTrip rename(@PathVariable String id, @RequestBody UpdateTripRequest request) {
        PackedTrip trip = findTripOrThrow(id);
        if (request.tripName() != null && !request.tripName().isBlank()) {
            trip.setTripName(request.tripName());
        }
        return repository.save(trip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No trip with id " + id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public PackedTrip addItem(@PathVariable String id, @RequestBody ItemInput item) {
        PackedTrip trip = findTripOrThrow(id);
        trip.getItems().add(toPackedItem(item));
        return repository.save(trip);
    }

    @PutMapping("/{id}/items/{itemId}")
    public PackedTrip updateItem(
            @PathVariable String id, @PathVariable String itemId, @RequestBody UpdateItemRequest request) {
        PackedTrip trip = findTripOrThrow(id);
        PackedItem item = trip.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No item with id " + itemId));
        if (request.checked() != null) {
            item.setChecked(request.checked());
        }
        if (request.quantity() != null) {
            item.setQuantity(Math.max(0, request.quantity()));
        }
        if (request.note() != null) {
            item.setNote(request.note());
        }
        return repository.save(trip);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public PackedTrip removeItem(@PathVariable String id, @PathVariable String itemId) {
        PackedTrip trip = findTripOrThrow(id);
        boolean removed = trip.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No item with id " + itemId);
        }
        return repository.save(trip);
    }

    private PackedTrip findTripOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No trip with id " + id));
    }

    private PackedItem toPackedItem(ItemInput input) {
        return new PackedItem(UUID.randomUUID().toString(), input.category(), input.name(), input.quantity(), input.note(), false);
    }

    private String blankToDefault(String tripName, String climate) {
        if (tripName != null && !tripName.isBlank()) {
            return tripName;
        }
        return (climate == null ? "Untitled" : climate) + " trip";
    }
}
