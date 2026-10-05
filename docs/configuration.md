# Configuration

Every setting is a `VORTEX_*` variable. A variable set in the environment wins over a `.env` file,
which wins over the default.

## Where settings are read

| Way of running | Where to set them |
|---|---|
| `deploy/docker-compose.yml` | Environment, or `.env` beside the compose file |
| `run-docker.sh` | `.env` at the repository root (deployment) + `backend/tsdb-service/.env` (nodes) |
| A node from source | `backend/tsdb-service/.env` |
| The web console from source | `frontend/tsdb-web/.env` |

Copy the `.env.example` beside each. `.env` files are not committed.

## Compose (`deploy/docker-compose.yml`)

| Variable | Default | Description |
|---|---|---|
| `VORTEX_REGISTRY` | `fredfeng033` | Image registry: `ghcr.io/paganini2008` for GHCR |
| `VORTEX_TAG` | `latest` | Image tag, e.g. `1.0.0-rc1` |
| `VORTEX_GATEWAY_PORT` | `9080` | API, Swagger UI and web console |
| `VORTEX_DASHBOARD_PORT` | `9081` | Traefik dashboard |
| `VORTEX_SUBNET_PREFIX` | `10.203.0` | The nodes' `/24` subnet; change it if taken |
| `VORTEX_CONTAINER_PREFIX` | `vortex` | Container names (`vortex-node-1`, `vortex-web`, `vortex-traefik`) and network name |
| `VORTEX_CLUSTER_NAME` | `vortex-tsd-cluster` | Cluster name |

Plus every node setting below. More nodes: copy a `vortex-node-N` block, add its IP to `VORTEX_CLUSTER_PEERS` and to the console's `VORTEX_API_URLS`, then recreate the stack.

## Nodes

| Variable | Default | Description |
|---|---|---|
| `VORTEX_RETENTION` | `24h` | How long a bucket is kept |
| `VORTEX_SPAN_MINUTES` | `1` | Bucket width in minutes; must divide 60 |
| `VORTEX_DISPLAY_SIZE` | `60` | Buckets returned by `/tsd/retrieve` |
| `VORTEX_TIME_ZONE` | `UTC` | Zone when a request has no `z` |
| `VORTEX_SNAPSHOT_INTERVAL` | `5m` | Snapshot interval besides shutdown; `0` for shutdown only. Each briefly pauses writes on the leader |
| `VORTEX_DATA_DIR` | `/data` in the image, `~/.vortex-tsdb` from source | Where the snapshot lives |
| `VORTEX_CACHE_MAX_KEYS` | `10000` | Keys per node in memory; every node holds a full copy |
| `VORTEX_REDIS_HOST` | blank | Overflow Redis. Blank: keys beyond the limit are deleted |
| `VORTEX_REDIS_PORT` | `6379` | |
| `VORTEX_REDIS_PASSWORD` | blank | |
| `VORTEX_REDIS_DATABASE` | `0` | |
| `VORTEX_REDIS_KEY_PREFIX` | `vortex:cache:` | Prefix of every key Vortex writes to Redis |
| `VORTEX_CLUSTER_NAME` | `vortex-tsd-cluster` | The same on every node; the only isolation between clusters |
| `VORTEX_CLUSTER_PEERS` | `127.0.0.1` | Every node's address, comma separated |
| `VORTEX_ADVERTISE_HOST` | detected | This node's address as the others reach it; set it in containers |
| `VORTEX_CLUSTER_PORT` | `22000` | Port between nodes; do not publish it |
| `VORTEX_SERVER_PORT` | `30080` in the image; unset from source | HTTP port. Unset: a free port in `VORTEX_PORT_RANGE`, logged at start |
| `VORTEX_PORT_RANGE` | `50000-60000` | Range for that free port |
| `VORTEX_CORS_ORIGINS` | `http://localhost:3000` | Origins that may call the API from a browser directly |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` | JVM options (image) |

### Redis examples

| Redis | Setting |
|---|---|
| On the Docker host (Docker Desktop, Rancher Desktop, OrbStack) | `VORTEX_REDIS_HOST=host.docker.internal VORTEX_REDIS_PASSWORD=secret` |
| On the Docker host, Linux Engine | the same, and uncomment `extra_hosts` in the compose file |
| Elsewhere | `VORTEX_REDIS_HOST=redis.internal.example` |

## `run-docker.sh` (root `.env`)

| Variable | Option | Default | Description |
|---|---|---|---|
| `VORTEX_NODES` | `-n 5` | `3` | Nodes to start |
| `VORTEX_WEB` | `--no-web` | `true` | Start the web console |
| `VORTEX_REDIS` | `--no-redis` | `true` | Start a Redis for the overflow (unless `VORTEX_REDIS_HOST` names one) |
| `VORTEX_CACHE_MAX_KEYS` | `--max-keys 50000` | `10000` | Keys per node |
| `VORTEX_GATEWAY_PORT` | | `9080` | Gateway |
| `VORTEX_DASHBOARD_PORT` | | `9081` | Traefik dashboard |
| `VORTEX_NODE_PORT_RANGE` | | `50000-60000` | Where each node's HTTP port is published |
| `VORTEX_SUBNET_PREFIX` | | free one picked | The nodes' subnet |

| Command | Effect |
|---|---|
| `./run-docker.sh` | Build (first time) and start |
| `./run-docker.sh --rebuild` | Rebuild the images from source |
| `./run-docker.sh status` | Containers, members, each node's direct port |
| `./run-docker.sh down [--purge]` | Stop; `--purge` deletes the data |

## Web console (`frontend/tsdb-web/.env`)

| Variable | Default | Description |
|---|---|---|
| `VORTEX_API_URLS` | `http://localhost:9080` | Where its server sends `/tsd/*`, comma separated, tried in turn |
| `VORTEX_WEB_PORT` | `3000` | `npm run dev` port |
| `PORT` | `3000` | Port of the image |
