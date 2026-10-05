# Vortex TSDB Web

The web console for [Vortex TSDB](https://hub.docker.com/r/fredfeng033/vortex-tsdb): query any
series with any range, step and window, check each node's health, and browse every category.

![System health](https://paganini2008.github.io/vortex/blogger/assets/system-health.png)

| | |
|---|---|
| Source | https://github.com/paganini2008/vortex |
| Database image | [`fredfeng033/vortex-tsdb`](https://hub.docker.com/r/fredfeng033/vortex-tsdb) |
| Also on GHCR | `docker pull ghcr.io/paganini2008/vortex-tsdb-web:latest` (the same build) |
| Platforms | `linux/amd64`, `linux/arm64` |
| License | Apache 2.0 |

## Tags

The same as `fredfeng033/vortex-tsdb`: use the same tag for both images.

## Quick start

The usual way is the compose file, which runs the console behind a gateway together with a
3-node cluster:

```bash
curl -fsSLO https://raw.githubusercontent.com/paganini2008/vortex/main/deploy/docker-compose.yml
docker compose up -d
# http://localhost:9080/
```

On its own, pointed at nodes or a gateway that already run:

```bash
docker run -d --name vortex-tsdb-web -p 3000:3000 \
  -e VORTEX_API_URLS=http://tsdb-node-1:30080,http://tsdb-node-2:30080 \
  fredfeng033/vortex-tsdb-web
# http://localhost:3000/
```

## Configuration

| Variable | Default | Description |
|---|---|---|
| `VORTEX_API_URLS` | `http://localhost:9080` | Where the console's server sends `/tsd/*`, comma separated: a gateway, or the nodes. Tried in turn when one is down |
| `PORT` | `3000` | HTTP port |

## Pages

| Page | For |
|---|---|
| Query explorer | Any series, range, step and window: chart, table and the matching API call |
| System health | Per node QPS, error rate, cache and Redis overflow, replication lag, leader, JVM |
| Categories | Every category, sortable and searchable; pin the ones you watch |
| Display | One board per category: latest values and trends |

The console runs as the non-root user `node` and stores nothing.
