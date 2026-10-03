"use client";

import { useMemo, useState, type FormEvent } from "react";
import { ApiError, DATA_TYPES, query, queryPath, seriesId, type DataType, type SeriesInfo, type SeriesSnapshot } from "@/lib/api";
import { formatAverage, formatTime, formatValue } from "@/lib/format";
import { SamplesChart, TrendChart, toChartPoints } from "./TrendCharts";

export const EXPLORER_RANGES = ["30m", "1h", "3h", "6h", "12h", "24h"];
export const STEPS = [1, 2, 3, 5, 10, 15, 20, 30, 60, 120, 180, 240, 360, 720];

interface Form {
  dataType: DataType;
  category: string;
  dimension: string;
  range: string;
  step: number;
  window: string;
}

const field = "h-10 rounded-xl border border-edge bg-panel-2 px-3 text-sm text-ink";

/**
 * For developers: query any series with any range, step and window, and see the result as a
 * chart, a table and the raw JSON, with the request to reproduce it.
 */
export function ExplorerView({ catalog, timeZone }: { catalog: SeriesInfo[]; timeZone: string }) {
  const [form, setForm] = useState<Form>({ dataType: "long", category: "", dimension: "", range: "1h", step: 1, window: "" });
  const [filter, setFilter] = useState("");
  const [result, setResult] = useState<SeriesSnapshot | null>(null);
  const [path, setPath] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);

  const listed = useMemo(
    () =>
      catalog
        .filter((s) => `${s.category} ${s.dimension}`.toLowerCase().includes(filter.trim().toLowerCase()))
        .sort((a, b) => `${a.category}/${a.dimension}`.localeCompare(`${b.category}/${b.dimension}`)),
    [catalog, filter],
  );

  async function run(f: Form) {
    const q = {
      series: { dataType: f.dataType, category: f.category.trim(), dimension: f.dimension.trim() },
      range: f.range,
      step: f.step,
      window: f.window.trim() ? Number(f.window) : undefined,
      timeZone,
    };
    if (!q.series.category || !q.series.dimension) {
      setError("Choose a series, or enter a category and a dimension.");
      return;
    }
    setBusy(true);
    setPath(queryPath(q));
    try {
      setResult(await query(q));
      setError(null);
    } catch (e) {
      setResult(null);
      setError(e instanceof ApiError ? e.message : "The query failed.");
    } finally {
      setBusy(false);
    }
  }

  function pick(s: SeriesInfo) {
    const f = { ...form, dataType: s.dataType, category: s.category, dimension: s.dimension };
    setForm(f);
    run(f);
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    run(form);
  }

  async function copyCurl() {
    if (!path) return;
    try {
      await navigator.clipboard.writeText(`curl '${location.origin}${path}'`);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard refused (an insecure origin, say); the command is on screen to copy by hand
    }
  }

  const points = result ? toChartPoints(result.points, timeZone) : [];
  const active = result ? seriesId(result) : null;

  return (
    <div className="grid items-start gap-4 lg:grid-cols-[260px_1fr]">
      <aside className="tile rounded-[22px] p-4">
        <label className="block">
          <span className="sr-only">Filter series</span>
          <input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Filter series" className={`${field} w-full`} />
        </label>
        <ul className="mt-3 max-h-[60vh] space-y-0.5 overflow-y-auto text-sm" aria-label="Series">
          {listed.length === 0 && <li className="px-2 py-1 text-muted">No series.</li>}
          {listed.map((s) => (
            <li key={seriesId(s)}>
              <button
                type="button"
                onClick={() => pick(s)}
                aria-pressed={seriesId(s) === active}
                className={`w-full rounded-lg px-2 py-1.5 text-left ${seriesId(s) === active ? "bg-primary-soft font-semibold" : "hover:bg-panel-2"}`}
              >
                {s.category} / {s.dimension} <span className="text-xs text-muted">{s.dataType}</span>
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <div className="flex min-w-0 flex-col gap-4">
        <form onSubmit={submit} className="tile flex flex-wrap items-end gap-3 rounded-[22px] p-4 text-sm">
          <label className="grid gap-1">
            <span className="text-xs text-muted">Type</span>
            <select value={form.dataType} onChange={(e) => setForm({ ...form, dataType: e.target.value as DataType })} className={field}>
              {DATA_TYPES.map((t) => (
                <option key={t}>{t}</option>
              ))}
            </select>
          </label>
          <label className="grid gap-1">
            <span className="text-xs text-muted">Category</span>
            <input value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} placeholder="car" className={`${field} w-36`} />
          </label>
          <label className="grid gap-1">
            <span className="text-xs text-muted">Dimension</span>
            <input value={form.dimension} onChange={(e) => setForm({ ...form, dimension: e.target.value })} placeholder="speed" className={`${field} w-36`} />
          </label>
          <label className="grid gap-1">
            <span className="text-xs text-muted">Range</span>
            <select value={form.range} onChange={(e) => setForm({ ...form, range: e.target.value })} className={field}>
              {EXPLORER_RANGES.map((r) => (
                <option key={r}>{r}</option>
              ))}
            </select>
          </label>
          <label className="grid gap-1">
            <span className="text-xs text-muted">Step (min)</span>
            <select value={form.step} onChange={(e) => setForm({ ...form, step: Number(e.target.value) })} className={field}>
              {STEPS.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1">
            <span className="text-xs text-muted">Window (min)</span>
            <input
              inputMode="numeric"
              value={form.window}
              onChange={(e) => setForm({ ...form, window: e.target.value })}
              placeholder="= step"
              className={`${field} w-24`}
            />
          </label>
          <button type="submit" disabled={busy} className="h-10 rounded-xl bg-primary px-5 font-semibold text-primary-ink disabled:opacity-50">
            Run query
          </button>
        </form>

        {path && (
          <div className="tile flex flex-wrap items-center gap-3 rounded-xl px-4 py-2.5 text-sm">
            <span className="text-muted">GET</span>
            <code className="min-w-0 flex-1 break-all text-ink-2">{path}</code>
            <button type="button" onClick={copyCurl} className="rounded-lg border border-edge px-3 py-1 font-medium">
              {copied ? "Copied" : "Copy curl"}
            </button>
          </div>
        )}

        {error && (
          <p role="alert" className="tile rounded-xl px-4 py-3 text-sm text-critical">
            {error}
          </p>
        )}

        {!result && !error && (
          <p className="tile rounded-[22px] p-8 text-muted">Pick a series on the left, or fill in the form and run the query.</p>
        )}

        {result && (
          <>
            <ul className="grid grid-cols-[repeat(auto-fit,minmax(140px,1fr))] gap-3">
              {(
                [
                  ["Instant value", formatValue(result.last?.value, result.dataType)],
                  ["Samples", result.summary.count.toLocaleString("en-US")],
                  ["Highest", formatValue(result.summary.highestValue, result.dataType)],
                  ["Average", formatAverage(result.summary.averageValue, result.dataType)],
                  ["Lowest", formatValue(result.summary.lowestValue, result.dataType)],
                  ["Total", formatValue(result.summary.totalValue, result.dataType)],
                ] as const
              ).map(([k, v]) => (
                <li key={k} className="tile rounded-2xl p-4">
                  <div className="text-xs text-muted">{k}</div>
                  <div className="readout mt-1 truncate text-2xl font-bold">{v}</div>
                </li>
              ))}
            </ul>

            <section aria-label="Chart" className="tile rounded-[22px] p-5">
              <p className="text-xs text-muted">
                {result.points.length} points, one every {result.step} min, each over {result.window} min ({result.window > result.step ? "sliding" : "tumbling"})
              </p>
              <TrendChart points={points} dataType={result.dataType} syncId="explorer" />
              <SamplesChart points={points} dataType={result.dataType} syncId="explorer" />
            </section>

            <section aria-label="Points" className="tile overflow-hidden rounded-[22px]">
              <div className="max-h-[420px] overflow-auto">
                <table className="w-full text-sm">
                  <thead className="sticky top-0 bg-panel text-left text-muted">
                    <tr className="border-b border-rule">
                      {["Point", "Window", "Samples", "Highest", "Lowest", "Average", "Total"].map((h) => (
                        <th key={h} scope="col" className={`px-3 py-2 font-medium ${h === "Point" || h === "Window" ? "" : "text-right"}`}>
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-rule">
                    {[...result.points].reverse().map((m) => (
                      <tr key={m.timestamp} className={m.count === 0 ? "text-muted" : undefined}>
                        <td className="px-3 py-1.5">{formatTime(m.timestamp, timeZone)}</td>
                        <td className="px-3 py-1.5 text-ink-2">
                          {m.from !== undefined && m.to !== undefined ? `${formatTime(m.from, timeZone)}–${formatTime(m.to, timeZone)}` : "–"}
                        </td>
                        <td className="px-3 py-1.5 text-right">{m.count.toLocaleString("en-US")}</td>
                        <td className="px-3 py-1.5 text-right">{formatValue(m.highestValue, result.dataType)}</td>
                        <td className="px-3 py-1.5 text-right">{formatValue(m.lowestValue, result.dataType)}</td>
                        <td className="px-3 py-1.5 text-right">{formatAverage(m.averageValue, result.dataType)}</td>
                        <td className="px-3 py-1.5 text-right">{formatValue(m.totalValue, result.dataType)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>

            <details className="tile rounded-[22px] p-4">
              <summary className="cursor-pointer text-sm font-medium">Raw response</summary>
              <pre className="mt-3 max-h-[400px] overflow-auto rounded-xl bg-night p-4 text-xs text-night-ink">{JSON.stringify(result, null, 2)}</pre>
            </details>
          </>
        )}
      </div>
    </div>
  );
}
