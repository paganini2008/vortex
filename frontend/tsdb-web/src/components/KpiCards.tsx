"use client";

import { seriesId, type SeriesSnapshot } from "@/lib/api";
import { formatAge, formatAverage, formatCompact, formatValue } from "@/lib/format";
import { ArrowUpRightIcon } from "./Icons";

interface Props {
  snapshots: SeriesSnapshot[];
  selected: string | null;
  onSelect: (id: string) => void;
  rangeLabel: string;
  now: number;
}

/** One card per dimension with its instant value; the selected card, or the first, is filled. */
export function KpiCards({ snapshots, selected, onSelect, rangeLabel, now }: Props) {
  const filled = selected ?? (snapshots[0] ? seriesId(snapshots[0]) : null);
  return (
    <ul className="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-4">
      {snapshots.map((s) => {
        const id = seriesId(s);
        const on = id === filled;
        return (
          <li key={id}>
            <button
              type="button"
              onClick={() => onSelect(id)}
              aria-pressed={id === selected}
              className={`group flex h-full w-full flex-col rounded-2xl p-4 text-left transition-colors ${
                on ? "bg-primary text-primary-ink shadow-[0_0_40px_-8px_var(--glow)]" : "tile text-ink hover:bg-panel-2"
              }`}
            >
              <span className="flex w-full items-start justify-between gap-3">
                <span className="min-w-0">
                  <span className="block truncate text-[15px] font-semibold">{s.dimension}</span>
                  <span className={`text-xs ${on ? "opacity-80" : "text-muted"}`}>{s.dataType}</span>
                </span>
                <span
                  aria-hidden
                  className={`grid size-8 shrink-0 place-items-center rounded-full border ${
                    on ? "border-primary-ink/50 bg-primary-ink text-primary" : "border-rule text-ink-2"
                  }`}
                >
                  <ArrowUpRightIcon width={16} height={16} />
                </span>
              </span>
              <span className={`mt-3 block truncate text-4xl font-bold ${on ? "" : "readout"}`} title={formatValue(s.last?.value, s.dataType)}>
                {formatCompact(s.last?.value, s.dataType)}
              </span>
              <span className={`mt-3 text-xs ${on ? "opacity-85" : "text-muted"}`}>
                {s.last ? (
                  <>
                    Updated {formatAge(s.last.timestamp, now)}, average {formatAverage(s.summary.averageValue, s.dataType)} over{" "}
                    {rangeLabel}
                  </>
                ) : (
                  <>No sample within the retention period</>
                )}
              </span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}
