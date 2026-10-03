"use client";

import { useMemo } from "react";
import { seriesId, type SeriesSnapshot } from "@/lib/api";
import { LIVE_WITHIN_MS, formatAge, formatAverage, formatValue, positionInRange } from "@/lib/format";
import { ChevronIcon, ExpandIcon } from "./Icons";
import { RangeGauge } from "./RangeGauge";
import { SamplesChart, TrendChart, toChartPoints } from "./TrendCharts";

interface Props {
  snapshot: SeriesSnapshot;
  timeZone: string;
  rangeLabel: string;
  expanded: boolean;
  highlighted: boolean;
  onToggle: () => void;
  now: number;
  /** Opens this tile enlarged; absent inside the enlarged view itself */
  onZoom?: () => void;
  /** The enlarged view: taller charts, nothing to collapse */
  large?: boolean;
}

function windowNote(s: SeriesSnapshot) {
  const each = s.step === 1 ? "every minute" : `every ${s.step} min`;
  return s.window > s.step ? `Moving ${s.window}-min window, ${each}` : `One point ${each}`;
}

/** One dimension as a tile of the display: instant value, trend, samples, and where it sits. */
export function DimensionPanel({ snapshot: s, timeZone, rangeLabel, expanded, highlighted, onToggle, now, onZoom, large = false }: Props) {
  const id = seriesId(s);
  const points = useMemo(() => toChartPoints(s.points, timeZone), [s.points, timeZone]);
  const live = s.last !== null && now - s.last.timestamp < LIVE_WITHIN_MS;
  const position = positionInRange(s.last?.value, s.summary.lowestValue, s.summary.highestValue);
  const bodyId = `panel-${id}`;

  const stats: [string, string][] = [
    ["Samples", s.summary.count.toLocaleString("en-US")],
    ["Highest", formatValue(s.summary.highestValue, s.dataType)],
    ["Average", formatAverage(s.summary.averageValue, s.dataType)],
    ["Lowest", formatValue(s.summary.lowestValue, s.dataType)],
    ["This minute", s.current.count.toLocaleString("en-US")],
  ];

  const heading = (
    <>
      <span aria-hidden className={`size-2 shrink-0 rounded-full ${live ? "pulse bg-good" : "bg-muted"}`} />
      <span className="min-w-0">
        <span className={`block truncate font-semibold ${large ? "text-2xl" : "text-base"}`}>{s.dimension}</span>
        <span className="block text-xs text-muted">
          {s.category}, {s.dataType}, {live ? "live" : s.last ? `idle, last ${formatAge(s.last.timestamp, now)}` : "no samples"}
        </span>
      </span>
      <span className={`readout ml-auto font-bold ${large ? "text-5xl" : "text-3xl"}`}>{formatValue(s.last?.value, s.dataType)}</span>
    </>
  );

  return (
    <section
      id={large ? undefined : `dim-${id}`}
      aria-label={s.dimension}
      className={`tile scroll-mt-4 rounded-[22px] ${highlighted ? "ring-2 ring-primary" : ""}`}
    >
      <div className="flex items-start">
        <h3 className="min-w-0 flex-1">
          {large ? (
            <div className="flex w-full items-center gap-3 px-6 pb-2 pt-5">{heading}</div>
          ) : (
            <button
              type="button"
              onClick={onToggle}
              aria-expanded={expanded}
              aria-controls={bodyId}
              className="flex w-full items-center gap-3 rounded-[22px] py-4 pl-5 pr-2 text-left"
            >
              {heading}
              <ChevronIcon className={`shrink-0 text-muted transition-transform ${expanded ? "rotate-180" : ""}`} />
            </button>
          )}
        </h3>
        {onZoom && (
          <button
            type="button"
            onClick={onZoom}
            title="Enlarge"
            className="mr-3 mt-4 grid size-9 shrink-0 place-items-center rounded-xl text-muted hover:bg-panel-2 hover:text-ink"
          >
            <ExpandIcon width={16} height={16} />
            <span className="sr-only">Enlarge {s.dimension}</span>
          </button>
        )}
      </div>

      {(expanded || large) && (
        <div id={bodyId} className={large ? "px-6 pb-6" : "px-5 pb-5"}>
          <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 text-xs text-ink-2">
            <div className="flex items-center gap-4" aria-label="Legend">
              <span className="flex items-center gap-2">
                <span aria-hidden className="h-0.5 w-5 rounded bg-primary" />
                Average
              </span>
              <span className="flex items-center gap-2">
                <span aria-hidden className="h-3 w-5 rounded-sm" style={{ background: "var(--series-band)" }} />
                Lowest to highest
              </span>
            </div>
            <span className="text-muted">{windowNote(s)}</span>
          </div>
          <div className="mt-2">
            <TrendChart points={points} dataType={s.dataType} syncId={large ? `${id}-large` : id} height={large ? 420 : undefined} />
          </div>
          <p className="mt-3 text-xs text-muted">Samples per point</p>
          <SamplesChart points={points} dataType={s.dataType} syncId={large ? `${id}-large` : id} height={large ? 140 : undefined} />

          <div className={`mt-4 grid items-center gap-4 rounded-2xl bg-panel-2 p-3 ${large ? "grid-cols-[200px_1fr] sm:p-5" : "grid-cols-[150px_1fr]"}`}>
            <RangeGauge position={position} caption={`Instant value within the last ${rangeLabel}'s range`} />
            <dl className="grid grid-cols-2 gap-x-3 gap-y-1.5 text-sm">
              {stats.map(([k, v]) => (
                <div key={k} className="contents">
                  <dt className="text-muted">{k}</dt>
                  <dd className="text-right font-semibold">{v}</dd>
                </div>
              ))}
            </dl>
          </div>
        </div>
      )}
    </section>
  );
}
