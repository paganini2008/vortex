# API reference

All endpoints are under `/tsd`, on the gateway (`http://localhost:9080`) or any node. The live
description is at `/swagger-ui.html` and `/v3/api-docs`.

## Response envelope

```json
{"code": 1, "msg": "ok", "data": …, "elapsed": 3, "requestPath": "/tsd/last"}
```

| Field | Meaning |
|---|---|
| `code` | `1` success, `0` failure |
| `msg` | `ok`, or the reason, e.g. `Not a number: abc` |
| `data` | The result; see each endpoint |
| `elapsed` | Milliseconds spent on the node |

| HTTP status | When |
|---|---|
| `200` | Success |
| `400` | Bad parameter (unknown type, invalid name, value not a number, window too large) |
| `503` | The cluster cannot take the write right now (e.g. during a leader change) |

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/tsd/push` | Record one sample |
| `POST` | `/tsd/test` | Record a random sample in [1, 10000) |
| `GET` | `/tsd/last` | Instant value of a series |
| `GET` | `/tsd/retrieve` | Latest 60 buckets (legacy) |
| `GET` | `/tsd/query` | A range, with tumbling or sliding windows |
| `GET` | `/tsd/category` | `/tsd/query` for every series of a category |
| `GET` | `/tsd/categories` | Every category, busiest first |
| `GET` | `/tsd/series` | Every series seen within the retention |
| `GET` | `/tsd/cluster` | Members and the leader |
| `GET` | `/tsd/health` | Every node's API, cache, replication and JVM figures |
| `GET` | `/tsd/health/self` | The same for the node that answers |

## Series parameters

| Parameter | Description | Rules |
|---|---|---|
| `t` | Data type | `long` (integers, exact to 2^53), `double`, `decimal` (8 places) |
| `c` | Category, e.g. a device kind or a service | 1–128 of `A-Z a-z 0-9 _ . -` |
| `d` | Dimension, e.g. a metric name | same as `c` |
| `v` | Value (`/tsd/push` only) | a number; integers only for `long` |

## `POST /tsd/push`

| Input | Output (`data`) |
|---|---|
| `/tsd/push?t=long&c=car&d=speed&v=44` | `{"dataType": "long", "category": "car", "dimension": "speed", "value": "44", "timestamp": 1791182928819}` |
| `/tsd/push?t=long&c=car&d=speed&v=abc` | `code: 0`, `msg: "Not a number: abc"`, HTTP 400 |

## `GET /tsd/last`

| Input | Output (`data`) |
|---|---|
| `/tsd/last?t=long&c=car&d=speed` | `{"value": 44, "timestamp": 1791182928819}`, or `null` when the series has none |

## `GET /tsd/retrieve` (legacy)

| Parameter | Default | Description |
|---|---|---|
| `z` | `UTC` | Zone of the `HH:mm:ss` keys (the legacy Vortex used `Australia/Sydney`) |

```json
{"dataType": "long", "category": "car", "dimension": "speed",
 "data": {
   "09:28:00": {"count": 0, "highestValue": null, "lowestValue": null, "totalValue": null, "averageValue": null, "timestamp": 1790990880000},
   "09:29:00": {"count": 3, "highestValue": 112, "lowestValue": 44, "totalValue": 223, "averageValue": 74.3333, "timestamp": 1790990940000}}}
```

60 buckets (`VORTEX_DISPLAY_SIZE`), oldest first. An empty bucket has `count: 0` and nulls.

## `GET /tsd/query`

| Parameter | Default | Description |
|---|---|---|
| `range` | `1h` | How far back: `30m`, `6h`, `1d`…; at most the retention |
| `step` | ~60 points | Minutes between points; must divide an hour (1–30) or a day (60–720) |
| `window` | `step` | Minutes each point covers. `= step`: tumbling; `> step`: sliding |
| `z` | `UTC` | Zone the points are aligned in |

| `data` field | Contents |
|---|---|
| `last` | Instant value `{value, timestamp}` |
| `current` | The bucket filling now |
| `summary` | The whole range folded into one |
| `points[]` | One aggregate per step, with `from` / `to` of its window |

Each aggregate: `count`, `highestValue`, `lowestValue`, `totalValue`, `averageValue`, `timestamp`.

| Example | Points |
|---|---|
| `range=1h&step=1&window=5` | 60, each a 5-minute moving aggregate |
| `range=6h&step=5&window=15` | 72, sliding |
| `range=1h&step=5` | 12, tumbling |

## `GET /tsd/category`

Same parameters as `/tsd/query` without `t` and `d`; returns a list of `/tsd/query` results, one per series, ordered by dimension.

## `GET /tsd/categories`

```json
[{"category": "car", "series": 4, "lastSeen": 1791182928819, "samplesPerMinute": 3600.0}]
```

`samplesPerMinute` is the average over the last 5 complete minutes; busiest first.

## `GET /tsd/series`

```json
[{"dataType": "long", "category": "car", "dimension": "speed", "lastSeen": 1791182928819}]
```

## `GET /tsd/health`

| Field | Contents |
|---|---|
| `leader`, `clusterName` | The cluster |
| `replicationLag`, `maxReplicationLag` | Versions each node is behind the leader |
| `instances[].http` | `qps`, `errorRate`, `serverErrorRate`, `avgLatencyMs`, request counts (5 s window) |
| `instances[].jvm` | Heap, non-heap, process CPU, load average, GC pauses, threads |
| `instances[].spreader` | openspreader's cache, replication channel and coordination figures |
| `instances[].reachable` | `false` when a node has not reported for 15 s |
