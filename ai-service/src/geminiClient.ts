import { GoogleGenAI, Type } from '@google/genai';

const MODEL_NAME = 'gemini-2.5-flash';
const apiKey = process.env.GEMINI_API_KEY;
const ai = apiKey ? new GoogleGenAI({ apiKey }) : null;

export interface AiInsightResult {
  insightType: 'SURGE_AND_SAFETY' | 'TRIP_SUMMARY';
  headline: string;
  summaryText: string;
  recommendations: string[];
  modelVersion: string;
}

export async function generateActiveRideInsight(event: {
  rideId: string;
  surgeMultiplier: number;
  nearbyDriversCount: number;
  pickupLat: number;
  pickupLon: number;
  dropoffLat: number;
  dropoffLon: number;
}): Promise<AiInsightResult> {
  if (!ai) {
    return {
      insightType: 'SURGE_AND_SAFETY',
      headline: `Dynamic Surge ${event.surgeMultiplier}x Active`,
      summaryText: `Detected ${event.nearbyDriversCount} available drivers within 5km. Surge of ${event.surgeMultiplier}x reflects current local supply/demand balance. Recommend well-lit main-road pickup.`,
      recommendations: ['Confirm license plate before boarding', 'Share live trip link with a trusted contact'],
      modelVersion: 'fallback-deterministic',
    };
  }

  const prompt = `
You are NeuroRide's real-time AI mobility copilot.
A ride was just requested with:
- Surge multiplier: ${event.surgeMultiplier}x
- Available nearby drivers (5km radius): ${event.nearbyDriversCount}
- Pickup coordinates: (${event.pickupLat}, ${event.pickupLon})
- Dropoff coordinates: (${event.dropoffLat}, ${event.dropoffLon})

Provide:
1. A concise, transparent explanation for the ${event.surgeMultiplier}x fare multiplier based on driver supply (${event.nearbyDriversCount} drivers).
2. Two practical safety or pickup/stopover recommendations for this trip.
Keep summaryText under 55 words, professional, and reassuring.
`;

  const response = await ai.models.generateContent({
    model: MODEL_NAME,
    contents: prompt,
    config: {
      responseMimeType: 'application/json',
      responseSchema: {
        type: Type.OBJECT,
        properties: {
          headline: { type: Type.STRING },
          summaryText: { type: Type.STRING },
          recommendations: { type: Type.ARRAY, items: { type: Type.STRING } },
        },
        required: ['headline', 'summaryText', 'recommendations'],
      },
    },
  });

  const parsed = JSON.parse(response.text || '{}');
  return {
    insightType: 'SURGE_AND_SAFETY',
    headline: parsed.headline || 'Live Route & Surge Insight',
    summaryText: parsed.summaryText,
    recommendations: parsed.recommendations || [],
    modelVersion: MODEL_NAME,
  };
}

export async function generateCompletedTripInsight(event: {
  rideId: string;
  distanceMeters: number;
  durationSeconds: number;
  finalFareUsd: number;
  surgeMultiplier: number;
  pickupAddress?: string;
  dropoffAddress?: string;
}): Promise<AiInsightResult> {
  const km = (event.distanceMeters / 1000).toFixed(2);
  const mins = Math.max(1, Math.round(event.durationSeconds / 60));

  if (!ai) {
    return {
      insightType: 'TRIP_SUMMARY',
      headline: `Trip Completed (${km} km in ${mins} min)`,
      summaryText: `Completed ${km} km trip in ~${mins} minutes for $${event.finalFareUsd}. Efficiency rating: Optimal.`,
      recommendations: ['Review driver rating', 'Download itemized receipt'],
      modelVersion: 'fallback-deterministic',
    };
  }

  const prompt = `
Summarize this completed ride for the NeuroRide rider receipt:
- Route: ${event.pickupAddress || 'Pickup'} -> ${event.dropoffAddress || 'Dropoff'}
- Distance: ${km} km, Duration: ${mins} mins
- Final Fare: $${event.finalFareUsd} (Surge: ${event.surgeMultiplier}x)
Generate a friendly 2-sentence trip efficiency & carbon/route summary plus 2 helpful follow-up suggestions.
`;

  const response = await ai.models.generateContent({
    model: MODEL_NAME,
    contents: prompt,
    config: {
      responseMimeType: 'application/json',
      responseSchema: {
        type: Type.OBJECT,
        properties: {
          headline: { type: Type.STRING },
          summaryText: { type: Type.STRING },
          recommendations: { type: Type.ARRAY, items: { type: Type.STRING } },
        },
        required: ['headline', 'summaryText', 'recommendations'],
      },
    },
  });

  const parsed = JSON.parse(response.text || '{}');
  return {
    insightType: 'TRIP_SUMMARY',
    headline: parsed.headline || 'Post-Trip AI Summary',
    summaryText: parsed.summaryText,
    recommendations: parsed.recommendations || [],
    modelVersion: MODEL_NAME,
  };
}