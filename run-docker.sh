#!/usr/bin/env bash
#
# Runs a Vortex TSDB cluster on Docker: N backend nodes on a private network, the web UI, a
# Redis for the cache's overflow, and a Traefik gateway in front.
#
#   ./run-docker.sh                 3 nodes + web UI + Redis + gateway
#   ./run-docker.sh -n 5            5 nodes
#   ./run-docker.sh --no-web        no web UI
#   ./run-docker.sh --no-redis      no Redis: keys beyond each node's limit are dropped
#   ./run-docker.sh status          what is running
#   ./run-docker.sh down            stop and remove the containers, keeping the data
#   ./run-docker.sh down --purge    the same, and delete the data too
#
# Settings: .env at the repository root for this script (see .env.example there), and
# backend/tsdb-service/.env for the nodes, every VORTEX_* setting of which is handed to each node.
# A variable already set in the environment wins over both files, and an option over all.
#
# Everything is reached through the gateway (http://localhost:9080 by default):
#   /                               web UI
#   /tsd/**                         the HTTP API, load balanced across healthy nodes
#   /swagger-ui.html, /v3/api-docs  API documentation
#
# The nodes find each other through fixed IPs on their own subnet and elect a leader among
# themselves. Data lives in memory, replicated to every node. Each node also writes its copy to
# its own volume (vortex-node-<i>-data) when it stops and every few minutes, and the next leader
# loads it, so stopping and starting again keeps the data. Beyond VORTEX_CACHE_MAX_KEYS keys a
# node moves the least recently used to Redis (vortex-redis, on its own volume).

set -euo pipefail

ROOT=$(cd "$(dirname "$0")" && pwd)

