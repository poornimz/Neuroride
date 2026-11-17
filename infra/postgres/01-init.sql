CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE user_role AS ENUM ('RIDER', 'DRIVER');
CREATE TYPE driver_status AS ENUM ('OFFLINE', 'AVAILABLE', 'ON_TRIP');
CREATE TYPE ride_status AS ENUM (
    'REQUESTED', 'MATCHED', 'DRIVER_ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(30)  NOT NULL UNIQUE,
    role            user_role    NOT NULL,
    rating          NUMERIC(3,2) NOT NULL DEFAULT 5.00,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE driver_profiles (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    vehicle_make    VARCHAR(60)  NOT NULL,
    vehicle_model   VARCHAR(60)  NOT NULL,
    license_plate   VARCHAR(20)  NOT NULL UNIQUE,
    status          driver_status NOT NULL DEFAULT 'OFFLINE',
    last_location   GEOGRAPHY(Point, 4326),
    last_seen_at    TIMESTAMPTZ
);

CREATE INDEX idx_driver_profiles_geo ON driver_profiles USING GIST (last_location);
CREATE INDEX idx_driver_profiles_status ON driver_profiles (status);

CREATE TABLE rides (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rider_id            UUID NOT NULL REFERENCES users(id),
    driver_id           UUID REFERENCES users(id),
    status              ride_status NOT NULL DEFAULT 'REQUESTED',
    pickup_location     GEOGRAPHY(Point, 4326) NOT NULL,
    dropoff_location    GEOGRAPHY(Point, 4326) NOT NULL,
    pickup_address      TEXT,
    dropoff_address     TEXT,
    surge_multiplier    NUMERIC(4,2) NOT NULL DEFAULT 1.00,
    estimated_fare_usd  NUMERIC(10,2) NOT NULL,
    final_fare_usd      NUMERIC(10,2),
    distance_meters     INTEGER,
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ
);

CREATE INDEX idx_rides_rider ON rides (rider_id, requested_at DESC);
CREATE INDEX idx_rides_driver ON rides (driver_id, requested_at DESC);
CREATE INDEX idx_rides_status ON rides (status) WHERE status NOT IN ('COMPLETED', 'CANCELLED');
CREATE INDEX idx_rides_pickup_geo ON rides USING GIST (pickup_location);

CREATE TABLE ride_ai_insights (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ride_id         UUID NOT NULL REFERENCES rides(id) ON DELETE CASCADE,
    insight_type    VARCHAR(50) NOT NULL, -- e.g. 'SURGE_EXPLANATION', 'SAFETY_STOPOVER', 'ROUTE_SUMMARY'
    summary_text    TEXT NOT NULL,
    raw_metadata    JSONB,
    model_version   VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ride_ai_insights_ride ON ride_ai_insights (ride_id);