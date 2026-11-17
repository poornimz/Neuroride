package com.neuroride.service;

import com.neuroride.domain.dto.NearbyDriverDto;
import com.neuroride.domain.dto.RideRequestDto;
import com.neuroride.domain.dto.RideResponseDto;
import com.neuroride.domain.entity.Ride;
import com.neuroride.domain.enums.RideStatus;
import com.neuroride.event.RideEventProducer;
import com.neuroride.event.RideRequestedEvent;
import com.neuroride.event.TripCompletedEvent;
import com.neuroride.repository.RideRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final DriverGeoService driverGeoService;
    private final RideEventProducer eventProducer;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public RideResponseDto requestRide(RideRequestDto dto) {
        List<NearbyDriverDto> nearbyDrivers = driverGeoService.findNearbyAvailableDrivers(
                dto.pickupLat(), dto.pickupLon(), 5.0, 10
        );

        int distanceMeters = calculateHaversineMeters(
                dto.pickupLat(), dto.pickupLon(), dto.dropoffLat(), dto.dropoffLon()
        );
        BigDecimal surge = computeDynamicSurge(nearbyDrivers.size());
        BigDecimal estimatedFare = calculateFare(distanceMeters, surge);

        Ride ride = Ride.builder()
                .riderId(dto.riderId())
                .status(RideStatus.REQUESTED)
                .pickupLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(dto.pickupLon(), dto.pickupLat())))
                .dropoffLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(dto.dropoffLon(), dto.dropoffLat())))
                .pickupAddress(dto.pickupAddress())
                .dropoffAddress(dto.dropoffAddress())
                .surgeMultiplier(surge)
                .estimatedFareUsd(estimatedFare)
                .distanceMeters(distanceMeters)
                .requestedAt(Instant.now())
                .build();

        ride = rideRepository.save(ride);

        // Attempt atomic lock on closest available driver
        UUID matchedDriverId = null;
        for (NearbyDriverDto candidate : nearbyDrivers) {
            if (driverGeoService.acquireDriverLock(candidate.driverId(), ride.getId().toString())) {
                matchedDriverId = UUID.fromString(candidate.driverId());
                ride.setDriverId(matchedDriverId);
                ride.setStatus(RideStatus.MATCHED);
                ride = rideRepository.save(ride);
                driverGeoService.removeDriverFromPool(candidate.driverId());

                // Push dispatch offer to driver via STOMP
                messagingTemplate.convertAndSend("/topic/driver." + matchedDriverId + ".offers", toDto(ride));
                break;
            }
        }

        meterRegistry.counter("neuroride.rides.requested",
                "matched", String.valueOf(matchedDriverId != null)).increment();

        RideResponseDto response = toDto(ride);

        // Emit Kafka event for async AI surge explanation & analytics
        eventProducer.publishRideRequested(new RideRequestedEvent(
                ride.getId().toString(),
                ride.getRiderId().toString(),
                matchedDriverId != null ? matchedDriverId.toString() : null,
                dto.pickupLat(), dto.pickupLon(), dto.dropoffLat(), dto.dropoffLon(),
                surge, estimatedFare, nearbyDrivers.size(), ride.getRequestedAt()
        ));

        return response;
    }

    @Transactional
    public RideResponseDto startRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found: " + rideId));
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        ride = rideRepository.save(ride);

        RideResponseDto dto = toDto(ride);
        messagingTemplate.convertAndSend("/topic/ride." + rideId, dto);
        return dto;
    }

    @Transactional
    public RideResponseDto completeRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found: " + rideId));

        Instant now = Instant.now();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(now);
        ride.setFinalFareUsd(ride.getEstimatedFareUsd());
        ride = rideRepository.save(ride);

        if (ride.getDriverId() != null) {
            driverGeoService.releaseDriverLock(ride.getDriverId().toString());
        }

        long durationSeconds = ride.getStartedAt() != null
                ? Duration.between(ride.getStartedAt(), now).getSeconds()
                : 600L;

        RideResponseDto dto = toDto(ride);
        messagingTemplate.convertAndSend("/topic/ride." + rideId, dto);

        // Emit trip-completed for Gemini AI post-trip insights
        eventProducer.publishTripCompleted(new TripCompletedEvent(
                ride.getId().toString(),
                ride.getRiderId().toString(),
                ride.getDriverId() != null ? ride.getDriverId().toString() : null,
                ride.getPickupLocation().getY(), ride.getPickupLocation().getX(),
                ride.getDropoffLocation().getY(), ride.getDropoffLocation().getX(),
                ride.getPickupAddress(), ride.getDropoffAddress(),
                ride.getDistanceMeters(), durationSeconds,
                ride.getSurgeMultiplier(), ride.getFinalFareUsd(), now
        ));

        meterRegistry.counter("neuroride.rides.completed").increment();
        return dto;
    }

    private BigDecimal computeDynamicSurge(int nearbySupply) {
        if (nearbySupply == 0) return new BigDecimal("1.80");
        if (nearbySupply <= 2) return new BigDecimal("1.45");
        if (nearbySupply <= 5) return new BigDecimal("1.15");
        return new BigDecimal("1.00");
    }

    private BigDecimal calculateFare(int distanceMeters, BigDecimal surge) {
        BigDecimal baseFare = new BigDecimal("3.00");
        BigDecimal perKm = new BigDecimal("1.65");
        BigDecimal km = BigDecimal.valueOf(distanceMeters / 1000.0);
        return baseFare.add(perKm.multiply(km)).multiply(surge).setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return (int) (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private RideResponseDto toDto(Ride r) {
        return new RideResponseDto(
                r.getId(), r.getRiderId(), r.getDriverId(), r.getStatus(),
                r.getSurgeMultiplier(), r.getEstimatedFareUsd(), r.getFinalFareUsd(),
                r.getDistanceMeters(), r.getRequestedAt()
        );
    }
}