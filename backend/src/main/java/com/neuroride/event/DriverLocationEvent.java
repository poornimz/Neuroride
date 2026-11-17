// DriverLocationEvent.java
package com.neuroride.event;
import java.time.Instant;

public record DriverLocationEvent(
    String driverId, double lat, double lon, double heading, String status, Instant timestamp
) {}
