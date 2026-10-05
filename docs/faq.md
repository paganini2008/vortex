# FAQ & troubleshooting

## Start-up

| Symptom | Cause | Fix |
|---|---|---|
| `Pool overlaps with other one on this address space` | `10.203.0.0/24` is taken | `VORTEX_SUBNET_PREFIX=10.77.0 docker compose up -d` |
| `port is already allocated` | `9080` or `9081` in use | `VORTEX_GATEWAY_PORT=9180 VORTEX_DASHBOARD_PORT=9181 docker compose up -d` |
| Swagger UI 404, `/tsd` works | Nodes still starting (20–30 s); the gateway sends everything to the console until they are healthy | Wait for `(healthy)` in `docker compose ps` |
| `container name … is already in use` | Another stack uses the same names | `VORTEX_CONTAINER_PREFIX=vortex2` |
| `pull access denied` / 401 | Stale registry login on this machine | `docker logout` (images are public), or log in again |

## Redis

| Question | Answer |
|---|---|
| Do I need Redis? | No. Without it, keys beyond `VORTEX_CACHE_MAX_KEYS` per node are deleted, oldest first |
| Redis on my laptop is not reached | Use `host.docker.internal`; on Linux Engine also uncomment `extra_hosts` in the compose file |
| Redis goes down | Nodes keep serving from memory; spills fail until it is back. The start-up log warns if it is unreachable |
| Why is a query slow? | It read keys that moved to Redis: one round trip each. Raise `VORTEX_CACHE_MAX_KEYS` |

## Data and failures

| Question | Answer |
|---|---|
| Is data kept across restarts? | Yes: snapshot at stop and every 5 min, in each node's volume. `docker compose down -v` deletes it |
| What if every node is killed at once? | Back to the last snapshot: up to `VORTEX_SNAPSHOT_INTERVAL` lost |
| What happens when the leader dies? | A new leader in about 5 s. Writes in flight fail (HTTP 503/502); a few may still have been stored |
| Should clients retry failed writes? | Only if a duplicate sample is acceptable |
| Can I add nodes to a running cluster? | Membership is fixed at start: add the node to `VORTEX_CLUSTER_PEERS` and recreate the stack |
| Why don't writes get faster with more nodes? | Every write is applied on the leader; nodes add read capacity and redundancy |

## Queries

| Question | Answer |
|---|---|
| `step` rejected | It must divide an hour (1–30) or a day (60–720) |
| Points don't line up with my local hour | Pass `z`, e.g. `z=Asia/Shanghai` |
| An empty bucket shows `null` | No samples in that minute; `count` is `0` |
| Is there a query language? | No, by design: `range`, `step`, `window`, `z` |
