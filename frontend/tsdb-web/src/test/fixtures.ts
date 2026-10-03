import type { ClusterView, Metric, SeriesSnapshot } from "@/lib/api";

export const NOW = Date.UTC(2026, 9, 3, 9, 30);

export function metric(count: number, base: number, timestamp: number): Metric {
  return count === 0
    ? { count: 0, highestValue: null, lowestValue: null, totalValue: null, averageValue: null, timestamp }
    : { count, highestValue: base + 10, lowestValue: base - 10, totalValue: base * count, averageValue: base, timestamp };
}

export function snapshot(over: Partial<SeriesSnapshot> = {}): SeriesSnapshot {
  const points = Array.from({ length: 6 }, (_, i) => metric(i % 3 === 0 ? 0 : 5, 50 + i, NOW - (5 - i) * 60_000));
  return {
    dataType: "long",
    category: "car",
    dimension: "speed",
    range: "1h",
    step: 1,
    window: 1,
    last: { value: 55, timestamp: NOW - 1_000 },
    current: metric(5, 55, NOW),
    summary: metric(20, 50, NOW - 3_600_000),
    points,
    ...over,
  };
}

export function cluster(over: Partial<ClusterView> = {}): ClusterView {
  const node = (n: number, leader: boolean) => ({
    id: `id-${n}`,
    name: "vortex-tsd-service",
    host: `10.0.0.${n}`,
    port: 22000,
    serverPort: "30080",
    state: "ALIVE",
    startTime: NOW - 600_000,
    leader,
    self: n === 1,
  });
  const members = [node(1, true), node(2, false)];
  return { clusterName: "vortex-tsd-cluster", self: members[0], members, cacheKeys: 42, ...over };
}
