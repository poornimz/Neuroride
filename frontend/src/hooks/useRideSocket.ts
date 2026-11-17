'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

export interface DriverLocation {
  driverId: string;
  lat: number;
  lon: number;
  heading: number;
  timestamp: string;
}

export interface AiInsight {
  rideId: string;
  insightType: string;
  headline: string;
  summaryText: string;
  recommendations: string[];
  modelVersion: string;
  createdAt: string;
}

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/ws';

export function useRideSocket(activeRideId: string | null) {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const [driverLocation, setDriverLocation] = useState<DriverLocation | null>(null);
  const [rideStatusUpdate, setRideStatusUpdate] = useState<any>(null);
  const [insights, setInsights] = useState<AiInsight[]>([]);

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 3000,
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  useEffect(() => {
    const client = clientRef.current;
    if (!client || !connected || !activeRideId) return;

    const locSub = client.subscribe(`/topic/ride.${activeRideId}.location`, (msg) => {
      setDriverLocation(JSON.parse(msg.body));
    });

    const stateSub = client.subscribe(`/topic/ride.${activeRideId}`, (msg) => {
      setRideStatusUpdate(JSON.parse(msg.body));
    });

    const aiSub = client.subscribe(`/topic/ride.${activeRideId}.insights`, (msg) => {
      const payload: AiInsight = JSON.parse(msg.body);
      setInsights((prev) => [payload, ...prev]);
    });

    return () => {
      locSub.unsubscribe();
      stateSub.unsubscribe();
      aiSub.unsubscribe();
    };
  }, [connected, activeRideId]);

  const sendDriverPing = useCallback(
    (payload: {
      driverId: string;
      activeRideId: string | null;
      lat: number;
      lon: number;
      heading: number;
      status: 'AVAILABLE' | 'ON_TRIP';
    }) => {
      if (clientRef.current?.connected) {
        clientRef.current.publish({
          destination: '/app/driver.location',
          body: JSON.stringify(payload),
        });
      }
    },
    []
  );

  const clearInsights = useCallback(() => setInsights([]), []);

  return { connected, driverLocation, rideStatusUpdate, insights, sendDriverPing, clearInsights };
}