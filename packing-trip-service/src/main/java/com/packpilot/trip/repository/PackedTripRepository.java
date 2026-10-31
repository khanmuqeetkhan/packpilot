package com.packpilot.trip.repository;

import com.packpilot.trip.model.PackedTrip;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PackedTripRepository extends MongoRepository<PackedTrip, String> {

    List<PackedTrip> findByClimateIgnoreCase(String climate);

    List<PackedTrip> findByTripNameContainingIgnoreCase(String tripName);
}
