flowchart TB
    subgraph Clients["Client Layer (Next.js App Router + Tailwind)"]
        Rider["Rider App\n(REST + STOMP/WS)"]
        Driver["Driver App\n(REST + STOMP/WS)"]
    end

    subgraph Edge["Gateway / Observability"]
        Sentry["Sentry SDK\n(Errors & Traces)"]
    end

    subgraph Core["Spring Boot Backend (WebSockets/STOMP + REST)"]
        WS["STOMP Broker\n(/topic/ride.*, /app/location)"]
        Match["Matching & Dispatch\nEngine"]
        Metrics["Spring Actuator\n(/actuator/prometheus)"]
    end

    subgraph FastPath["Hot Path Cache & Geo"]
        Redis[("Redis 7\nGEOADD / GEOSEARCH\nDriver Status & Locks")]
    end

    subgraph Durable["System of Record"]
        PG[("PostgreSQL 16 + PostGIS\nTrips, Users, Spatial History")]
    end

    subgraph Streaming["Apache Kafka (KRaft)"]
        T1[["driver-location-updates\n(Key: driverId)"]]
        T2[["ride-requests\n(Key: rideId)"]]
        T3[["trip-completed\n(Key: rideId)"]]
    end

    subgraph AI["AI Enrichment Worker"]
        GeminiSvc["Gemini Insights Consumer\n(Surge, Route & Safety AI)"]
        GeminiAPI(("Google Gemini API"))
    end

    subgraph Mon["Observability Stack"]
        Prom["Prometheus"]
        Graf["Grafana Dashboards"]
    end

    %% Hot Path Flows
    Driver -- "1. GPS Ping (2-5s via STOMP)" --> WS
    WS -- "2. GEOADD + TTL" --> Redis
    WS -- "3. Async Telemetry" --> T1

    Rider -- "4. Request Ride (REST)" --> Match
    Match -- "5. GEOSEARCH (Radius/Count)" --> Redis
    Match -- "6. Persist Trip (PENDING)" --> PG
    Match -- "7. Publish Event" --> T2
    Match -- "8. Push Offer (STOMP)" --> Driver

    %% Trip Completion & AI
    Driver -- "9. Complete Trip (REST)" --> Match
    Match -- "10. Update Trip (COMPLETED)" --> PG
    Match -- "11. Publish Event" --> T3

    T2 & T3 -- "12. Consume Events" --> GeminiSvc
    GeminiSvc -- "13. Prompt w/ Geo & Context" --> GeminiAPI
    GeminiSvc -- "14. Store Insights" --> PG
    GeminiSvc -- "15. Push AI Insight (STOMP)" --> WS
    WS -- "16. Live Updates & AI Cards" --> Rider

    %% Telemetry
    Metrics -. "Scrape" .-> Prom --> Graf
    Core & Clients -. "Exceptions" .-> Sentry