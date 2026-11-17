package com.neuroride.controller;

import com.neuroride.domain.dto.GpsUpdateMessage;
import com.neuroride.event.DriverLocationEvent;
import com.neuroride.event.RideEventProducer;
import com.neuroride.service.DriverGeoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LocationWebSocketController {

    private final DriverGeoService driverGeoService;
    private final RideEventProducer eventProducer;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/driver.location")
    public void handleGpsPing(@Valid GpsUpdateMessage msg) {
        String driverId = msg.driverId().toString();
        String status = msg.status() != null ? msg.status() : "AVAILABLE";

        // 1. Hot-path Redis GEOADD + 30s heartbeat TTL
        driverGeoService.updateDriverLocation(driverId, msg.lat(), msg.lon(), msg.heading(), status);

        // 2. If driver is on an active trip, stream coordinates directly to Rider UI
        if (msg.activeRideId() != null) {
            messagingTemplate.convertAndSend("/topic/ride." + msg.activeRideId() + ".location", Map.of(
                    "driverId", driverId,
                    "lat", msg.lat(),
                    "lon", msg.lon(),
                    "heading", msg.heading(),
                    "timestamp", Instant.now().toString()
            ));
        }

        // 3. Emit asynchronously to Kafka for PostGIS sync & telemetry
        eventProducer.publishDriverLocation(new DriverLocationEvent(
                driverId, msg.lat(), msg.lon(), msg.heading(), status, Instant.now()
        ));
    }
}