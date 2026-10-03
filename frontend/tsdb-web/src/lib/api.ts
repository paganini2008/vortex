// Client for the Vortex TSDB HTTP API. Requests go to this app's own origin: behind the
// gateway /tsd/* is routed to the nodes, otherwise src/app/tsd/[...path]/route.ts proxies it.

export type DataType = "long" | "double" | "decimal";

export const DATA_TYPES: DataType[] = ["long", "double", "decimal"];

export interface ApiResult<T> {
  code: number; // 1 success, 0 failure
  msg: string;
  data: T;
  elapsed: number;
  requestPath: string;
}

/** One window's aggregate. Values are null when the window holds no samples. */
export interface Metric {
  count: number;
  highestValue: number | null;
  lowestValue: number | null;
  totalValue: number | null;
  averageValue: number | null;
  /** The start of the window's step, epoch ms */
  timestamp: number;
  /** The span the window aggregates; wider than the step for a sliding window */
  from?: number;
  to?: number;
}

export interface LastValue {
  value: number;
  timestamp: number;
}

export interface SeriesSnapshot {
  dataType: DataType;
  category: string;
  dimension: string;
  range: string;
  step: number;
  window: number;
  last: LastValue | null;
  current: Metric;
  summary: Metric;
  points: Metric[];
}

export interface SeriesInfo {
  dataType: DataType;
  category: string;
  dimension: string;
  lastSeen: number;
}

export interface NodeView {
  id: string;
  name: string;
  host: string;
  port: number;
  serverPort: string | null;
  state: string;
  startTime: number;
  leader: boolean;
  self: boolean;
}

export interface ClusterView {
  clusterName: string;
  self: NodeView;
  members: NodeView[];
  cacheKeys: number;
}

export interface Series {
  dataType: DataType;
  category: string;
  dimension: string;
}

export interface RangeQuery {
  range: string;
  /** Minutes each point aggregates; omitted means tumbling windows */
  window?: number;
  step?: number;
  timeZone: string;
}

export class ApiError extends Error {}

const UNREACHABLE = "Can't reach the TSDB. Check that a node is running.";

async function call<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, { cache: "no-store", ...init });
  } catch {
    throw new ApiError(UNREACHABLE);
  }
  let body: ApiResult<T> | null = null;
  try {
    body = (await res.json()) as ApiResult<T>;
  } catch {
    // A proxy error page rather than an ApiResult
  }
  if (!body) {
    throw new ApiError(res.status >= 500 ? UNREACHABLE : `The TSDB answered ${res.status} without a result.`);
  }
  if (body.code !== 1) {
    throw new ApiError(body.msg || `Request failed (${res.status}).`);
  }
  return body.data;
}

function seriesParams(s: Series) {
  return new URLSearchParams({ t: s.dataType, c: s.category, d: s.dimension });
}

export function categorySnapshot(category: string, q: RangeQuery) {
  const p = new URLSearchParams({ c: category, range: q.range, z: q.timeZone });
  if (q.step) p.set("step", String(q.step));
  if (q.window) p.set("window", String(q.window));
  return call<SeriesSnapshot[]>(`/tsd/category?${p}`);
}

export function push(s: Series, value: string) {
  const q = seriesParams(s);
  q.set("v", value);
  return call<Record<string, unknown>>(`/tsd/push?${q}`, { method: "POST" });
}

export function pushRandom(s: Series) {
  return call<Record<string, unknown>>(`/tsd/test?${seriesParams(s)}`, { method: "POST" });
}

export function listSeries() {
  return call<SeriesInfo[]>("/tsd/series");
}

export function clusterInfo() {
  return call<ClusterView>("/tsd/cluster");
}

export function seriesId(s: Series) {
  return `${s.dataType}|${s.category}|${s.dimension}`;
}

export interface HttpRates {
  qps: number;
  /** Share of requests that failed, 4xx and 5xx, over the last few seconds */
  errorRate: number;
  serverErrorRate: number;
  avgLatencyMs: number;
  requests: number;
  clientErrors: number;
  serverErrors: number;
}

