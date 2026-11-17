'use client';

import React from 'react';
import { Navigation, MapPin, Car } from 'lucide-react';

interface Props {
  pickup: { lat: number; lon: number };
  dropoff: { lat: number; lon: number };
  nearbyDrivers: Array<{ driverId: string; lat: number; lon: number; distanceKm: number }>;
  liveDriver: { lat: number; lon: number; heading: number } | null;
}

export default function LiveRadarMap({ pickup, dropoff, nearbyDrivers, liveDriver }: Props) {
  // Project lat/lon deltas relative to pickup center onto a 400x320 SVG viewport
  const project = (lat: number, lon: number) => {
    const scale = 3200;
    const x = 200 + (lon - pickup.lon) * scale;
    const y = 160 - (lat - pickup.lat) * scale;
    return {
      x: Math.max(20, Math.min(380, x)),
      y: Math.max(20, Math.min(300, y)),
    };
  };

  const pPos = project(pickup.lat, pickup.lon);
  const dPos = project(dropoff.lat, dropoff.lon);
  const driverPos = liveDriver ? project(liveDriver.lat, liveDriver.lon) : null;

  return (
    <div className="relative rounded-xl border border-slate-800 bg-slate-950 p-4 overflow-hidden">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-semibold uppercase tracking-wider text-emerald-400">
          Live Geospatial Radar (Redis Geo + STOMP)
        </span>
        <span className="text-xs text-slate-400">
          {nearbyDrivers.length} drivers in 5km radius
        </span>
      </div>

      <svg viewBox="0 0 400 320" className="w-full h-72 rounded-lg bg-slate-900/80 border border-slate-800">
        {/* Radar rings */}
        <circle cx={pPos.x} cy={pPos.y} r="55" fill="none" stroke="#1e293b" strokeDasharray="4 4" />
        <circle cx={pPos.x} cy={pPos.y} r="110" fill="none" stroke="#1e293b" strokeDasharray="4 4" />

        {/* Route vector */}
        <line
          x1={pPos.x}
          y1={pPos.y}
          x2={dPos.x}
          y2={dPos.y}
          stroke="#38bdf8"
          strokeWidth="2"
          strokeDasharray="6 4"
        />

        {/* Nearby available drivers */}
        {nearbyDrivers.map((d) => {
          const pos = project(d.lat, d.lon);
          return (
            <g key={d.driverId} transform={`translate(${pos.x}, ${pos.y})`}>
              <circle r="5" fill="#fbbf24" opacity="0.85" />
            </g>
          );
        })}

        {/* Pickup Marker */}
        <g transform={`translate(${pPos.x}, ${pPos.y})`}>
          <circle r="7" fill="#10b981" />
          <text x="10" y="4" fill="#a7f3d0" fontSize="10" fontWeight="bold">PICKUP</text>
        </g>

        {/* Dropoff Marker */}
        <g transform={`translate(${dPos.x}, ${dPos.y})`}>
          <circle r="7" fill="#f43f5e" />
          <text x="10" y="4" fill="#fecdd3" fontSize="10" fontWeight="bold">DROPOFF</text>
        </g>

        {/* Live Assigned Driver */}
        {driverPos && (
          <g transform={`translate(${driverPos.x}, ${driverPos.y})`}>
            <circle r="10" fill="#38bdf8" opacity="0.25" />
            <circle r="6" fill="#38bdf8" stroke="#fff" strokeWidth="1.5" />
            <text x="10" y="-6" fill="#7dd3fc" fontSize="10" fontWeight="bold">DRIVER (LIVE)</text>
          </g>
        )}
      </svg>

      <div className="mt-3 flex gap-4 text-xs text-slate-400">
        <span className="flex items-center gap-1"><MapPin className="w-3 h-3 text-emerald-400" /> Pickup</span>
        <span className="flex items-center gap-1"><MapPin className="w-3 h-3 text-rose-400" /> Dropoff</span>
        <span className="flex items-center gap-1"><Car className="w-3 h-3 text-amber-400" /> Available Pool</span>
        <span className="flex items-center gap-1"><Navigation className="w-3 h-3 text-sky-400" /> Assigned Driver</span>
      </div>
    </div>
  );
}