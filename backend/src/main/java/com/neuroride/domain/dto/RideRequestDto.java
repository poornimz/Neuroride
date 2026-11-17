// RideRequestDto.java
package com.neuroride.domain.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RideRequestDto(
    @NotNull UUID riderId,
    double pickupLat,
    double pickupLon,
    double dropoffLat,
    double dropoffLon,
    String pickupAddress,
    String dropoffAddress
) {}
