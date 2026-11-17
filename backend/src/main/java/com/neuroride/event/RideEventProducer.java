package com.neuroride.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${neuroride.kafka.topics.driver-location}")
    private String driverLocationTopic;

    @Value("${neuroride.kafka.topics.ride-requests}")
    private String rideRequestsTopic;

    @Value("${neuroride.kafka.topics.trip-completed}")
    private String tripCompletedTopic;

    public void publishDriverLocation(DriverLocationEvent event) {
        kafkaTemplate.send(driverLocationTopic, event.driverId(), event);
    }

    public void publishRideRequested(RideRequestedEvent event) {
        kafkaTemplate.send(rideRequestsTopic, event.rideId(), event)
                .whenComplete((res, ex) -> {
                    if (ex != null) log.error("Failed to publish RideRequestedEvent {}", event.rideId(), ex);
                });
    }

    public void publishTripCompleted(TripCompletedEvent event) {
        kafkaTemplate.send(tripCompletedTopic, event.rideId(), event)
                .whenComplete((res, ex) -> {
                    if (ex != null) log.error("Failed to publish TripCompletedEvent {}", event.rideId(), ex);
                });
    }
}