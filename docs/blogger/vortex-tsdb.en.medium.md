# Vortex TSDB: A Lightweight Distributed Time Series Database with HTTP Writes, Minute Aggregates and Window Queries

**Lightweight. Replicated. Real-time.** A time series database that starts as a cluster in one command: every node holds the whole dataset, reads stay local, writes replicate themselves.

Vortex TSDB is a lightweight distributed time series database for real-time metrics. Write numeric samples over HTTP; read back per-minute count / max / min / sum / avg, the latest value of every series, and tumbling or sliding windows over any range. The cluster elects its own leader, replicates and snapshots, with no ZooKeeper, broker or external database.

**Version 1.0.0** · Image fredfeng033/vortex-tsdb (amd64 / arm64) · Source https://github.com/paganini2008/vortex · Apache 2.0

![Query explorer: a 5-minute sliding window, one point a minute over the last hour](https://paganini2008.github.io/vortex/blogger/assets/query-explorer.png)

---

## 1. What Problem Does It Solve?

Real-time metrics such as devices online, API QPS, orders or temperature need exact counts under concurrent writers, aggregation by the minute and over any window, and a service that survives a dead node. A general-purpose TSDB can do it, but usually brings a disk storage engine, a query language and several components to operate: heavy for "the last day of live metrics".

Vortex does that one layer only: numeric sample writes, per-minute aggregates, window queries and a highly available cluster, all in memory, with Redis as an optional overflow store.

- **Many components to deploy and run** → one Spring Boot service + optional Redis; one command starts a cluster
- **Inexact counts under concurrent writes** → every write runs on the leader, one at a time, then replicates
- **Window aggregates need tables or a query language** → plain HTTP parameters: range, step, window, z
- **Single point of failure** → a full copy on every node; a new leader within 5 s
- **Unbounded memory** → a per-node key limit; the overflow moves to Redis

---

## 2. Quick Start

**Now on Docker Hub, free to pull.** Ready-made images for linux/amd64 and linux/arm64: no JDK, Node or build needed. latest always carries the newest build.

```
docker pull fredfeng033/vortex-tsdb:latest
docker pull fredfeng033/vortex-tsdb-web:latest
```

**Option 1: the Docker Hub images, nothing to build**

```
curl -fsSLO https://raw.githubusercontent.com/paganini2008/vortex/main/deploy/docker-compose.yml
docker compose up -d   # 3 nodes + web console + gateway; no Redis by default
```

The nodes take 20-30 seconds to start. Until they report healthy the gateway sends every request to the web console: /tsd/... still works through it, Swagger UI answers 404. docker compose ps shows (healthy) on each node when they are ready.

For the Redis overflow, set VORTEX_REDIS_HOST (and VORTEX_REDIS_PASSWORD...) to your own Redis.

**Option 2: build from source**

```
git clone https://github.com/paganini2008/vortex.git
cd vortex
./run-docker.sh            # 3 nodes + Redis + Traefik gateway (with a web console)
```

The first run builds the images (a few minutes). Then:

- **HTTP API**: http://localhost:9080/tsd/...
- **Swagger UI**: http://localhost:9080/swagger-ui.html
- **Web console**: http://localhost:9080/
- **Traefik dashboard**: http://localhost:9081/dashboard/

Every endpoint is in Swagger UI:

![Swagger UI](https://paganini2008.github.io/vortex/blogger/assets/swagger-ui.png)

Push a number and read it straight back:

```
curl -X POST 'http://localhost:9080/tsd/push?t=long&c=car&d=speed&v=44'
curl 'http://localhost:9080/tsd/last?t=long&c=car&d=speed'
# {"code":1,"msg":"ok","data":{"value":44,"timestamp":1791090941234}, ...}
```

Everyday commands:

```
./run-docker.sh -n 5              five nodes
./run-docker.sh --no-web          no web UI
./run-docker.sh --no-redis        no Redis; keys beyond the limit are dropped
./run-docker.sh --max-keys 50000  keys each node keeps in memory
./run-docker.sh status            containers, members, each node's direct port
./run-docker.sh down [--purge]    stop (--purge deletes the data too)
```

---

## 3. Requirements

- **Docker deployment (recommended)**: Docker Desktop, or Docker Engine 23+ with BuildKit
- **Backend, run locally**: JDK 17+ (the image uses 21), Maven 3.9
- **Frontend, run locally**: Node.js 20+
- **Overflow store (optional)**: Redis 7; run-docker.sh brings its own

Stack: Spring Boot 4.1, openspreader, Next.js 16, React 19, TypeScript, Tailwind 4, Recharts 3, Traefik v3.

---

## 4. How It Works

```
                      ┌────────────── Docker network ──────────────┐
  browser / client    │                                            │
  ─────────────────▶ Traefik ── /tsd/**, /swagger-ui ──▶ node 1 ◀─┐ │
     :9080            │   │                             node 2 ◀─┤ gossip, leader,
                      │   │                             node 3 ◀─┘ replicated cache
                      │   │                               │ overflow (leader only)
                      │   │                             Redis      │
                      │   └── / ─────────────────────▶ web UI      │
                      └────────────────────────────────────────────┘
```

The path of one write:

```
POST /tsd/push ──▶ any node ──forward──▶ leader applies 4 cache writes, one at a time
                                          │  max / min / sum(+count) / latest value
                                          ▼
                               replicated to every node ──▶ any node reads locally
```

Data layout:

```
Data            Key                                               Structure
Minute bucket   tsd:<type>:<category>:<dimension>:<bucketStart>   STATS aggregate, retention as TTL
Latest value    tsd:last:<category>                               Hash: a category in one read
Series catalog  tsd:catalog                                       Sorted set, score = last sample
Cluster health  tsd:health                                        Hash, each node every 5 s
```

- **Exact counts without locks**: every write runs on the leader, one at a time, then replicates.
- **Windows are not stored**: an hour, or a 15-minute sliding window, is folded from minute buckets in local memory at read time.
- **Restarts keep the data**: snapshots at shutdown and every 5 minutes; only the leader loads its snapshot, the others copy from it.
- **Bounded memory**: cold keys beyond the limit are written to Redis by the leader and read back from there on a miss.

---

## 5. Code Examples

**Example 1: push, then read the last hour (legacy Vortex API)**

Input:

```
for v in 44 67 112; do
  curl -s -X POST "http://localhost:9080/tsd/push?t=long&c=car&d=speed&v=$v"
done
curl -s 'http://localhost:9080/tsd/retrieve?t=long&c=car&d=speed&z=Asia/Shanghai'
```

Execution: the three samples land in one minute bucket; retrieve returns the latest 60 buckets, labelled in zone z.

Output (abridged):

```
"09:28:00": {"count": 0, "highestValue": null, "lowestValue": null, "averageValue": null},
"09:29:00": {"count": 3, "highestValue": 112, "lowestValue": 44, "totalValue": 223, "averageValue": 74.3333}
```

**Example 2: six hours, a 15-minute sliding window, a point every 5 minutes**

Input:

```
curl -s 'http://localhost:9080/tsd/query?t=long&c=car&d=speed&range=6h&step=5&window=15&z=Asia/Shanghai'
```

Execution: 72 points, each folding the 15 minute buckets before it; with step equal to window they would be tumbling windows.

Output (abridged):

```
"range": "6h", "step": 5, "window": 15,
"last":    {"value": 61, "timestamp": 1791090941234},
"summary": {"count": 21540, "highestValue": 140, "lowestValue": 1, "averageValue": 70.02},
"points":  [{"from": 1791069300000, "to": 1791070200000, "count": 900, "averageValue": 69.8}, ...]
```

**Example 3: a whole category, and the cluster's health**

```
curl -s 'http://localhost:9080/tsd/categories'          # every category, busiest first
curl -s 'http://localhost:9080/tsd/category?c=car&range=1h'
curl -s 'http://localhost:9080/tsd/health'              # each node's QPS, cache, replication, JVM
```

![System health: each node's QPS, cache, replication and JVM](https://paganini2008.github.io/vortex/blogger/assets/system-health.png)

**The bundled web console**

Besides the API, the image ships a web console for development and operations:

- **Query explorer**: any series, range, step and window as chart + table + the matching API call
- **System health**: per node QPS, error rate, cache and overflow, replication lag, leader / split brain, JVM
- **Categories / Display**: browse every series; latest values and trends per category

---

## 6. Configuration

One .env per component (copy its .env.example); environment variables win over the file:

- **.env at the repository root**: read by run-docker.sh; node count, web UI / Redis on or off, ports
- **backend/tsdb-service/.env**: read by every node; storage, cluster, Redis
- **frontend/tsdb-web/.env**: read by the web UI outside Docker; API address, dev port

Common node settings:

```
Variable                   Default        Description
VORTEX_SPAN_MINUTES        1              Bucket width in minutes; must divide 60
VORTEX_RETENTION           24h            How long data is kept
VORTEX_TIME_ZONE           UTC            Zone used when a request has no z
VORTEX_SNAPSHOT_INTERVAL   5m             Snapshot interval; 0 for shutdown only
VORTEX_CACHE_MAX_KEYS      10000          Keys per node in memory
VORTEX_REDIS_HOST          (blank)        Overflow Redis; blank deletes keys beyond the limit
VORTEX_PORT_RANGE          50000-60000    Random HTTP port range when no port is set
```

---

## 7. Performance

Environment: Docker Desktop (4 CPUs, 8 GB), 3 nodes, k6 load through the Traefik gateway.

- **Stepped load**: 826,781 writes, 0 failures, identical totals on all three nodes
- **Knee**: about 1,000 samples/s (about 6,800 cache operations/s on the leader)
- **Peak**: 1,400-1,500 samples/s; leader CPU about 230%, heap ≤ 310 MB
- **kill -9 the leader at 600 samples/s**: a new leader in 4.7 s; 869 of 314,091 requests failed (0.28%), all at the moment of the kill
- **Graceful stop of the leader**: immediate hand-over, no failed request
- **kill -9 every node, then restart**: back to the last snapshot
- **12,000 series with a 10,000-key limit**: 3,393 keys moved to Redis; reads correct

Where it sits next to common choices:

```
              Vortex TSDB         Prometheus     InfluxDB        TimescaleDB
Writes        HTTP push           Periodic pull  HTTP push       SQL
Queries       HTTP parameters     PromQL         InfluxQL / SQL  SQL
Storage       In-memory copies    Local disk     Disk            PostgreSQL
Dependencies  None (Redis opt.)   None           None            PostgreSQL
HA            Built in            Extra setup    By edition      PostgreSQL tooling
Best for      Real-time, days     Monitoring     Time series     Long-term analytics
```

---

## 8. Limitations & Trade-offs

- **Writes do not scale with nodes**: every write is serialised on the leader; about 1,500 samples/s at most.
- **Data lives in memory**: if every node is killed at once, whatever came after the last snapshot (5 minutes by default) is lost.
- **Replication is asynchronous**: writes in flight when the leader crashes fail and should be retried by the client.
- **Redis is slower**: reading a key that moved to Redis is a network round trip; keep the hot set in memory.
- **Precision**: aggregates are doubles; long is exact up to 2^53, decimal is rounded to 8 places.
- **Not included**: a query language, alerting, authentication.

Not a fit for: ledgers or anything that must never lose a record, months or years of retention, tens of thousands of writes per second or more.

---

## 9. Summary

1. **One command for a cluster**: run-docker.sh starts the nodes, gateway and Redis.
2. **Just HTTP**: push to write, query with range / step / window to read.
3. **Any node, any call**: local reads, leader-serialised writes, exact counts.
4. **Tumbling and sliding windows**: folded from minute buckets at no storage cost.
5. **Highly available**: back within 5 s after the leader is killed.
6. **Bounded memory**: a per-node key limit, cold keys overflow to Redis.
7. **Restarts keep the data**: snapshots, loaded by the leader, copied to the rest.
8. **Observable**: /tsd/health reports each node's QPS, cache, replication lag and JVM.
9. **Legacy Vortex compatible**: /tsd/push, /tsd/test and /tsd/retrieve are unchanged.
10. **A clear niche**: a lightweight real-time TSDB with days of retention; leave long-term storage and heavy analytics to a full TSDB.

Project: https://github.com/paganini2008/vortex (Apache 2.0)
