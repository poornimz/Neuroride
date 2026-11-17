import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '20s', target: 15 }, // Ramp up to 15 concurrent riders
    { duration: '40s', target: 35 }, // Sustained dispatch load
    { duration: '15s', target: 0 },  // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<250'], // 95% of requests under 250ms
    errors: ['rate<0.01'],            // <1% error rate
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080/api/v1';
const RIDER_ID = '11111111-1111-1111-1111-111111111111';

export default function () {
  // Slight coordinate jitter around San Francisco
  const latJitter = (Math.random() - 0.5) * 0.02;
  const lonJitter = (Math.random() - 0.5) * 0.02;

  // 1. Sub-millisecond Redis Geo driver scan
  const geoRes = http.get(
    `${BASE_URL}/drivers/nearby?lat=${37.7749 + latJitter}&lon=${-122.4194 + lonJitter}&radiusKm=5`
  );
  const geoOk = check(geoRes, { 'geo scan 200': (r) => r.status === 200 });
  errorRate.add(!geoOk);

  // 2. Request a ride (Postgres write + Redis lock + Kafka publish)
  const payload = JSON.stringify({
    riderId: RIDER_ID,
    pickupLat: 37.7749 + latJitter,
    pickupLon: -122.4194 + lonJitter,
    dropoffLat: 37.7897,
    dropoffLon: -122.4014,
    pickupAddress: 'Market St, SF',
    dropoffAddress: 'Financial District, SF',
  });

  const rideRes = http.post(`${BASE_URL}/rides`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  const rideOk = check(rideRes, { 'ride created 200': (r) => r.status === 200 });
  errorRate.add(!rideOk);

  if (rideOk) {
    const rideId = rideRes.json('rideId');
    // 3. Complete ride to exercise Kafka trip-completed -> AI worker pipeline
    const completeRes = http.post(`${BASE_URL}/rides/${rideId}/complete`);
    const completeOk = check(completeRes, { 'ride completed 200': (r) => r.status === 200 });
    errorRate.add(!completeOk);
  }

  sleep(0.5);
}