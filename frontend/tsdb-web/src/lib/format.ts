import type { DataType } from "./api";

/** The ranges the dashboard offers, with the step that gives about 60 points each. */
export const RANGES = [
  { range: "1h", step: 1, label: "1 hour" },
  { range: "6h", step: 5, label: "6 hours" },
  { range: "24h", step: 30, label: "24 hours" },
] as const;

export type RangeKey = (typeof RANGES)[number]["range"];

/** Smoothing turns each point into a moving aggregate over this many steps. */
export const SMOOTHING_STEPS = 5;

export function rangeOf(key: string | null | undefined) {
  return RANGES.find((r) => r.range === key) ?? RANGES[0];
}

/** "Live" when a sample arrived within this long. */
export const LIVE_WITHIN_MS = 2 * 60_000;

export function formatValue(v: number | null | undefined, dataType: DataType): string {
  if (v === null || v === undefined || Number.isNaN(v)) return "–";
  if (dataType === "long") return Math.round(v).toLocaleString("en-US");
  return v.toLocaleString("en-US", { maximumFractionDigits: 4 });
}

/** Averages of integer series need no more than two places. */
export function formatAverage(v: number | null | undefined, dataType: DataType): string {
  if (v === null || v === undefined || Number.isNaN(v)) return "–";
  return v.toLocaleString("en-US", { maximumFractionDigits: dataType === "long" ? 2 : 4 });
}

/** Shorter numbers for the large readouts: 12,345,678 becomes 12.35M. */
export function formatCompact(v: number | null | undefined, dataType: DataType): string {
  if (v === null || v === undefined || Number.isNaN(v)) return "–";
  if (Math.abs(v) < 100_000) return formatValue(v, dataType);
  return v.toLocaleString("en-US", { notation: "compact", maximumFractionDigits: 2 });
}

export function formatTime(ts: number, timeZone: string): string {
  return new Intl.DateTimeFormat("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone,
  }).format(ts);
}

export function formatAge(ts: number, now: number): string {
  const s = Math.max(0, Math.round((now - ts) / 1000));
  if (s < 5) return "just now";
  if (s < 60) return `${s}s ago`;
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ago`;
  return `${Math.floor(s / 86400)}d ago`;
}

export function formatUptime(ms: number, now: number): string {
  const s = Math.max(0, Math.round((now - ms) / 1000));
  if (s < 60) return `${s}s`;
  if (s < 3600) return `${Math.floor(s / 60)}m`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ${Math.floor((s % 3600) / 60)}m`;
  return `${Math.floor(s / 86400)}d`;
}

/** A rate for the sidebar: 0, "<1", or whole samples per minute. */
export function formatRate(perMinute: number): string {
  if (perMinute <= 0) return "0/min";
  if (perMinute < 1) return "<1/min";
  if (perMinute < 10_000) return `${Math.round(perMinute).toLocaleString("en-US")}/min`;
  return `${(perMinute / 1000).toFixed(0)}k/min`;
}

/**
 * Where a value sits between a low and a high, from 0 to 1. Null when there is no range to
 * place it in; a flat range (low == high) puts it in the middle.
 */
export function positionInRange(
  value: number | null | undefined,
  low: number | null | undefined,
  high: number | null | undefined,
): number | null {
  if (value == null || low == null || high == null) return null;
  if (high === low) return 0.5;
  return Math.min(1, Math.max(0, (value - low) / (high - low)));
}
