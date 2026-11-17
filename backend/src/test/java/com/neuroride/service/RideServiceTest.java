package com.neuroride.service;

import com.neuroride.domain.dto.NearbyDriverDto;
import com.neuroride.domain.dto.RideRequestDto;
import com.neuroride.domain.dto.RideResponseDto;
import com.neuroride.domain.entity.Ride;
import com.neuroride.domain.enums.RideStatus;
import com.neuroride.event.RideEventProducer;
import com.neuroride.repository.RideRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private DriverGeoService driverGeoService;
    @Mock private RideEventProducer eventProducer;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private RideService rideService;

    @BeforeEach
    void setUp() {
        rideService = new RideService(
                rideRepository, driverGeoService, eventProducer, messagingTemplate, new SimpleMeterRegistry()
        );
    }

    @Test
    void requestRide_matchesClosestDriverAndEmitsKafkaEvent() {
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        when(driverGeoService.findNearbyAvailableDrivers(anyDouble(), anyDouble(), eq(5.0), eq(10)))
                .thenReturn(List.of(new NearbyDriverDto(driverId.toString(), 0.8, 37.775, -122.419)));
        when(driverGeoService.acquireDriverLock(eq(driverId.toString()), anyString())).thenReturn(true);
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });

        RideRequestDto req = new RideRequestDto(
                riderId, 37.7749, -122.4194, 37.7897, -122.4014, "Market St", "Financial District"
        );

        RideResponseDto res = rideService.requestRide(req);

        assertThat(res.status()).isEqualTo(RideStatus.MATCHED);
        assertThat(res.driverId()).isEqualTo(driverId);
        assertThat(res.surgeMultiplier()).isEqualByComparingTo(new BigDecimal("1.45"));
        verify(driverGeoService).removeDriverFromPool(driverId.toString());
        verify(eventProducer).publishRideRequested(any());
    }
}