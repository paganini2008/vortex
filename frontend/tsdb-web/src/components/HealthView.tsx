"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Line, LineChart, ResponsiveContainer, YAxis } from "recharts";
import { ApiError, clusterHealth, type ClusterHealth, type InstanceHealth } from "@/lib/api";
import { formatAge, formatUptime } from "@/lib/format";
import { useChartColors } from "@/lib/useChartColors";
import { ExpandIcon } from "./Icons";
import { ZoomDialog } from "./ZoomDialog";

export const HEALTH_REFRESH_MS = 3000;
const HISTORY = 40;

export type Status = "healthy" | "degraded" | "unreachable";

/** What one instance's figures add up to; each reason is shown beside the status. */
/** The cache component of an instance's openspreader snapshot */
export const cacheOf = (i: InstanceHealth) => i.spreader?.components?.cache ?? {};

export function statusOf(i: InstanceHealth): { status: Status; reasons: string[] } {
  if (!i.reachable) return { status: "unreachable", reasons: [i.error ?? "not reporting"] };
  const reasons: string[] = [];
  const cache = cacheOf(i);
  if (i.spreader?.cluster?.splitBrain?.splitting) reasons.push("split brain");
  if ((cache.outboxOverflow ?? 0) > 0) reasons.push("broadcast queue overflowed");
  if (cache.syncing) reasons.push("resynchronising");
  if ((cache.spillFailures ?? 0) > 0) reasons.push("spill failures");
  if (i.jvm && i.jvm.status !== "UP") reasons.push(`Spring health ${i.jvm.status}`);
  if (i.jvm && i.jvm.heapMax > 0 && i.jvm.heapUsed / i.jvm.heapMax > 0.9) reasons.push("heap nearly full");
  if ((i.http?.serverErrorRate ?? 0) >= 0.01) reasons.push("server errors");
  else if ((i.http?.errorRate ?? 0) >= 0.05) reasons.push("request errors");
  return { status: reasons.length ? "degraded" : "healthy", reasons };
}

const STATUS_STYLE: Record<Status, { label: string; className: string; icon: string }> = {
  healthy: { label: "Healthy", className: "text-good border-good", icon: "M5 12l4 4 10-10" },
  degraded: { label: "Degraded", className: "text-[#fab219] border-[#fab219]", icon: "M12 7v6M12 17h.01" },
  unreachable: { label: "Unreachable", className: "text-critical border-critical", icon: "M7 7l10 10M17 7L7 17" },
};

