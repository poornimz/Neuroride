'use client';

import React from 'react';
import { Sparkles, ShieldCheck } from 'lucide-react';
import { AiInsight } from '@/hooks/useRideSocket';

export default function AiInsightsPanel({ insights }: { insights: AiInsight[] }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold flex items-center gap-2 text-violet-300">
          <Sparkles className="w-4 h-4" />
          NeuroRide Gemini AI Insights
        </h2>
        <span className="text-[11px] px-2 py-0.5 rounded bg-violet-950 text-violet-300 border border-violet-800">
          Kafka Streamed
        </span>
      </div>

      {insights.length === 0 ? (
        <p className="text-xs text-slate-400 py-6 text-center">
          Request or complete a ride to stream real-time surge explanations, safety tips, and trip summaries from Gemini.
        </p>
      ) : (
        <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
          {insights.map((item, idx) => (
            <div key={idx} className="rounded-lg border border-violet-900/60 bg-slate-950 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-violet-200">{item.headline}</span>
                <span className="text-[10px] text-slate-500">{item.modelVersion}</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">{item.summaryText}</p>
              {item.recommendations?.length > 0 && (
                <ul className="space-y-1 pt-1 border-t border-slate-800">
                  {item.recommendations.map((rec, i) => (
                    <li key={i} className="text-[11px] text-emerald-300 flex items-center gap-1.5">
                      <ShieldCheck className="w-3 h-3 shrink-0" />
                      {rec}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}