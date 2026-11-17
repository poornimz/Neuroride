'use client';

import React, { useState, useEffect } from 'react';
import { Activity, Play, CheckCircle2, Radio } from 'lucide-react';
import { useRideSocket } from '@/hooks/useRideSocket';
import LiveRadarMap from '@/components/LiveRadarMap';
import AiInsightsPanel from '@/components/AiInsightsPanel';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

// Demo UUIDs (seed matching rows in users/driver_profiles for local testing)
const DEMO_RIDER_ID = '11111111-1111-1111-1111-111111111111';
const DEMO_DRIVER_ID = '22222222-2222-2222-2222-222222222222';

export default function NeuroRideDashboard() {
  const [pickup] = useState({ lat: 37.7749, lon: -122.4194, address: 'Market St, SF' });
  const [dropoff] = useState({ lat: 37.7897, lon: -122.4014, address: 'Financial District, SF' });

  const [ride, setRide] = useState<any>(null);
  const [nearbyDrivers, setNearbyDrivers] = useState<any[]>([]);
  const [simulatingDriver, setSimulatingDriver] = useState(false);
  const [driverCoords, setDriverCoords] = useState({ lat: 37.7725, lon: -122.4220 });

  const { connected, driverLocation, rideStatusUpdate, insights, sendDriverPing, clearInsights } =
    useRideSocket(ride?.rideId || null);

  useEffect(() => {
    if (rideStatusUpdate) setRide(rideStatusUpdate);
  }, [rideStatusUpdate]);

  // Poll nearby available drivers from Redis Geo
  const refreshNearby = async () => {
    const res = await fetch(`${API_BASE}/drivers/nearby?lat=${pickup.lat}&lon=${pickup.lon}&radiusKm=5`);
    if (res.ok) setNearbyDrivers(await res.json());
  };

  // Simulated Driver GPS loop (emits STOMP frames every 2s)
  useEffect(() => {
    if (!simulatingDriver || !connected) return;
    const interval = setInterval(() => {
      setDriverCoords((prev) => {
        const next = {
          lat: prev.lat + (pickup.lat - prev.lat) * 0.15,
          lon: prev.lon + (pickup.lon - prev.lon) * 0.15,
        };
        sendDriverPing({
          driverId: DEMO_DRIVER_ID,
          activeRideId: ride?.rideId || null,
          lat: next.lat,
          lon: next.lon,
          heading: 45,
          status: ride && ride.status !== 'COMPLETED' ? 'ON_TRIP' : 'AVAILABLE',
        });
        return next;
      });
    }, 2000);
    return () => clearInterval(interval);
  }, [simulatingDriver, connected, ride, pickup, sendDriverPing]);

  const handleRequestRide = async () => {
    clearInsights();
    const res = await fetch(`${API_BASE}/rides`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        riderId: DEMO_RIDER_ID,
        pickupLat: pickup.lat,
        pickupLon: pickup.lon,
        dropoffLat: dropoff.lat,
        dropoffLon: dropoff.lon,
        pickupAddress: pickup.address,
        dropoffAddress: dropoff.address,
      }),
    });
    if (res.ok) setRide(await res.json());
  };

  const handleTransition = async (action: 'start' | 'complete') => {
    if (!ride?.rideId) return;
    const res = await fetch(`${API_BASE}/rides/${ride.rideId}/${action}`, { method: 'POST' });
    if (res.ok) setRide(await res.json());
  };

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header */}
        <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight bg-gradient-to-r from-sky-400 to-violet-400 bg-clip-text text-transparent">
              NeuroRide Distributed Dispatch
            </h1>
            <p className="text-xs text-slate-400">
              Spring Boot STOMP • Redis Geo • PostGIS • Apache Kafka • Gemini AI
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className={`text-xs px-3 py-1 rounded-full flex items-center gap-1.5 border ${
              connected
                ? 'bg-emerald-950/60 border-emerald-700 text-emerald-300'
                : 'bg-rose-950/60 border-rose-700 text-rose-300'
            }`}>
              <Radio className="w-3 h-3" />
              {connected ? 'STOMP Connected' : 'Connecting...'}
            </span>
            <button
              onClick={() => setSimulatingDriver((v) => !v)}
              className={`text-xs px-3 py-1.5 rounded-lg font-medium border transition ${
                simulatingDriver
                  ? 'bg-amber-500/20 border-amber-500 text-amber-300'
                  : 'bg-slate-900 border-slate-700 text-slate-300 hover:bg-slate-800'
              }`}
            >
              {simulatingDriver ? 'Stop Driver GPS Ping' : 'Simulate Online Driver'}
            </button>
          </div>
        </header>

        {/* Main Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left 2 Cols: Radar & Controls */}
          <div className="lg:col-span-2 space-y-4">
            <LiveRadarMap
              pickup={pickup}
              dropoff={dropoff}
              nearbyDrivers={nearbyDrivers}
              liveDriver={driverLocation}
            />

            <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 flex flex-wrap items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="text-xs text-slate-400">Trip State</div>
                <div className="text-sm font-semibold flex items-center gap-2">
                  <Activity className="w-4 h-4 text-sky-400" />
                  {ride ? `${ride.status} • Surge ${ride.surgeMultiplier}x • $${ride.estimatedFareUsd}` : 'Idle'}
                </div>
              </div>

              <div className="flex flex-wrap gap-2">
                <button
                  onClick={refreshNearby}
                  className="px-3 py-2 text-xs rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700"
                >
                  Scan Redis Geo
                </button>
                <button
                  onClick={handleRequestRide}
                  className="px-4 py-2 text-xs font-semibold rounded-lg bg-sky-600 hover:bg-sky-500 text-white"
                >
                  Request Ride
                </button>
                {ride?.status === 'MATCHED' && (
                  <button
                    onClick={() => handleTransition('start')}
                    className="px-3 py-2 text-xs font-semibold rounded-lg bg-emerald-600 hover:bg-emerald-500 flex items-center gap-1"
                  >
                    <Play className="w-3 h-3" /> Start Trip
                  </button>
                )}
                {ride?.status === 'IN_PROGRESS' && (
                  <button
                    onClick={() => handleTransition('complete')}
                    className="px-3 py-2 text-xs font-semibold rounded-lg bg-violet-600 hover:bg-violet-500 flex items-center gap-1"
                  >
                    <CheckCircle2 className="w-3 h-3" /> Complete Trip
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Right Col: Gemini AI Panel */}
          <AiInsightsPanel insights={insights} />
        </div>
      </div>
    </main>
  );
}