export interface LatencyStats {
  count: number;
  min: number;
  avg: number;
  max: number;
  p50: number;
  p95: number;
  p99: number;
}

/** One openspreader channel, as its actuator reports it */
export interface ChannelStats {
  counters?: { sent: number; sendFailures: number; retries: number; received: number; receiveFailures: number; duplicates: number };
  concurrency?: { current: number; peak: number };
  rates?: { errorRate: number; sendErrorRate: number; receiveErrorRate: number; retryRate: number };
  throughput?: { tps: number; sentTps: number; receivedTps: number; peakTps: number };
  latencyMillis?: { outbound?: LatencyStats; inbound?: LatencyStats };
}

export interface ScheduledTaskStats {
  executed: number;
  failed: number;
  skipped: number;
  avgElapsedMs: number;
  lastExecutedAtMs: number;
}

/** openspreader's actuator snapshot (/actuator/spreader), the parts the health view reads */
export interface SpreaderSnapshot {
  cluster?: {
    splitBrain?: { healthy: boolean; splitting: boolean; everSplit: boolean; occurrences: number; holders?: string[] };
  };
  channels?: Record<string, ChannelStats>;
  components?: {
    cache?: CacheHealth;
    mutex?: { acquired: number; released: number; contended: number; contentionRate: number; avgWaitMillis: number; heldNow: number; acquireTimeouts: number };
    scheduled?: Record<string, ScheduledTaskStats>;
  };
}

export interface CacheHealth {
  epoch?: number;
  appliedVersion?: number;
  keyCount?: number;
  approxBytes?: number;
  maxKeys?: number;
  evicted?: number;
  spilled?: number;
  spillFailures?: number;
  bufferedUpdates?: number;
  outboxDepth?: number;
  outboxOverflow?: number;
  framesSent?: number;
  opsSent?: number;
  opsPerFrame?: number;
  opsApplied?: number;
  resyncCount?: number;
  syncing?: boolean;
}

export interface JvmHealth {
  /** Spring Boot's aggregate health: UP, DOWN, OUT_OF_SERVICE, UNKNOWN */
  status: string;
  heapUsed: number;
  heapCommitted: number;
  heapMax: number;
  nonHeapUsed: number;
  /** Share of the available CPUs, 0 to 1 */
  processCpu: number;
  systemCpu: number;
  processors: number;
  /** Collections since the previous reading, and their pause time */
  gcPauses: number;
  gcPauseMs: number;
  threads: number;
  peakThreads: number;
}

export interface InstanceHealth {
  id: string;
  host: string;
  serverPort: string | null;
  state: string;
  startTime: number;
  leader: boolean;
  /** When the instance took this reading, epoch ms; 0 when it never reported */
  reportedAt: number;
  reachable: boolean;
  error: string | null;
  spreader: SpreaderSnapshot;
  http: HttpRates | null;
  jvm: JvmHealth | null;
}

export interface ClusterHealth {
  clusterName: string;
  leader: string | null;
  maxReplicationLag: number;
  /** Versions each follower trails the leader by, from readings of the same round */
  replicationLag: Record<string, number>;
  instances: InstanceHealth[];
}

export function clusterHealth() {
  return call<ClusterHealth>("/tsd/health");
}

export interface SeriesQuery extends RangeQuery {
  series: Series;
}

/** The URL a range query is sent to; shown to developers so they can reuse it. */
export function queryPath(q: SeriesQuery) {
  const p = seriesParams(q.series);
  p.set("range", q.range);
  if (q.step) p.set("step", String(q.step));
  if (q.window) p.set("window", String(q.window));
  p.set("z", q.timeZone);
  return `/tsd/query?${p}`;
}

export function query(q: SeriesQuery) {
  return call<SeriesSnapshot>(queryPath(q));
}

/** A category and how busy it is; the list comes busiest first */
export interface CategoryInfo {
  category: string;
  series: number;
  lastSeen: number;
  /** Mean over the last 5 complete minutes, all series together */
  samplesPerMinute: number;
}

export function listCategories() {
  return call<CategoryInfo[]>("/tsd/categories");
}
