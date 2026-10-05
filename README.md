# Vortex TSDB

[![Version](https://img.shields.io/badge/version-1.0.0-blueviolet.svg)](https://github.com/paganini2008/vortex)
[![Docker Hub](https://img.shields.io/badge/docker-fredfeng033%2Fvortex--tsdb-2496ED.svg?logo=docker&logoColor=white)](https://hub.docker.com/r/fredfeng033/vortex-tsdb)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-brightgreen.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![openspreader](https://img.shields.io/badge/cluster-openspreader-blue.svg)](https://github.com/chaconne-ai/openspreader)

Vortex TSDB is a lightweight, distributed time series database for real-time metrics. Push
numeric samples over HTTP; read them back as per-minute aggregates (count, highest, lowest,
total, average), the latest value of every series, and tumbling or sliding windows over the
last day. Every node of the cluster holds the whole dataset in memory, so any node answers any
query from its own copy, and writes it to disk so that a restart keeps the data.

## Features

- **Per-minute aggregates** for three value types: `long`, `double` and `decimal`
- **Instant values**: the latest sample of each series
- **Tumbling and sliding windows**, folded from the minute buckets at query time
- **Self-contained cluster**: no ZooKeeper, no broker. The nodes find each other, elect a
  leader, and each keeps a snapshot on disk
- **Bounded memory, optional Redis**: each node holds at most `VORTEX_CACHE_MAX_KEYS` keys
  (10,000 by default); with a Redis address configured the least recently used move there and
  are read back transparently, without one they are dropped
- **Any node, any call**: writes are replicated to every node; reads never leave the node
- **One entry point**: a Traefik gateway load-balances the API across healthy nodes and serves
  the web UI and Swagger UI
- **Wall display**: one dashboard per category, a tile per dimension with its instant value,
  trend, samples and range, refreshed every 3 seconds; any tile opens full screen
- **Browse many categories**: pinned categories, the 10 busiest, a sortable table of all of them,
  and a ⌘K jump box
- **System health**: per node API rate, error rate and latency, cache size and spill to Redis,
  replication lag, leader and split-brain state, JVM heap, CPU and GC
- **Query explorer** for developers: any series, range, step and window, as a chart, a table and
  the exact API call

## Quick start (Docker, recommended)

### From Docker Hub: nothing to build

Images for `linux/amd64` and `linux/arm64`:
[`fredfeng033/vortex-tsdb`](https://hub.docker.com/r/fredfeng033/vortex-tsdb) and
[`fredfeng033/vortex-tsdb-web`](https://hub.docker.com/r/fredfeng033/vortex-tsdb-web).

```bash
curl -fsSLO https://raw.githubusercontent.com/paganini2008/vortex/main/deploy/docker-compose.yml
docker compose up -d       # 3 nodes + web UI + gateway, on http://localhost:9080
```

No Redis is started; set `VORTEX_REDIS_HOST` (and `VORTEX_REDIS_PASSWORD`...) to use your own for
the cache's overflow. Everything else is in the comments of
[`deploy/docker-compose.yml`](deploy/docker-compose.yml) and on the
[Docker Hub page](https://hub.docker.com/r/fredfeng033/vortex-tsdb).

### From source, with `run-docker.sh`

Requires Docker with BuildKit (Docker Desktop, or Docker Engine 23+).

```bash
git clone https://github.com/paganini2008/vortex.git
cd vortex
./run-docker.sh            # 3 nodes + web UI + gateway
```

The first run builds the images (a few minutes, mostly downloading dependencies). Then:

| | |
|---|---|
| Web UI | http://localhost:9080/ |
| API | http://localhost:9080/tsd/... |
| Swagger UI | http://localhost:9080/swagger-ui.html |
| Traefik dashboard | http://localhost:9081/dashboard/ |
| Node *i*, bypassing the gateway | a free port in 50000-60000, shown by `./run-docker.sh status` |

```bash
./run-docker.sh -n 5             # five nodes
./run-docker.sh --no-web         # API and gateway only, no web UI
./run-docker.sh --no-redis       # no Redis: keys beyond the limit are dropped instead of spilled
./run-docker.sh --max-keys 50000 # keys each node keeps in memory (default 10000)
./run-docker.sh --rebuild        # rebuild the images after changing the code
./run-docker.sh status           # containers and cluster membership
./run-docker.sh down             # stop the containers; the data stays in the volumes
./run-docker.sh down --purge     # stop and delete the data too
```

Each node keeps its data in a Docker volume (`vortex-node-<i>-data`). It writes a snapshot when
it stops and every 5 minutes; at the next start the new leader loads its snapshot and the other
nodes copy it from the leader. So `down` then `./run-docker.sh` keeps the data. Losing one node
loses nothing, since the others hold full copies; if every node is killed at once, the cluster
comes back with the last snapshot, at most 5 minutes old.

By default the script also starts a Redis (`vortex-redis`, volume `vortex-redis-data`) for the
cache's overflow. Setting `VORTEX_REDIS_HOST` in `backend/tsdb-service/.env` points the nodes at
your own Redis instead. Ports are set in the root `.env` (see [Configuration](#configuration));
the script refuses to start if the gateway or dashboard port is already taken.

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

## API

Paths and parameters are unchanged from the legacy Vortex. Every response is wrapped as
`{"code": 1, "msg": "ok", "data": ..., "elapsed": 3, "requestPath": "/tsd/..."}`; `code` is 0
on failure, with the reason in `msg`.

### Push a sample

`POST /tsd/push?t=long&c=car&d=speed&v=44`

| Parameter | Required | Description | Example |
|---|---|---|---|
| `t` | yes | Data type: `long`, `double` or `decimal` | `long` |
| `c` | yes | Category, e.g. a device kind or module | `car` |
| `d` | yes | Dimension, e.g. a metric name | `speed` |
| `v` | yes | The value. A `long` series takes integers only | `44` |

Category and dimension are 1 to 128 letters, digits, `_`, `.` or `-`.
`POST /tsd/test?t=&c=&d=` pushes a random value in [1, 10000).

### Retrieve the last hour

`GET /tsd/retrieve?t=long&c=car&d=speed&z=Asia/Shanghai`

`z` is optional (default `UTC`; the legacy Vortex used `Australia/Sydney`). Returns the latest 60 buckets, oldest first, keyed
by bucket start as `HH:mm:ss` in that zone:

```json
{
  "dataType": "long", "category": "car", "dimension": "speed",
  "data": {
    "09:28:00": {"count": 0, "highestValue": null, "lowestValue": null, "totalValue": null, "averageValue": null, "timestamp": 1790990880000},
    "09:29:00": {"count": 101, "highestValue": 112, "lowestValue": 44, "totalValue": 6831, "averageValue": 67.6337, "timestamp": 1790990940000}
  }
}
```

An empty bucket reports `count: 0` and null values rather than zeros.

### Instant value

`GET /tsd/last?t=long&c=car&d=speed` returns `{"value": 44, "timestamp": 1790990941234}`, the
latest sample, or `null` when the series has none.

### Query a range, with tumbling or sliding windows

`GET /tsd/query?t=long&c=car&d=speed&range=6h&step=5&window=15&z=Asia/Shanghai`

| Parameter | Default | Description |
|---|---|---|
| `range` | `1h` | How far back: `30m`, `6h`, `1d`...; at most the retention |
| `step` | about 60 points | Minutes between points. Must divide an hour (1-30) or a day (60-720) |
| `window` | `step` | Minutes each point aggregates. Equal to `step`: **tumbling** windows, each bucket counted once. Larger: **sliding** windows, e.g. `window=15&step=1` is a 15-minute moving aggregate updated every minute |
| `z` | `UTC` | Zone the steps are aligned in, so hourly points fall on the local hour |

Returns the instant value (`last`), the bucket now filling (`current`), the whole range folded
into one (`summary`), and `points`: one aggregate per step, each with the window it covers in
`from`/`to`.

`GET /tsd/category?c=car&range=6h` returns the same for every series in a category at once; it
is what the dashboard calls.

### Also

| | |
|---|---|
| `GET /tsd/series` | The series that received samples within the retention period |
| `GET /tsd/categories` | Every category with its series count, samples per minute and last sample, busiest first |
| `GET /tsd/cluster` | Cluster members and the current leader |
| `GET /tsd/health` | Every node's API rates, cache and replication figures, and JVM, as the health page shows them |
| `GET /tsd/health/self` | The same for the node that answers |

The full description is at `/swagger-ui.html` and `/v3/api-docs`.

## How it works

Each bucket of each series is one statistical aggregate in the
[openspreader](https://github.com/chaconne-ai/openspreader) replicated cache, under
`tsd:<type>:<category>:<dimension>:<bucketStart>`. A sample is recorded with the cache's `max`,
`min` and `sum` operations; `sum` counts the sample as it goes, so a bucket costs four numbers
no matter how many samples it receives.

The latest sample of each series goes into a hash per category, `tsd:last:<category>`, so a
dashboard reads a whole category's instant values in one lookup. Coarser and sliding windows are
not stored: a query folds the minute buckets it covers (highest of highest, lowest of lowest,
totals and counts added), all from local memory.

Writes are forwarded to the cluster leader, applied there one at a time, and replicated to every
node as operations. That is what makes concurrent writes from many clients and nodes add up
exactly, with no locking in this code. Bucket keys carry the retention as a TTL, so expiry needs
no sweeper.

openspreader writes each node's copy to disk when it stops (`cache.persistent`), and Vortex adds a
snapshot every `vortex.tsd.snapshot-interval`. Only the leader loads its file at start; the other
nodes take a full copy from the leader, so the replicas never diverge. Expiry times are stored as
instants, so buckets that expired while the cluster was down are dropped on load.

A catalog, `tsd:catalog`, records every series with the time of its last sample. It drives
`/tsd/series` and `/tsd/categories`, lets a query skip buckets that cannot exist (newer than the
series' last sample), and is pruned every minute by one node of the cluster.

Each node holds at most `VORTEX_CACHE_MAX_KEYS` keys. With Redis configured, openspreader's
`RedisCacheStore` takes the least recently used beyond that: only the leader writes to Redis, a
key lives either in memory or in Redis, and a read that misses memory looks in Redis. Without
Redis those keys are deleted.

Every 5 seconds each node writes its own health figures (API rates from Micrometer, the cache and
replication figures of `/actuator/spreader`, JVM) into the replicated hash `tsd:health`, so any
node can answer `/tsd/health` for the whole cluster from memory, without calling its peers.

## Configuration

Every setting is a `VORTEX_*` variable, read from a `.env` file per component. Copy the
`.env.example` beside it and edit; `.env` files are not committed. A variable already set in the
environment wins over the file, and a commented-out setting takes its default.

| File | Read by | What it holds |
|---|---|---|
| `.env` (repository root) | `run-docker.sh` | How the cluster is deployed: nodes, web UI, Redis, ports, images |
| `backend/tsdb-service/.env` | each node; `run-docker.sh` hands it to every node | Storage, cluster, Redis |
| `frontend/tsdb-web/.env` | the web UI, outside Docker | Where to send API calls, dev server port |

**Deployment** (root `.env`; `-n`, `--no-web`, `--no-redis` and `--max-keys` override it):

| Variable | Default | Description |
|---|---|---|
| `VORTEX_NODES` | `3` | Nodes to start |
| `VORTEX_WEB` | `true` | Start the web UI |
| `VORTEX_REDIS` | `true` | Start a Redis for the overflow (unless `VORTEX_REDIS_HOST` names one) |
| `VORTEX_GATEWAY_PORT` | `9080` | Web UI, API and Swagger UI |
| `VORTEX_DASHBOARD_PORT` | `9081` | Traefik dashboard |
| `VORTEX_NODE_PORT_RANGE` | `50000-60000` | Where each node's HTTP port is published on the host |

**Nodes** (`backend/tsdb-service/.env`):

| Variable | Default | Description |
|---|---|---|
| `VORTEX_SERVER_PORT` | unset | HTTP port. Unset: a free port in `VORTEX_PORT_RANGE` (`50000-60000`), logged at start-up. `30080` inside Docker |
| `VORTEX_SPAN_MINUTES` | `1` | Bucket width in minutes; must divide 60 |
| `VORTEX_DISPLAY_SIZE` | `60` | Buckets returned by `/tsd/retrieve` |
| `VORTEX_RETENTION` | `24h` | How long a bucket is kept |
| `VORTEX_TIME_ZONE` | `UTC` | Zone used when `z` is omitted. The web UI sends the browser's zone, and lets the viewer pick another |
| `VORTEX_SNAPSHOT_INTERVAL` | `5m` | How often each node writes its copy to disk, besides at shutdown. `0` for shutdown only. Each write briefly pauses writes on the leader |
| `VORTEX_DATA_DIR` | `~/.vortex-tsdb` | Where the snapshot lives; `/data` in Docker |
| `VORTEX_CACHE_MAX_KEYS` | `10000` | Keys **per node** in memory; every node holds a full copy |
| `VORTEX_REDIS_HOST` | blank | Redis for keys beyond the limit. Blank: no Redis, those keys are deleted. With `VORTEX_REDIS_PORT`, `_PASSWORD`, `_DATABASE`, `_KEY_PREFIX` |
| `VORTEX_CLUSTER_NAME` | `vortex-tsd-cluster` | Cluster name; the only isolation between environments |
| `VORTEX_CLUSTER_PEERS` | `127.0.0.1` | Where nodes look for each other |
| `VORTEX_CORS_ORIGINS` | `http://localhost:3000` | Origins that may call the API from a browser directly |

**Web UI** (`frontend/tsdb-web/.env`): `VORTEX_API_URLS`, comma-separated gateway or node
addresses the UI's server proxies `/tsd/*` to (default `http://localhost:9080`, tried in turn when
one is down), and `VORTEX_WEB_PORT` (`3000`) for `npm run dev`. On Docker the UI sits behind the
gateway and needs neither.

## Limits

Know these before relying on it. Figures measured on Docker Desktop (4 CPUs, 8 GB), 3 nodes,
through the gateway.

| | |
|---|---|
| **In memory, with snapshots** | A restart keeps what was in the last snapshot: everything, after a clean stop; up to the last 5 minutes if every node is killed at once |
| **Key budget** | One series at 1-minute buckets and 24-hour retention is up to 1,440 bucket keys. The 10,000 keys per node held in memory are enough for the recent buckets of a few hundred busy series; beyond that, keys go to Redis or are dropped |
| **Redis is slower** | A read of a key that moved to Redis is a network round trip, on any node. Raise `VORTEX_CACHE_MAX_KEYS` so the working set stays in memory |
| **Write ceiling** | Writes are serialised on the leader and do not scale with node count. Each sample is four cache writes. Throughput levels off at about 1,000 samples/s and peaks at 1,400-1,500 samples/s; 826,781 samples at increasing rates with no failure, the same totals on every node |
| **Leader failover** | Killing the leader with `kill -9`: a new leader within 5 s. Writes in flight on the dead leader fail (0.28% of requests at 600 samples/s during the test, nearly all within that second) and should be retried by the client. A graceful stop hands over at once, with no failed request |
| **Writes lost on a leader crash** | Replication is asynchronous. Samples the leader acknowledged but had not yet broadcast are lost when it crashes. Fine for metrics; not for data that must never be lost |
| **Precision** | Aggregates are doubles. `long` values are exact up to 2^53; `decimal` results are rounded to 8 places |

## Local development

Docker is the supported way to run Vortex. To run the pieces directly (JDK 17+, Maven, Node 20+):

```bash
# backend: each instance takes a free port in 50000-60000 and logs it; instances on one machine
# form a cluster by themselves. Settings in backend/tsdb-service/.env
cd backend/tsdb-service && mvn spring-boot:run

# frontend: proxies /tsd/* to VORTEX_API_URLS from frontend/tsdb-web/.env
cd frontend/tsdb-web && npm install && npm run dev
```

Tests, with an 80% coverage gate on both sides:

```bash
cd backend/tsdb-service && mvn verify      # JUnit + JaCoCo, report in target/site/jacoco
cd frontend/tsdb-web && npm run coverage   # Vitest + V8, report in coverage/
```

The Redis overflow tests run against a local Redis when one answers on `localhost:6379`
(`VORTEX_TEST_REDIS_HOST`, `_PORT`, `_PASSWORD` to point them elsewhere), and are skipped otherwise.

## Project layout

| | |
|---|---|
| `backend/tsdb-service/` | Spring Boot service: API, storage on openspreader, Swagger UI |
| `frontend/tsdb-web/` | Next.js, React, TypeScript, Tailwind web UI |
| `run-docker.sh` | Builds the images and runs the cluster, Redis, gateway and UI on Docker |
| `deploy/` | `docker-compose.yml` for the published images, and the Docker Hub descriptions |
| `.github/workflows/` | Publishes both images to Docker Hub for every `v*` tag |
| `.env.example` | Deployment settings for `run-docker.sh`; each component has its own beside its code |

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
