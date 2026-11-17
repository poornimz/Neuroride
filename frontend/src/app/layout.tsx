import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'NeuroRide | AI-Powered Distributed Ride-Hailing',
  description: 'Real-time dispatch powered by Spring Boot STOMP, Redis Geo, PostGIS, Kafka, and Gemini AI',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="bg-slate-950 text-slate-100 antialiased">{children}</body>
    </html>
  );
}