# A .env file: NAME=value lines; comments and blank lines skipped; the environment wins
load_env() {
  local file=$1 line name value
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
    name=${line%%=*}
    value=${line#*=}
    [[ "$name" =~ ^[A-Z_][A-Z0-9_]*$ ]] || continue
    [[ -n "${!name+x}" ]] || export "$name=$value"
  done <"$file"
}
load_env "$ROOT/.env"
load_env "$ROOT/backend/tsdb-service/.env"

# Written for the bash 3.2 macOS ships: no ${var,,}
bool() { [[ "$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')" =~ ^(1|true|yes|on)$ ]] && echo 1 || echo 0; }

NODES=${VORTEX_NODES:-3}
WEB=$(bool "${VORTEX_WEB:-true}")
REDIS=$(bool "${VORTEX_REDIS:-true}")
REBUILD=0
PURGE=0
GATEWAY_PORT=${VORTEX_GATEWAY_PORT:-9080}
DASHBOARD_PORT=${VORTEX_DASHBOARD_PORT:-9081}
NODE_PORT_RANGE=${VORTEX_NODE_PORT_RANGE:-50000-60000}
SUBNET_PREFIX=${VORTEX_SUBNET_PREFIX:-}
TRAEFIK_IMAGE=${VORTEX_TRAEFIK_IMAGE:-traefik:v3}
REDIS_IMAGE=${VORTEX_REDIS_IMAGE:-redis:7-alpine}
NETWORK=vortex-tsd-net
BACKEND_IMAGE=vortex-tsdb:latest
WEB_IMAGE=vortex-tsdb-web:latest
LABEL_KEY=com.github.vortex.tsd
LABEL="${LABEL_KEY}=1"

# Settings for the script and the web UI rather than the nodes; the script sets the rest of
# these for each container itself
NOT_FOR_NODES='^VORTEX_(NODES|WEB|REDIS|GATEWAY_PORT|DASHBOARD_PORT|NODE_PORT_RANGE|SUBNET_PREFIX|TRAEFIK_IMAGE|REDIS_IMAGE|API_URLS|WEB_PORT|SERVER_PORT|PORT_RANGE|CLUSTER_PEERS|ADVERTISE_HOST|DATA_DIR)$'

usage() {
  cat <<EOF
Usage: $0 [up] [options]
       $0 status
       $0 down [--purge]

Options (each overrides its .env setting):
  -n, --nodes N        number of TSDB nodes (VORTEX_NODES, default 3)
      --no-web         do not start the web UI (VORTEX_WEB)
      --no-redis       no Redis behind the cache: keys beyond each node's limit are dropped
                       (VORTEX_REDIS); with VORTEX_REDIS_HOST set, that Redis is used instead
      --max-keys N     keys each node holds in memory (VORTEX_CACHE_MAX_KEYS, default 10000)
      --rebuild        rebuild the images even if they exist
      --purge          delete the data volumes (with up: start empty)
  -h, --help           show this help

Ports on this machine:
  gateway              http://localhost:${GATEWAY_PORT}           (VORTEX_GATEWAY_PORT)
  Traefik dashboard    http://localhost:${DASHBOARD_PORT}           (VORTEX_DASHBOARD_PORT)
  node i, direct       a free port in ${NODE_PORT_RANGE}; shown at start and by "status" (VORTEX_NODE_PORT_RANGE)
EOF
}

log() { printf '\033[1m%s\033[0m\n' "$*"; }
fail() { printf 'Error: %s\n' "$*" >&2; exit 1; }

node_name() { echo "vortex-node-$1"; }
node_ip() { echo "${SUBNET_PREFIX}.$((10 + $1))"; }
node_volume() { echo "vortex-node-$1-data"; }

# The host port Docker picked for a node's HTTP port
node_port() {
  docker port "$1" 30080/tcp 2>/dev/null | head -1 | sed 's/.*://'
}

down() {
  local ids
  ids=$(docker ps -aq --filter "label=${LABEL}")
  if [[ -n "$ids" ]]; then
    log "Stopping containers"
    # docker stop, not rm -f: a node writes its data to disk as it shuts down, and SIGKILL
    # would skip that. The grace period covers writing a large cache
    docker stop -t 60 $ids >/dev/null
    docker rm $ids >/dev/null
  fi
  if docker network inspect "$NETWORK" >/dev/null 2>&1; then
    docker network rm "$NETWORK" >/dev/null
  fi
  if [[ $PURGE -eq 1 ]]; then
    ids=$(docker volume ls -q --filter "label=${LABEL}")
    if [[ -n "$ids" ]]; then
      log "Deleting data volumes"
      docker volume rm $ids >/dev/null
    fi
  fi
  log "Stopped"
}

status() {
  docker ps --filter "label=${LABEL}" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
  local name
  for name in $(docker ps --filter "label=${LABEL}" --filter name=vortex-node- --format '{{.Names}}' | sort); do
    echo "  ${name}  direct: http://localhost:$(node_port "$name")"
  done
  if docker ps -q --filter "label=${LABEL}" --filter name=vortex-gateway | grep -q .; then
    echo
    print_cluster
  fi
}

print_cluster() {
  curl -fsS "http://localhost:${GATEWAY_PORT}/tsd/cluster" 2>/dev/null | python3 -c '
import json, sys
d = json.load(sys.stdin)["data"]
print("Cluster %s, %d nodes:" % (d["clusterName"], len(d["members"])))
for m in sorted(d["members"], key=lambda m: m["host"]):
    print("  %-15s %-6s %s" % (m["host"], m["state"].lower(), "leader" if m["leader"] else ""))
' || echo "Cluster view unavailable through the gateway"
}

# Fixed IPs need a subnet of our own. Take VORTEX_SUBNET_PREFIX if given, otherwise the first
# candidate that does not overlap a network that already exists on this machine.
create_network() {
  local candidates
  if [[ -n "$SUBNET_PREFIX" ]]; then
    candidates=$SUBNET_PREFIX
  else
    candidates="10.230.0 10.231.0 10.232.0 192.168.230 192.168.231"
  fi
  for SUBNET_PREFIX in $candidates; do
    if docker network create --subnet "${SUBNET_PREFIX}.0/24" --label "$LABEL" "$NETWORK" \
        >/dev/null 2>&1; then
      log "Created network ${NETWORK} (${SUBNET_PREFIX}.0/24)"
      return
    fi
  done
  fail "no free subnet among: ${candidates} (set VORTEX_SUBNET_PREFIX, e.g. 10.240.0)"
}

build_images() {
  if [[ $REBUILD -eq 1 ]] || ! docker image inspect "$BACKEND_IMAGE" >/dev/null 2>&1; then
    log "Building ${BACKEND_IMAGE} (the first build downloads dependencies; a few minutes)"
    docker build -t "$BACKEND_IMAGE" "$ROOT/backend/tsdb-service"
  fi
  if [[ $WEB -eq 1 ]] && { [[ $REBUILD -eq 1 ]] || ! docker image inspect "$WEB_IMAGE" >/dev/null 2>&1; }; then
    log "Building ${WEB_IMAGE}"
    docker build -t "$WEB_IMAGE" "$ROOT/frontend/tsdb-web"
  fi
  local image
  for image in "$TRAEFIK_IMAGE" $([[ $REDIS -eq 1 ]] && echo "$REDIS_IMAGE"); do
    if ! docker image inspect "$image" >/dev/null 2>&1; then
      log "Pulling ${image}"
      docker pull -q "$image" >/dev/null
    fi
  done
}

port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# Something else listening on a port Docker publishes takes the traffic silently, so check first
check_ports() {
  local p busy=""
  for p in "$GATEWAY_PORT:VORTEX_GATEWAY_PORT" "$DASHBOARD_PORT:VORTEX_DASHBOARD_PORT"; do
    port_in_use "${p%%:*}" && busy+="  ${p%%:*} (set ${p#*:})"$'\n'
  done
  [[ -z "$busy" ]] || fail $'these ports are already in use on this machine:\n'"${busy%$'\n'}"
}

# Waits until a URL answers 2xx, failing early if the container behind it has exited.
wait_for() {
  local url=$1 name=$2 deadline=$((SECONDS + 120))
  until curl -fsS "$url" >/dev/null 2>&1; do
    if (( SECONDS > deadline )); then
      docker logs --tail 40 "$name" >&2 || true
      fail "$name did not become ready within 120s"
    fi
    if [[ "$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null)" != "true" ]]; then
      docker logs --tail 40 "$name" >&2 || true
      fail "$name exited"
    fi
    sleep 1
  done
}

# Waits until a container's own HEALTHCHECK reports healthy
wait_healthy() {
  local name=$1 deadline=$((SECONDS + 150)) state
  while true; do
    state=$(docker inspect -f '{{.State.Health.Status}}' "$name" 2>/dev/null || echo gone)
    [[ "$state" == "healthy" ]] && return
    if (( SECONDS > deadline )) || [[ "$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null)" != "true" ]]; then
      docker logs --tail 40 "$name" >&2 || true
      fail "$name did not become healthy ($state)"
    fi
    sleep 1
  done
}

# Keys each node has no room for in memory go here. Append-only file on its own volume, so they
# survive a restart as the nodes' snapshots do
start_redis() {
  log "Starting Redis (cache overflow)"
  docker volume create --label "$LABEL" vortex-redis-data >/dev/null
  docker run -d --name vortex-redis --hostname vortex-redis --label "$LABEL" --network "$NETWORK" \
    -v vortex-redis-data:/data "$REDIS_IMAGE" redis-server --appendonly yes --save "" >/dev/null
}

# Traefik discovers the nodes and the web UI from their labels, so a different -n needs no
# gateway configuration. Only containers carrying the script's label are considered.
start_gateway() {
  log "Starting gateway (Traefik)"
  docker run -d --name vortex-gateway --label "$LABEL" --network "$NETWORK" \
    -p "${GATEWAY_PORT}:80" -p "${DASHBOARD_PORT}:8080" \
    -v /var/run/docker.sock:/var/run/docker.sock:ro \
    "$TRAEFIK_IMAGE" \
    --entrypoints.web.address=:80 \
    --providers.docker=true \
    --providers.docker.exposedbydefault=false \
    --providers.docker.network="$NETWORK" \
    --providers.docker.constraints="Label(\`${LABEL_KEY}\`,\`1\`)" \
    --serverstransport.forwardingtimeouts.dialtimeout=1s \
    --api.dashboard=true --api.insecure=true \
    --ping=true >/dev/null
}

# Every node carries the same router and service labels; Traefik merges them into one service
# with one server per node. The health check takes a node out of rotation within seconds of it
# going down, and the retry covers a request that lands on it before then: a killed container's
# IP simply stops answering, so the gateway's dial timeout is kept to 1s to fail over quickly.
# There is deliberately no response timeout. A retry then only happens when a node could not be
# reached at all, never after it received the request, so a push is not applied twice.
node_labels() {
  cat <<EOF
traefik.enable=true
traefik.http.routers.vortex-api.entrypoints=web
traefik.http.routers.vortex-api.rule=PathPrefix(\`/tsd\`) || PathPrefix(\`/v3/api-docs\`) || PathPrefix(\`/swagger-ui\`)
traefik.http.routers.vortex-api.service=vortex-api
traefik.http.routers.vortex-api.middlewares=vortex-retry
traefik.http.middlewares.vortex-retry.retry.attempts=3
traefik.http.middlewares.vortex-retry.retry.initialinterval=200ms
traefik.http.services.vortex-api.loadbalancer.server.port=30080
traefik.http.services.vortex-api.loadbalancer.healthcheck.path=/actuator/health
traefik.http.services.vortex-api.loadbalancer.healthcheck.interval=2s
traefik.http.services.vortex-api.loadbalancer.healthcheck.timeout=1s
EOF
}

web_labels() {
  cat <<EOF
traefik.enable=true
traefik.http.routers.vortex-web.entrypoints=web
traefik.http.routers.vortex-web.rule=PathPrefix(\`/\`)
traefik.http.routers.vortex-web.priority=1
traefik.http.routers.vortex-web.service=vortex-web
traefik.http.services.vortex-web.loadbalancer.server.port=3000
EOF
}

up() {
  [[ "$NODES" =~ ^[0-9]+$ ]] && (( NODES >= 1 && NODES <= 200 )) || fail "-n must be 1..200"
  [[ -z "${VORTEX_CACHE_MAX_KEYS:-}" || "$VORTEX_CACHE_MAX_KEYS" =~ ^[0-9]+$ ]] || fail "--max-keys must be a number"
  command -v docker >/dev/null || fail "docker is not installed"
  command -v curl >/dev/null || fail "curl is not installed"
  docker info >/dev/null 2>&1 || fail "the Docker daemon is not running"

  # A Redis of the user's own takes the place of the script's
  local own_redis=0
  if [[ -n "${VORTEX_REDIS_HOST:-}" ]]; then
    own_redis=1
    REDIS=0
  fi

  build_images
  # A cluster's membership is fixed at start, so a new size means a fresh cluster. The data
  # carries over: the nodes write it to their volumes as they stop
  down >/dev/null
  check_ports
  create_network
  start_gateway
  [[ $REDIS -eq 1 ]] && start_redis

  local peers="" urls="" i labels
  for ((i = 1; i <= NODES; i++)); do
    peers+="${peers:+,}$(node_ip $i)"
    urls+="${urls:+,}http://$(node_name $i):30080"
  done

  # Every backend VORTEX_* setting from .env or the environment goes to each node
  local settings=() var
  for var in $(compgen -e | grep '^VORTEX_' | grep -Ev "$NOT_FOR_NODES" || true); do
    settings+=(-e "$var=${!var}")
  done
  [[ $REDIS -eq 1 ]] && settings+=(-e VORTEX_REDIS_HOST=vortex-redis)

  log "Starting ${NODES} nodes"
  labels=$(mktemp)
  node_labels >"$labels"
  # All at once: they race for leadership and settle on one, as a real rollout would
  for ((i = 1; i <= NODES; i++)); do
    docker volume create --label "$LABEL" "$(node_volume $i)" >/dev/null
    docker run -d --name "$(node_name $i)" --hostname "$(node_name $i)" --label "$LABEL" \
      --label-file "$labels" \
      --network "$NETWORK" --ip "$(node_ip $i)" \
      -p "${NODE_PORT_RANGE}:30080" \
      -v "$(node_volume $i):/data" \
      ${settings[@]+"${settings[@]}"} \
      -e VORTEX_DATA_DIR=/data \
      -e VORTEX_CLUSTER_PEERS="$peers" \
      -e VORTEX_ADVERTISE_HOST="$(node_ip $i)" \
      "$BACKEND_IMAGE" >/dev/null
  done
  for ((i = 1; i <= NODES; i++)); do
    wait_healthy "$(node_name $i)"
    echo "  $(node_name $i)  $(node_ip $i)  direct: http://localhost:$(node_port "$(node_name $i)")"
  done

  if [[ $WEB -eq 1 ]]; then
    log "Starting web UI"
    web_labels >"$labels"
    # The UI's own proxy is a fallback; behind the gateway the browser's /tsd calls are routed
    # straight to the nodes
    docker run -d --name vortex-web --label "$LABEL" --label-file "$labels" \
      --network "$NETWORK" -e VORTEX_API_URLS="$urls" "$WEB_IMAGE" >/dev/null
  fi
  rm -f "$labels"

  wait_for "http://localhost:${GATEWAY_PORT}/tsd/cluster" vortex-gateway
  [[ $WEB -eq 1 ]] && wait_for "http://localhost:${GATEWAY_PORT}/" vortex-web
  # Membership converges over a few gossip rounds after the nodes report healthy
  sleep 3
  echo
  print_cluster
  echo
  if [[ $own_redis -eq 1 ]]; then
    echo "Redis:       ${VORTEX_REDIS_HOST}:${VORTEX_REDIS_PORT:-6379} (from VORTEX_REDIS_HOST)"
  elif [[ $REDIS -eq 0 ]]; then
    echo "Note: no Redis; beyond ${VORTEX_CACHE_MAX_KEYS:-10000} keys per node the oldest buckets are dropped"
  fi
  [[ $WEB -eq 1 ]] && echo "Web UI:      http://localhost:${GATEWAY_PORT}/"
  echo "API:         http://localhost:${GATEWAY_PORT}/tsd/..."
  echo "Swagger UI:  http://localhost:${GATEWAY_PORT}/swagger-ui.html"
  echo "Traefik:     http://localhost:${DASHBOARD_PORT}/dashboard/"
  echo "Push:        curl -X POST 'http://localhost:${GATEWAY_PORT}/tsd/push?t=long&c=car&d=speed&v=44'"
  echo "Stop:        $0 down          (keeps the data; add --purge to delete it)"
}

CMD=up
while [[ $# -gt 0 ]]; do
  case "$1" in
    up | status | down) CMD=$1 ;;
    -n | --nodes) NODES=${2:-}; shift ;;
    --no-web) WEB=0 ;;
    --no-redis) REDIS=0 ;;
    --max-keys) export VORTEX_CACHE_MAX_KEYS=${2:-}; shift ;;
    --rebuild) REBUILD=1 ;;
    --purge) PURGE=1 ;;
    -h | --help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
  shift
done

"$CMD"
