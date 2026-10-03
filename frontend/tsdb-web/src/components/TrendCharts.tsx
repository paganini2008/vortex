"use client";

import { Area, Bar, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { DataType, Metric } from "@/lib/api";
import { formatAverage, formatTime, formatValue } from "@/lib/format";
import { useChartColors } from "@/lib/useChartColors";

export interface ChartPoint {
  label: string;
  count: number;
  highest: number | null;
  average: number | null;
  lowest: number | null;
  /** [lowest, highest], or null for an empty window so the band breaks there */
  range: [number, number] | null;
}

export function toChartPoints(points: Metric[], timeZone: string): ChartPoint[] {
  return points.map((m) => ({
    label: formatTime(m.timestamp, timeZone),
    count: m.count,
    highest: m.highestValue,
    average: m.averageValue,
    lowest: m.lowestValue,
    range: m.count > 0 && m.lowestValue !== null && m.highestValue !== null ? [m.lowestValue, m.highestValue] : null,
  }));
}

export function PointTooltip({
  active,
  label,
  points,
  dataType,
}: {
  active?: boolean;
  label?: string | number;
  points: ChartPoint[];
  dataType: DataType;
}) {
  // Looked up by label: an empty window has no payload, but still deserves an answer
  const p = active ? points.find((x) => x.label === label) : undefined;
  if (!p) return null;
  const rows: [string, string][] =
    p.count === 0
      ? [["Samples", "0"]]
      : [
          ["Samples", p.count.toLocaleString("en-US")],
          ["Highest", formatValue(p.highest, dataType)],
          ["Average", formatAverage(p.average, dataType)],
          ["Lowest", formatValue(p.lowest, dataType)],
        ];
  return (
    <div className="rounded-xl border border-rule bg-panel px-3 py-2 text-sm shadow-lg">
      <div className="mb-1 font-semibold text-ink">{label}</div>
      <dl className="grid grid-cols-[auto_auto] gap-x-4 gap-y-0.5">
        {rows.map(([k, v]) => (
          <div key={k} className="contents">
            <dt className="text-muted">{k}</dt>
            <dd className="text-right text-ink">{v}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

function axisProps(color: string) {
  return { stroke: color, tick: { fill: color, fontSize: 12 }, tickLine: false, axisLine: false };
}

interface ChartProps {
  points: ChartPoint[];
  dataType: DataType;
  syncId: string;
  /** Pixels; an enlarged card draws taller charts */
  height?: number;
}

/** Lowest-to-highest as a band with the average drawn through it, on one y-axis. */
export function TrendChart({ points, dataType, syncId, height = 190 }: ChartProps) {
  const c = useChartColors();
  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={points} syncId={syncId} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
        <CartesianGrid stroke={c.grid} vertical={false} />
        <XAxis dataKey="label" scale="band" {...axisProps(c.axis)} minTickGap={36} />
        <YAxis {...axisProps(c.axis)} width={52} tickFormatter={(v: number) => formatValue(v, dataType)} />
        <Tooltip
          cursor={{ stroke: c.axis, strokeWidth: 1, strokeDasharray: "3 3" }}
          content={({ active, label }) => <PointTooltip active={active} label={label} points={points} dataType={dataType} />}
        />
        <Area dataKey="range" name="Range" stroke="none" fill={c.band} fillOpacity={1} isAnimationActive={false} activeDot={false} connectNulls={false} />
        <Line
          dataKey="average"
          name="Average"
          stroke={c.series}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4, stroke: c.panel, strokeWidth: 2, fill: c.series }}
          isAnimationActive={false}
          connectNulls={false}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

/** Samples per point as capsules standing on a full-height track. */
export function SamplesChart({ points, dataType, syncId, height = 84 }: ChartProps) {
  const c = useChartColors();
  // Recharts skips a bar's background where the value is 0, so the track is a bar of its own,
  // as tall as the busiest point, drawn on a second x-axis so it sits exactly behind
  const top = Math.max(1, ...points.map((p) => p.count));
  const data = points.map((p) => ({ ...p, track: top }));
  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={data} syncId={syncId} margin={{ top: 4, right: 8, bottom: 0, left: 0 }} barCategoryGap="30%">
        <XAxis dataKey="label" scale="band" hide />
        <XAxis dataKey="label" scale="band" hide xAxisId="track" />
        <YAxis {...axisProps(c.axis)} width={52} allowDecimals={false} tickCount={2} domain={[0, top]} />
        <Tooltip cursor={false} content={({ active, label }) => <PointTooltip active={active} label={label} points={points} dataType={dataType} />} />
        <Bar dataKey="track" xAxisId="track" fill={c.track} radius={[999, 999, 999, 999]} maxBarSize={12} isAnimationActive={false} activeBar={false} />
        <Bar dataKey="count" name="Samples" fill={c.series} radius={[999, 999, 999, 999]} maxBarSize={12} isAnimationActive={false} />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
