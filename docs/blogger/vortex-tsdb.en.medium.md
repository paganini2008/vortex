# Vortex TSDB: Live Metrics in One Command

**Lightweight. Replicated. Real-time.** HTTP writes, minute aggregates, sliding windows — every node holds the whole dataset in memory.

![Vortex TSDB web console](https://paganini2008.github.io/vortex/blogger/assets/dashboard.png)

**Version 1.0.0** · Images: fredfeng033/vortex-tsdb and ghcr.io/paganini2008/vortex-tsdb (amd64 · arm64) · Source: https://github.com/paganini2008/vortex · Apache 2.0

---

## 1. What Problem Does It Solve?

Live metrics need exact counts from many writers, aggregation over any window, and a service that survives a dead node. General-purpose TSDBs bring a disk engine, a query language and several components. Vortex does only this layer, in memory, behind plain HTTP.

- **Many components** → one service + optional Redis
- **Counts drift under concurrency** → writes serialised on the leader, then replicated
- **Windows need a query language** → range · step · window · z, folded at read time
- **One node down, service down** → full replica everywhere; new leader in ~5 s
- **Memory grows with every series** → per-node key limit, overflow to Redis

---

## 2. Quick Start

Free on Docker Hub and GHCR. No JDK, Node or build needed.

```
curl -fsSLO https://raw.githubusercontent.com/paganini2008/vortex/main/deploy/docker-compose.yml
docker compose up -d

curl -X POST 'http://localhost:9080/tsd/push?t=long&c=car&d=speed&v=44'
curl 'http://localhost:9080/tsd/last?t=long&c=car&d=speed'
```

```
{"code":1,"data":{"value":44,"timestamp":1791182928819},"msg":"ok", ...}
```

- **Web console:** http://localhost:9080/
- **Swagger UI:** http://localhost:9080/swagger-ui.html
- **From GHCR:** VORTEX_REGISTRY=ghcr.io/paganini2008 docker compose up -d
- **Your Redis:** VORTEX_REDIS_HOST=host.docker.internal VORTEX_REDIS_PASSWORD=secret docker compose up -d

Nodes take 20–30 s to start.

---

## 3. Requirements

- **Run:** Docker with compose v2
- **Overflow (optional):** Redis, tested with 7.4 and 8.6
- **Build from source:** JDK 17+, Maven 3.9, Node.js 20+
- **Stack:** Spring Boot 4.1 · openspreader · Next.js 16 · Traefik v3

---

## 4. How It Works

![Architecture](https://paganini2008.github.io/vortex/blogger/assets/architecture.png)

![One write, one read](https://paganini2008.github.io/vortex/blogger/assets/dataflow.png)

![Tumbling and sliding windows](https://paganini2008.github.io/vortex/blogger/assets/windows.png)

- **Minute bucket** — max · min · sum · count per series per minute; retention as TTL
- **Latest value** — one hash per category, read in one call
- **Catalog** — every series by its last sample
- **Health** — each node publishes its figures every 5 s

---

## 5. Code Examples

**Example 1 — push and read back**

```
for v in 44 67 112; do curl -X POST "localhost:9080/tsd/push?t=long&c=car&d=speed&v=$v"; done
curl 'localhost:9080/tsd/retrieve?t=long&c=car&d=speed&z=Asia/Shanghai'
```

```
"09:29:00": {"count": 3, "highestValue": 112, "lowestValue": 44, "totalValue": 223, "averageValue": 74.3333}
```

**Example 2 — a sliding window**

```
curl 'localhost:9080/tsd/query?t=long&c=car&d=speed&range=6h&step=5&window=15'
```

72 points, each over the 15 minutes before it. With window = step they are tumbling windows.

![Query explorer](https://paganini2008.github.io/vortex/blogger/assets/query-explorer.png)

**Example 3 — the whole cluster**

```
curl 'localhost:9080/tsd/categories'
curl 'localhost:9080/tsd/health'
```

![System health](https://paganini2008.github.io/vortex/blogger/assets/system-health.png)

![Categories](https://paganini2008.github.io/vortex/blogger/assets/categories.png)

---

## 6. Configuration

```
VORTEX_RETENTION          24h     how long data is kept
VORTEX_SPAN_MINUTES       1       bucket width; divides 60
VORTEX_TIME_ZONE          UTC     zone when a request has no z
VORTEX_CACHE_MAX_KEYS     10000   keys per node in memory
VORTEX_REDIS_HOST         -       overflow Redis; blank deletes keys beyond the limit
VORTEX_SNAPSHOT_INTERVAL  5m      snapshot interval
VORTEX_GATEWAY_PORT       9080    host port
```

---

## 7. Performance & Comparison

Docker Desktop, 4 CPUs / 8 GB, 3 nodes, k6 through the gateway.

- **826,781 writes at rising rates:** 0 failures, identical totals on all nodes
- **Sustained:** ~1,000 samples/s
- **Peak:** 1,400–1,500 samples/s, leader ~2.3 CPUs, heap ≤ 310 MB
- **kill -9 the leader at 600/s:** new leader in 4.7 s, 0.28% of requests failed
- **Graceful leader stop:** immediate hand-over, 0 failures

```
             Vortex          Prometheus   InfluxDB      TimescaleDB
Writes       HTTP push       pull         HTTP push     SQL
Queries      HTTP params     PromQL       InfluxQL/SQL  SQL
Storage      memory+snap     disk         disk          PostgreSQL
HA           built in        extra setup  by edition    PG tooling
Best for     live metrics    monitoring   time series   analytics
```

---

## 8. Limitations & Trade-offs

- **One leader applies every write** — throughput does not grow with nodes
- **Data in memory** — every node killed at once means back to the last snapshot
- **Async replication** — writes in flight when the leader crashes fail; a few may be stored
- **Redis overflow** — reads of spilled keys cost a round trip
- **No query language, alerting or auth** — keep it on a private network

Not for: ledgers · months of retention · tens of thousands of writes per second.

---

## 9. Summary

1. One command: docker compose up -d, amd64 and arm64
2. Just HTTP: t/c/d/v to write, range/step/window/z to read
3. Exact: leader-serialised writes, no locks
4. Local reads: every node a full replica
5. Windows for free: folded from minute buckets
6. Self-healing: new leader in ~5 s
7. Durable enough: snapshots at stop and every 5 minutes
8. Bounded: key limit per node, optional Redis
9. Observable: /tsd/health and a web console
10. A clear niche: live metrics with days of retention

GitHub: https://github.com/paganini2008/vortex · Website: https://paganini2008.github.io/vortex/
