// RideResponseDto.java
package com.neuroride.domain.dto;
import com.neuroride.domain.enums.RideStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RideResponseDto(
    UUID rideId,
    UUID riderId,
    UUID driverId,
    RideStatus status,
    BigDecimal surgeMultiplier,
    BigDecimal estimatedFareUsd,
    BigDecimal finalFareUsd,
    Integer distanceMeters,
    Instant requestedAt
) {}