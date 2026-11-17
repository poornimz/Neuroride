package com.neuroride.controller;

import com.neuroride.domain.dto.NearbyDriverDto;
import com.neuroride.domain.dto.RideRequestDto;
import com.neuroride.domain.dto.RideResponseDto;
import com.neuroride.service.DriverGeoService;
import com.neuroride.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final DriverGeoService driverGeoService;

    @PostMapping("/rides")
    public ResponseEntity<RideResponseDto> requestRide(@Valid @RequestBody RideRequestDto request) {
        return ResponseEntity.ok(rideService.requestRide(request));
    }

    @PostMapping("/rides/{rideId}/start")
    public ResponseEntity<RideResponseDto> startRide(@PathVariable UUID rideId) {
        return ResponseEntity.ok(rideService.startRide(rideId));
    }

    @PostMapping("/rides/{rideId}/complete")
    public ResponseEntity<RideResponseDto> completeRide(@PathVariable UUID rideId) {
        return ResponseEntity.ok(rideService.completeRide(rideId));
    }

    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<NearbyDriverDto>> findNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        return ResponseEntity.ok(driverGeoService.findNearbyAvailableDrivers(lat, lon, radiusKm, 15));
    }
}