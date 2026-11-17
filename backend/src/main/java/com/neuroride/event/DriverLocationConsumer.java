package com.neuroride.event;

import com.neuroride.domain.enums.DriverStatus;
import com.neuroride.repository.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverLocationConsumer {

    private final DriverProfileRepository driverProfileRepository;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    @KafkaListener(topics = "${neuroride.kafka.topics.driver-location}", groupId = "neuroride-postgis-sync")
    public void consumeLocationUpdate(DriverLocationEvent event) {
        UUID driverId = UUID.fromString(event.driverId());
        driverProfileRepository.findById(driverId).ifPresent(profile -> {
            Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(event.lon(), event.lat()));
            profile.setLastLocation(point);
            profile.setLastSeenAt(event.timestamp());
            profile.setStatus(DriverStatus.valueOf(event.status()));
            driverProfileRepository.save(profile);
        });
    }
}