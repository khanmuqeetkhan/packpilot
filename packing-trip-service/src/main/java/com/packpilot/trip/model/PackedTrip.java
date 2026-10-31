package com.packpilot.trip.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "trips")
public class PackedTrip {

    @Id
    private String id;

    private String tripName;

    @Indexed
    private String climate;

    private int tripLengthDays;

    private int adults;

    private int children;

    private int toddlers;

    @Indexed
    private List<String> activities;

    private List<PackedItem> items;

    private Instant createdAt;

    public PackedTrip() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getClimate() {
        return climate;
    }

    public void setClimate(String climate) {
        this.climate = climate;
    }

    public int getTripLengthDays() {
        return tripLengthDays;
    }

    public void setTripLengthDays(int tripLengthDays) {
        this.tripLengthDays = tripLengthDays;
    }

    public int getAdults() {
        return adults;
    }

    public void setAdults(int adults) {
        this.adults = adults;
    }

    public int getChildren() {
        return children;
    }

    public void setChildren(int children) {
        this.children = children;
    }

    public int getToddlers() {
        return toddlers;
    }

    public void setToddlers(int toddlers) {
        this.toddlers = toddlers;
    }

    public List<String> getActivities() {
        return activities;
    }

    public void setActivities(List<String> activities) {
        this.activities = activities;
    }

    public List<PackedItem> getItems() {
        return items;
    }

    public void setItems(List<PackedItem> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
