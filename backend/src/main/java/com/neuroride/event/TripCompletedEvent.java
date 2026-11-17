// TripCompletedEvent.java
package com.neuroride.event;
import java.math.BigDecimal;
import java.time.Instant;

public record TripCompletedEvent(
    String rideId, String riderId, String driverId,
    double pickupLat, double pickupLon, double dropoffLat, double dropoffLon,
    String pickupAddress, String dropoffAddress,
    int distanceMeters, long durationSeconds,
    BigDecimal surgeMultiplier, BigDecimal finalFareUsd, Instant completedAt
) {}