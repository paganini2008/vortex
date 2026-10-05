# Vortex TSDB: Live Metrics in One Command — HTTP Writes, Minute Aggregates, Sliding Windows, Replicated in Memory

## 1. Overview

> **Lightweight. Replicated. Real-time.**

**Vortex TSDB** is a distributed time series database for live metrics. Push numbers over HTTP;
read per-minute aggregates, instant values and tumbling or sliding windows. Every node holds the
whole dataset in memory.

| Version | Images (amd64 · arm64) | Source | License |
|---|---|---|---|
| **1.0.0** | [`fredfeng033/vortex-tsdb`](https://hub.docker.com/r/fredfeng033/vortex-tsdb) · `ghcr.io/paganini2008/vortex-tsdb` | [GitHub](https://github.com/paganini2008/vortex) | Apache 2.0 |

![Vortex TSDB web console](https://paganini2008.github.io/vortex/blogger/assets/dashboard.png)

## 2. What Problem Does It Solve?

Live metrics — devices online, API QPS, orders, temperatures — need exact counts from many
writers, aggregation by the minute and over any window, and a service that survives a dead node.
General-purpose TSDBs do this with a disk engine, a query language and several components; Vortex
does only this layer, in memory, behind plain HTTP.

| Pain | Vortex |
|---|---|
| Many components to deploy and run | One service + optional Redis; `docker compose up -d` |
| Counts drift under concurrent writers | Writes serialised on the leader, then replicated: exact, no locks |
| Windows need tables or a query language | `range` · `step` · `window` · `z`, folded at read time |
| One node down, service down | Full replica on every node; new leader in ~5 s |
| Memory grows with every series | Per-node key limit; overflow to Redis |

## 3. Quick Start

> **Free on Docker Hub and GHCR** · `linux/amd64` + `linux/arm64` · no JDK, Node or build needed

```bash
curl -fsSLO https://raw.githubusercontent.com/paganini2008/vortex/main/deploy/docker-compose.yml
docker compose up -d                     # 3 nodes + web console + gateway

curl -X POST 'http://localhost:9080/tsd/push?t=long&c=car&d=speed&v=44'
curl 'http://localhost:9080/tsd/last?t=long&c=car&d=speed'
```

```json
{"code":1,"data":{"value":44,"timestamp":1791182928819},"elapsed":1,"msg":"ok","requestPath":"/tsd/last"}
```

| Open | URL |
|---|---|
| Web console | http://localhost:9080/ |
| Swagger UI | http://localhost:9080/swagger-ui.html |

| Also | Command |
|---|---|
| Pull | `docker pull fredfeng033/vortex-tsdb:latest` · `docker pull fredfeng033/vortex-tsdb-web:latest` |
| GHCR | `docker pull ghcr.io/paganini2008/vortex-tsdb:latest` · `VORTEX_REGISTRY=ghcr.io/paganini2008 docker compose up -d` |
| Your Redis | `VORTEX_REDIS_HOST=host.docker.internal VORTEX_REDIS_PASSWORD=secret docker compose up -d` |
| From source | `git clone https://github.com/paganini2008/vortex.git && cd vortex && ./run-docker.sh` |

> Nodes take 20–30 s to start; wait for `(healthy)` in `docker compose ps`.

## 4. Requirements

| To | You need |
|---|---|
| Run the images | Docker with compose v2 (Docker Desktop, Rancher Desktop, OrbStack, Engine 23+) |
| Overflow (optional) | Redis — tested with 7.4 and 8.6 |
| Build from source | JDK 17+, Maven 3.9, Node.js 20+ |
| Stack | Spring Boot 4.1 · openspreader · Next.js 16 · React 19 · Tailwind 4 · Traefik v3 |

## 5. How It Works

![Architecture](https://paganini2008.github.io/vortex/blogger/assets/architecture.png)

![One write, one read](https://paganini2008.github.io/vortex/blogger/assets/dataflow.png)

![Tumbling and sliding windows](https://paganini2008.github.io/vortex/blogger/assets/windows.png)

| Data | Key | Structure |
|---|---|---|
| Minute bucket | `tsd:<type>:<category>:<dimension>:<minute>` | max · min · sum · count; retention as TTL |
| Latest value | `tsd:last:<category>` | Hash: a category in one read |
| Catalog | `tsd:catalog` | Sorted set by last sample |
| Health | `tsd:health` | Hash; each node every 5 s |

## 6. Code Examples

### Example 1 — push and read back

| | |
|---|---|
| **Input** | `for v in 44 67 112; do curl -X POST "localhost:9080/tsd/push?t=long&c=car&d=speed&v=$v"; done` then `curl 'localhost:9080/tsd/retrieve?t=long&c=car&d=speed&z=Asia/Shanghai'` |
| **Execution** | Three samples land in one minute bucket; `retrieve` returns the latest 60 buckets in zone `z` |
| **Output** | `"09:29:00": {"count": 3, "highestValue": 112, "lowestValue": 44, "totalValue": 223, "averageValue": 74.3333}` |

### Example 2 — a sliding window

| | |
|---|---|
| **Input** | `curl 'localhost:9080/tsd/query?t=long&c=car&d=speed&range=6h&step=5&window=15'` |
| **Execution** | 72 points; each folds the 15 minute buckets before it. `window = step` gives tumbling windows |
| **Output** | `{"step": 5, "window": 15, "last": {"value": 61}, "summary": {"count": 21540, …}, "points": [{"from": …, "to": …, "count": 900, "averageValue": 69.8}, …]}` |

![Query explorer](https://paganini2008.github.io/vortex/blogger/assets/query-explorer.png)

### Example 3 — the whole cluster

| | |
|---|---|
| **Input** | `curl 'localhost:9080/tsd/categories'` · `curl 'localhost:9080/tsd/health'` |
| **Execution** | Any node answers from replicated memory |
| **Output** | Categories busiest first; per node QPS, error rate, cache, replication lag, JVM |

![System health](https://paganini2008.github.io/vortex/blogger/assets/system-health.png)

| Categories | Zoom | Light theme |
|---|---|---|
| ![](https://paganini2008.github.io/vortex/blogger/assets/categories.png) | ![](https://paganini2008.github.io/vortex/blogger/assets/zoom.png) | ![](https://paganini2008.github.io/vortex/blogger/assets/dashboard-light.png) |

## 7. Configuration

| Variable | Default | Description |
|---|---|---|
| `VORTEX_RETENTION` | `24h` | How long data is kept |
| `VORTEX_SPAN_MINUTES` | `1` | Bucket width; must divide 60 |
| `VORTEX_TIME_ZONE` | `UTC` | Zone when a request has no `z` |
| `VORTEX_CACHE_MAX_KEYS` | `10000` | Keys per node in memory |
| `VORTEX_REDIS_HOST` | blank | Overflow Redis; blank deletes keys beyond the limit |
| `VORTEX_SNAPSHOT_INTERVAL` | `5m` | Snapshot interval |
| `VORTEX_GATEWAY_PORT` | `9080` | Host port |

All settings: [docs/configuration.md](https://github.com/paganini2008/vortex/blob/main/docs/configuration.md).

## 8. Performance & Comparison

**Environment:** Docker Desktop, 4 CPUs / 8 GB, 3 nodes, k6 through the gateway.

| Scenario | Result |
|---|---|
| 826,781 writes at rising rates | **0 failures**, identical totals on all nodes |
| Sustained | **~1,000 samples/s** |
| Peak | **1,400–1,500 samples/s**, leader ~2.3 CPUs, heap ≤ 310 MB |
| `kill -9` leader at 600/s | new leader in **4.7 s**, 0.28% of requests failed |
| Graceful leader stop | immediate hand-over, 0 failures |

|  | Vortex | Prometheus | InfluxDB | TimescaleDB |
|---|---|---|---|---|
| Writes | HTTP push | pull | HTTP push | SQL |
| Queries | HTTP params | PromQL | InfluxQL / SQL | SQL |
| Storage | memory + snapshots | disk | disk | PostgreSQL |
| HA | built in | extra setup | by edition | PG tooling |
| Best for | live metrics, days | monitoring | time series | analytics |

## 9. Limitations & Trade-offs

| Limit | Consequence |
|---|---|
| One leader applies every write | ~1,000–1,500 samples/s; does not grow with nodes |
| Data in memory | Every node killed at once → back to the last snapshot (≤ 5 min) |
| Async replication | Writes in flight when the leader crashes fail; some may still be stored |
| Redis overflow | Reads of spilled keys cost a network round trip |
| No query language, alerting, auth | Keep it on a private network |

**Not for:** ledgers that must never lose a record · months or years of retention · tens of thousands of writes per second.

## 10. Summary

1. **One command** — `docker compose up -d`, images for amd64 and arm64.
2. **Just HTTP** — push with `t/c/d/v`, query with `range/step/window/z`.
3. **Exact** — leader-serialised writes, no drift, no locks.
4. **Local reads** — every node a full replica.
5. **Windows for free** — tumbling and sliding, folded from minute buckets.
6. **Self-healing** — new leader in ~5 s; graceful stops unnoticed.
7. **Durable enough** — snapshots at stop and every 5 minutes.
8. **Bounded** — key limit per node, optional Redis overflow.
9. **Observable** — `/tsd/health` and a web console out of the box.
10. **A clear niche** — live metrics with days of retention.

**GitHub:** https://github.com/paganini2008/vortex · **Website:** https://paganini2008.github.io/vortex/
