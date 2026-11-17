// GpsUpdateMessage.java
package com.neuroride.domain.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GpsUpdateMessage(
    @NotNull UUID driverId,
    UUID activeRideId,
    double lat,
    double lon,
    double heading,
    String status // AVAILABLE or ON_TRIP
) {}
