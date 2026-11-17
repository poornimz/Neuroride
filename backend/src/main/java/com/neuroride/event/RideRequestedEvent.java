// RideRequestedEvent.java
package com.neuroride.event;
import java.math.BigDecimal;
import java.time.Instant;

public record RideRequestedEvent(
    String rideId, String riderId, String assignedDriverId,
    double pickupLat, double pickupLon, double dropoffLat, double dropoffLon,
    BigDecimal surgeMultiplier, BigDecimal estimatedFareUsd,
    int nearbyDriversCount, Instant requestedAt
) {}