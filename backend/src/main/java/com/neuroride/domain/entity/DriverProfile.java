package com.neuroride.domain.entity;

import com.neuroride.domain.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "vehicle_make", nullable = false)
    private String vehicleMake;

    @Column(name = "vehicle_model", nullable = false)
    private String vehicleModel;

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "driver_status")
    private DriverStatus status;

    @Column(name = "last_location", columnDefinition = "geography(Point,4326)")
    private Point lastLocation;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}