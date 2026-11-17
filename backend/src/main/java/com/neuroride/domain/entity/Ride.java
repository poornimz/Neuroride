package com.neuroride.domain.entity;

import com.neuroride.domain.enums.RideStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rides")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "ride_status")
    private RideStatus status;

    @Column(name = "pickup_location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point pickupLocation;

    @Column(name = "dropoff_location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point dropoffLocation;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "dropoff_address")
    private String dropoffAddress;

    @Column(name = "surge_multiplier", nullable = false)
    private BigDecimal surgeMultiplier;

    @Column(name = "estimated_fare_usd", nullable = false)
    private BigDecimal estimatedFareUsd;

    @Column(name = "final_fare_usd")
    private BigDecimal finalFareUsd;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}