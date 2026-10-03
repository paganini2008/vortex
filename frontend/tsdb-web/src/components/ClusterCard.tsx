"use client";

import type { ClusterView } from "@/lib/api";
import { formatUptime } from "@/lib/format";
import { NodesIcon } from "./Icons";

/** The dark card at the foot of the sidebar: which nodes answer and which one leads. */
export function ClusterCard({ cluster, error, now }: { cluster: ClusterView | null; error: string | null; now: number }) {
  const leader = cluster?.members.find((m) => m.leader);
  return (
    <section
      aria-labelledby="cluster-heading"
      className="relative overflow-hidden rounded-2xl bg-night p-4 text-night-ink"
    >
      <div
        aria-hidden
        className="pointer-events-none absolute -right-10 -top-12 size-36 rounded-full opacity-40"
        style={{ background: "radial-gradient(circle, var(--primary) 0%, transparent 70%)" }}
      />
      <div className="relative flex items-center gap-2">
        <NodesIcon />
        <h2 id="cluster-heading" className="text-sm font-semibold">
          Cluster
        </h2>
        {cluster && <span aria-hidden className="pulse ml-auto size-2 rounded-full bg-good" />}
      </div>
      {error ? (
        <p className="relative mt-3 text-sm text-night-muted">{error}</p>
      ) : !cluster ? (
        <p className="relative mt-3 text-sm text-night-muted">Connecting…</p>
      ) : (
        <>
          <p className="relative mt-3 text-3xl font-bold">
            {cluster.members.length}
            <span className="ml-1.5 text-sm font-medium text-night-muted">
              {cluster.members.length === 1 ? "node" : "nodes"}
            </span>
          </p>
          <ul className="relative mt-3 space-y-1.5 text-xs">
            {cluster.members.map((m) => (
              <li key={m.id} className="flex items-center gap-1.5">
                <span className="min-w-0 truncate">{m.host}</span>
                {m.leader && <span className="shrink-0 rounded bg-primary px-1.5 py-px text-[10px] font-semibold text-primary-ink">leader</span>}
                <span className="ml-auto shrink-0 text-night-muted">up {formatUptime(m.startTime, now)}</span>
              </li>
            ))}
          </ul>
          {!leader && <p className="relative mt-2 text-xs text-night-muted">Electing a leader…</p>}
        </>
      )}
    </section>
  );
}
