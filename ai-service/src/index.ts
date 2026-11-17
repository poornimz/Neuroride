import * as Sentry from '@sentry/node';
import express from 'express';
import { Kafka } from 'kafkajs';
import { Pool } from 'pg';
import client from 'prom-client';
import {
  generateActiveRideInsight,
  generateCompletedTripInsight,
  AiInsightResult,
} from './geminiClient';

if (process.env.SENTRY_DSN) {
  Sentry.init({ dsn: process.env.SENTRY_DSN, tracesSampleRate: 0.2 });
}

// Prometheus Metrics
client.collectDefaultMetrics();
const aiLatencyHist = new client.Histogram({
  name: 'neuroride_ai_generation_duration_seconds',
  help: 'Time spent calling Gemini API and persisting insights',
  labelNames: ['event_type'],
  buckets: [0.1, 0.3, 0.5, 1, 2, 5],
});
const aiErrorCounter = new client.Counter({
  name: 'neuroride_ai_errors_total',
  help: 'Total failed AI insight generations',
});

const pgPool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 5432),
  database: process.env.DB_NAME || 'neuroride',
  user: process.env.POSTGRES_USER || 'neuroride_admin',
  password: process.env.POSTGRES_PASSWORD || 'neuroride_secret',
});

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

async function persistAndBroadcast(rideId: string, insight: AiInsightResult) {
  await pgPool.query(
    `INSERT INTO ride_ai_insights (ride_id, insight_type, summary_text, raw_metadata, model_version)
     VALUES ($1, $2, $3, $4, $5)`,
    [
      rideId,
      insight.insightType,
      insight.summaryText,
      JSON.stringify({ headline: insight.headline, recommendations: insight.recommendations }),
      insight.modelVersion,
    ]
  );

  // Push to Spring Boot WebSocket bridge so Rider UI receives insight in real time
  await fetch(`${BACKEND_URL}/api/v1/internal/rides/${rideId}/insights`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      rideId,
      ...insight,
      createdAt: new Date().toISOString(),
    }),
  });
}

async function start() {
  const kafka = new Kafka({
    clientId: 'neuroride-ai-worker',
    brokers: (process.env.KAFKA_BOOTSTRAP_SERVERS || 'localhost:29092').split(','),
  });

  const consumer = kafka.consumer({ groupId: 'neuroride-ai-insights-group' });
  await consumer.connect();
  await consumer.subscribe({ topics: ['ride-requests', 'trip-completed'], fromBeginning: false });

  await consumer.run({
    eachMessage: async ({ topic, message }) => {
      if (!message.value) return;
      const endTimer = aiLatencyHist.startTimer({ event_type: topic });

      try {
        const payload = JSON.parse(message.value.toString());
        if (topic === 'ride-requests') {
          const insight = await generateActiveRideInsight(payload);
          await persistAndBroadcast(payload.rideId, insight);
          console.log(`[AI] Published surge/safety insight for ride ${payload.rideId}`);
        } else if (topic === 'trip-completed') {
          const insight = await generateCompletedTripInsight(payload);
          await persistAndBroadcast(payload.rideId, insight);
          console.log(`[AI] Published post-trip summary for ride ${payload.rideId}`);
        }
      } catch (err) {
        aiErrorCounter.inc();
        Sentry.captureException(err);
        console.error(`[AI] Error processing ${topic}:`, err);
      } finally {
        endTimer();
      }
    },
  });

  const app = express();
  app.get('/health', (_req, res) => res.json({ status: 'UP' }));
  app.get('/metrics', async (_req, res) => {
    res.set('Content-Type', client.register.contentType);
    res.end(await client.register.metrics());
  });

  const port = Number(process.env.PORT || 8081);
  app.listen(port, () => console.log(`[AI] Health & Prometheus metrics listening on :${port}`));
}

start().catch((err) => {
  Sentry.captureException(err);
  console.error('Fatal AI worker error:', err);
  process.exit(1);
});