package com.neuroride.service;

import com.neuroride.domain.dto.NearbyDriverDto;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DriverGeoService {

    private static final String GEO_KEY = "drivers:geo:active";
    private static final String META_KEY_PREFIX = "driver:meta:";
    private static final String LOCK_KEY_PREFIX = "lock:driver:";
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);
    private static final Duration LOCK_TTL = Duration.ofSeconds(15);

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public void updateDriverLocation(String driverId, double lat, double lon, double heading, String status) {
        if ("AVAILABLE".equalsIgnoreCase(status)) {
            redisTemplate.opsForGeo().add(GEO_KEY, new Point(lon, lat), driverId);
        } else {
            redisTemplate.opsForGeo().remove(GEO_KEY, driverId);
        }

        String metaKey = META_KEY_PREFIX + driverId;
        redisTemplate.opsForHash().putAll(metaKey, Map.of(
                "status", status,
                "lat", String.valueOf(lat),
                "lon", String.valueOf(lon),
                "heading", String.valueOf(heading),
                "updatedAt", Instant.now().toString()
        ));
        redisTemplate.expire(metaKey, HEARTBEAT_TTL);
    }

    public List<NearbyDriverDto> findNearbyAvailableDrivers(double lat, double lon, double radiusKm, int limit) {
        return meterRegistry.timer("neuroride.geo.search.latency").record(() -> {
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                    GEO_KEY,
                    GeoReference.fromCoordinate(lon, lat),
                    new Distance(radiusKm, Metrics.KILOMETERS),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                            .includeDistance()
                            .includeCoordinates()
                            .sortAscending()
                            .limit(limit)
            );

            if (results == null) return List.of();

            List<NearbyDriverDto> available = new ArrayList<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
                String driverId = result.getContent().getName();
                Boolean hasHeartbeat = redisTemplate.hasKey(META_KEY_PREFIX + driverId);

                if (Boolean.FALSE.equals(hasHeartbeat)) {
                    // Lazy-prune disconnected drivers from the Geo index
                    redisTemplate.opsForGeo().remove(GEO_KEY, driverId);
                    continue;
                }

                Point coords = result.getContent().getPoint();
                available.add(new NearbyDriverDto(
                        driverId,
                        result.getDistance().getValue(),
                        coords.getY(),
                        coords.getX()
                ));
            }
            return available;
        });
    }

    public boolean acquireDriverLock(String driverId, String rideId) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY_PREFIX + driverId, rideId, LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseDriverLock(String driverId) {
        redisTemplate.delete(LOCK_KEY_PREFIX + driverId);
    }

    public void removeDriverFromPool(String driverId) {
        redisTemplate.opsForGeo().remove(GEO_KEY, driverId);
    }
}