function StatusBadge({ status }: { status: Status }) {
  const s = STATUS_STYLE[status];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${s.className}`}>
      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" aria-hidden>
        <path d={s.icon} />
      </svg>
      {s.label}
    </span>
  );
}

export function formatBytes(b: number | undefined) {
  if (!b) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  let i = 0;
  let v = b;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i++;
  }
  return `${v.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

const pct = (v: number | undefined) => `${((v ?? 0) * 100).toFixed(v && v < 0.01 ? 2 : 1)}%`;
const num = (v: number | undefined) => (v ?? 0).toLocaleString("en-US");
const ms = (v: number | undefined) => `${(v ?? 0).toFixed(1)} ms`;

function Figures({ title, rows }: { title: string; rows: [string, string, boolean?][] }) {
  return (
    <div>
      <h4 className="text-xs font-semibold text-muted">{title}</h4>
      <dl className="mt-1.5 grid grid-cols-[1fr_auto] gap-x-3 gap-y-1 text-sm">
        {rows.map(([k, v, warn]) => (
          <div key={k} className="contents">
            <dt className="text-ink-2">{k}</dt>
            <dd className={`text-right font-semibold ${warn ? "text-[#fab219]" : ""}`}>{v}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

function HeapBar({ used, max }: { used: number; max: number }) {
  const share = max > 0 ? Math.min(1, used / max) : 0;
  return (
    <div>
      <div className="flex justify-between text-xs text-muted">
        <span>Heap</span>
        <span>
          {formatBytes(used)} of {formatBytes(max)}
        </span>
      </div>
      <div
        role="meter"
        aria-label="Heap used"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(share * 100)}
        className="mt-1 h-2 overflow-hidden rounded-full"
        style={{ background: "var(--track)" }}
      >
        <div className={`h-full rounded-full ${share > 0.9 ? "bg-critical" : "bg-primary"}`} style={{ width: `${share * 100}%` }} />
      </div>
    </div>
  );
}

function QpsSpark({ values, height }: { values: number[]; height: number }) {
  const c = useChartColors();
  const data = values.map((v, i) => ({ i, v }));
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={data} margin={{ top: 4, right: 0, bottom: 4, left: 0 }}>
        <YAxis hide domain={[0, "dataMax"]} />
        <Line dataKey="v" stroke={c.series} strokeWidth={2} dot={false} isAnimationActive={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}

interface CardProps {
  i: InstanceHealth;
  /** Versions behind the leader; undefined when no reading of the same round compares */
  lag: number | undefined;
  qpsHistory: number[];
  now: number;
  onZoom?: () => void;
  large?: boolean;
}

export function InstanceCard({ i, lag, qpsHistory, now, onZoom, large = false }: CardProps) {
  const { status, reasons } = statusOf(i);
  const cache = cacheOf(i);
  const channel = i.spreader?.channels?.["spreader.cache"];
  const split = i.spreader?.cluster?.splitBrain;
  const mutex = i.spreader?.components?.mutex;
  const tasks = Object.entries(i.spreader?.components?.scheduled ?? {});
  return (
    <section aria-label={`${i.host}:${i.serverPort ?? "?"}`} className="tile rounded-[22px] p-5">
      <header className="flex flex-wrap items-center gap-3">
        <div className="min-w-0">
          <h3 className="truncate text-lg font-semibold">
            {i.host}:{i.serverPort ?? "?"}
          </h3>
          <p className="text-xs text-muted">
            {i.leader ? "Leader" : "Follower"}, up {formatUptime(i.startTime, now)}
            {i.reportedAt > 0 && `, reported ${formatAge(i.reportedAt, now)}`}
          </p>
        </div>
        <span className="ml-auto">
          <StatusBadge status={status} />
        </span>
        {onZoom && (
          <button type="button" onClick={onZoom} title="Enlarge" className="grid size-9 place-items-center rounded-xl text-muted hover:bg-panel-2 hover:text-ink">
            <ExpandIcon width={16} height={16} />
            <span className="sr-only">Enlarge {i.host}</span>
          </button>
        )}
      </header>
      {reasons.length > 0 && <p className="mt-2 text-xs text-ink-2">{reasons.join(", ")}</p>}

      {i.reachable && (
        <>
          <div className="mt-4 grid grid-cols-[auto_1fr] items-center gap-4 rounded-2xl bg-panel-2 p-3">
            <div>
              <div className="text-xs text-muted">Requests per second</div>
              <div className="readout text-3xl font-bold">{num(Math.round(i.http?.qps ?? 0))}</div>
            </div>
            <QpsSpark values={qpsHistory} height={large ? 160 : 44} />
          </div>
          {i.jvm && (
            <div className="mt-4 rounded-2xl bg-panel-2 p-3">
              <HeapBar used={i.jvm.heapUsed} max={i.jvm.heapMax} />
            </div>
          )}
          <div className={`mt-4 grid grid-cols-1 gap-5 sm:grid-cols-2 ${large ? "xl:grid-cols-5" : "2xl:grid-cols-3"}`}>
            <Figures
              title="API"
              rows={[
                ["Error rate", pct(i.http?.errorRate), (i.http?.errorRate ?? 0) >= 0.05],
                ["Server errors", pct(i.http?.serverErrorRate), (i.http?.serverErrorRate ?? 0) >= 0.01],
                ["Mean latency", ms(i.http?.avgLatencyMs)],
                ["Requests", num(i.http?.requests)],
              ]}
            />
            <Figures
              title="Cache"
              rows={[
                ["Keys", num(cache.keyCount)],
                ["Memory", formatBytes(cache.approxBytes)],
                ["Version", num(cache.appliedVersion)],
                ["Behind leader", i.leader || lag === undefined ? "–" : num(lag)],
                ["Outbox", `${num(cache.outboxDepth)} queued`, (cache.outboxOverflow ?? 0) > 0],
                ["Overflows", num(cache.outboxOverflow), (cache.outboxOverflow ?? 0) > 0],
                ["Resyncs", num(cache.resyncCount)],
                ["Evicted", num(cache.evicted)],
              ]}
            />
            <Figures
              title="Replication"
              rows={[
                ["Messages/s", num(Math.round(channel?.throughput?.tps ?? 0))],
                ["Send p50 / p99", `${ms(channel?.latencyMillis?.outbound?.p50)} / ${ms(channel?.latencyMillis?.outbound?.p99)}`],
                ["Apply p99", ms(channel?.latencyMillis?.inbound?.p99)],
                ["Error rate", pct(channel?.rates?.errorRate), (channel?.rates?.errorRate ?? 0) > 0],
                ["Retry rate", pct(channel?.rates?.retryRate)],
                ["In flight", num(channel?.concurrency?.current)],
                ["Sent / received", `${num(channel?.counters?.sent)} / ${num(channel?.counters?.received)}`],
                ["Send failures", `${num(channel?.counters?.sendFailures)} (${num(channel?.counters?.retries)} retries)`, (channel?.counters?.sendFailures ?? 0) > 0],
                ["Buffered", num(cache.bufferedUpdates)],
              ]}
            />
            <Figures
              title="Coordination"
              rows={[
                ["Split brain", split ? (split.splitting ? "Splitting" : "No") : "–", !!split?.splitting],
                ["Splits seen", num(split?.occurrences)],
                ["Locks held", num(mutex?.heldNow)],
                ["Lock contention", pct(mutex?.contentionRate)],
                ["Lock wait", ms(mutex?.avgWaitMillis)],
                ...tasks.map(([name, t]): [string, string, boolean] => [
                  name.split("/").pop() ?? name,
                  `${num(t.executed)} runs${t.failed ? `, ${num(t.failed)} failed` : ""}${t.lastExecutedAtMs ? `, last ${formatAge(t.lastExecutedAtMs, now)}` : ""}`,
                  t.failed > 0,
                ]),
              ]}
            />
            {i.jvm && (
              <Figures
                title="JVM"
                rows={[
                  ["Spring health", i.jvm.status, i.jvm.status !== "UP"],
                  ["Process CPU", pct(i.jvm.processCpu)],
                  ["System CPU", pct(i.jvm.systemCpu)],
                  ["CPUs", num(i.jvm.processors)],
                  ["Heap committed", formatBytes(i.jvm.heapCommitted)],
                  ["Non-heap", formatBytes(i.jvm.nonHeapUsed)],
                  ["GC (last 5 s)", `${num(i.jvm.gcPauses)}, ${ms(i.jvm.gcPauseMs)}`],
                  ["Threads", `${num(i.jvm.threads)} (peak ${num(i.jvm.peakThreads)})`],
                ]}
              />
            )}
          </div>
        </>
      )}
    </section>
  );
}

/** Cluster-wide health: one card per instance, refreshed every few seconds. */
export function HealthView({ live, now, onSummary }: { live: boolean; now: number; onSummary?: (text: string) => void }) {
  const [health, setHealth] = useState<ClusterHealth | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [history, setHistory] = useState<Record<string, number[]>>({});
  const [zoomed, setZoomed] = useState<string | null>(null);
  const closeZoom = useCallback(() => setZoomed(null), []);
  const [writes, setWrites] = useState<number | null>(null);
  const lastLeader = useRef<{ version: number; at: number } | null>(null);

  useEffect(() => {
    const load = () =>
      clusterHealth()
        .then((h) => {
          setHealth(h);
          setError(null);
          setHistory((prev) => {
            const next: Record<string, number[]> = {};
            for (const i of h.instances) next[i.id] = [...(prev[i.id] ?? []), i.http?.qps ?? 0].slice(-HISTORY);
            return next;
          });
          const leader = h.instances.find((i) => i.leader && i.reachable);
          const v = leader ? cacheOf(leader).appliedVersion : undefined;
          if (leader && v !== undefined) {
            const prev = lastLeader.current;
            // The version restarts from 0 under a new leader; skip that reading
            if (prev && v >= prev.version && leader.reportedAt > prev.at) {
              setWrites(Math.round(((v - prev.version) * 1000) / (leader.reportedAt - prev.at)));
            }
            lastLeader.current = { version: v, at: leader.reportedAt };
          }
        })
        .catch((e) => setError(e instanceof ApiError ? e.message : "Health is unavailable."));
    load();
    if (!live) return;
    const id = setInterval(load, HEALTH_REFRESH_MS);
    return () => clearInterval(id);
  }, [live]);

  const instances = health?.instances ?? [];
  const up = instances.filter((i) => i.reachable);
  const qps = up.reduce((s, i) => s + (i.http?.qps ?? 0), 0);
  const errors = up.reduce((s, i) => s + (i.http?.qps ?? 0) * (i.http?.errorRate ?? 0), 0);
  const leader = instances.find((i) => i.leader && i.reachable);
  const zoomedInstance = instances.find((i) => i.id === zoomed);

  useEffect(() => {
    if (health) onSummary?.(`${up.length} of ${instances.length} instances reporting, leader ${health.leader ?? "none"}`);
  }, [health, up.length, instances.length, onSummary]);

  const kpis: [string, string][] = [
    ["Requests per second", num(Math.round(qps))],
    ["Error rate", qps > 0 ? pct(errors / qps) : "0%"],
    ["Cache writes per second", writes === null ? "–" : num(writes)],
    ["Keys", num(leader ? cacheOf(leader).keyCount : undefined)],
    ["Max replication lag", `${num(health?.maxReplicationLag)} versions`],
    ["Instances up", `${up.length} / ${instances.length}`],
  ];

  return (
    <div className="flex flex-col gap-4">
      {error && (
        <p role="alert" className="tile rounded-xl px-4 py-3 text-sm text-critical">
          {error}
        </p>
      )}
      {!health && !error && <p className="tile rounded-[22px] p-8 text-muted">Loading…</p>}
      {health && (
        <>
          <ul className="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-4">
            {kpis.map(([k, v]) => (
              <li key={k} className="tile rounded-2xl p-4">
                <div className="text-xs text-muted">{k}</div>
                <div className="readout mt-2 truncate text-3xl font-bold">{v}</div>
              </li>
            ))}
          </ul>
          <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,520px),1fr))] items-start gap-4">
            {instances.map((i) => (
              <InstanceCard key={i.id} i={i} lag={health.replicationLag?.[i.id]} qpsHistory={history[i.id] ?? []} now={now} onZoom={() => setZoomed(i.id)} />
            ))}
          </div>
          {zoomedInstance && (
            <ZoomDialog title={`${zoomedInstance.host}:${zoomedInstance.serverPort ?? "?"}`} onClose={closeZoom}>
              <InstanceCard i={zoomedInstance} lag={health?.replicationLag?.[zoomedInstance.id]} qpsHistory={history[zoomedInstance.id] ?? []} now={now} large />
            </ZoomDialog>
          )}
        </>
      )}
    </div>
  );
}
