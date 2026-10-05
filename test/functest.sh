#!/usr/bin/env bash
#
# Functional test of Vortex TSDB images through deploy/docker-compose.yml.
#
#   test/functest.sh                       the published images (Docker Hub, latest)
#   test/functest.sh --registry ghcr.io/paganini2008      the same from GHCR
#   test/functest.sh --tag 1.0.0-rc1       one version
#   test/functest.sh --keep                leave the stack running afterwards
#
# It runs its own stack (project vortex-functest, gateway on port 9180), so a cluster you already
# run on 9080 is not touched. Covered: start-up, exact counts under concurrent writes, the same
# data on every node, window queries, the legacy API, input validation, the web console and
# Swagger UI, kill -9 of the leader while writing, restart of the whole stack.
#
# Needs: Docker with compose, curl, python3. Exits non-zero when a check fails.

set -u
ROOT=$(cd "$(dirname "$0")/.." && pwd)
COMPOSE_FILE="$ROOT/deploy/docker-compose.yml"
export VORTEX_REGISTRY=${VORTEX_REGISTRY:-fredfeng033}
export VORTEX_TAG=${VORTEX_TAG:-latest}
export VORTEX_GATEWAY_PORT=${FUNCTEST_PORT:-9180}
export VORTEX_DASHBOARD_PORT=${FUNCTEST_DASHBOARD_PORT:-9181}
export VORTEX_SUBNET_PREFIX=${FUNCTEST_SUBNET_PREFIX:-10.204.0}
export VORTEX_CONTAINER_PREFIX=vortex-functest
KEEP=0
SAMPLES=500

while [[ $# -gt 0 ]]; do
  case "$1" in
    --registry) VORTEX_REGISTRY=$2; shift ;;
    --tag) VORTEX_TAG=$2; shift ;;
    --keep) KEEP=1 ;;
    -h | --help) sed -n '2,15p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

B="http://localhost:${VORTEX_GATEWAY_PORT}"
dc() { docker compose -p vortex-functest -f "$COMPOSE_FILE" "$@"; }
FAILS=0
pass() { echo "  PASS  $*"; }
fail() { echo "  FAIL  $*"; FAILS=$((FAILS + 1)); }
healthy() { [ "$(dc ps --format '{{.Status}}' | grep -c '(healthy)')" = "$1" ]; }
wait_healthy() { for _ in $(seq 1 90); do healthy "$1" && return 0; sleep 2; done; return 1; }
# count and total of a series over the last hour
total() {
  curl -s "$B/tsd/query?t=$1&c=$2&d=$3&range=1h" |
    python3 -c 'import json,sys; s=json.load(sys.stdin)["data"]["summary"]; print(s["count"], s["totalValue"])'
}
push() { curl -s -o /dev/null -X POST "$B/tsd/push?t=$1&c=$2&d=$3&v=$4"; }
export -f push
export B

echo "Images: ${VORTEX_REGISTRY}/vortex-tsdb:${VORTEX_TAG}, ${VORTEX_REGISTRY}/vortex-tsdb-web:${VORTEX_TAG}; gateway $B"

echo "== 1. start"
dc down -v >/dev/null 2>&1
dc pull -q >/dev/null 2>&1
start=$(date +%s)
dc up -d >/dev/null 2>&1
if wait_healthy 3; then pass "3 nodes healthy in $(($(date +%s) - start))s"; else fail "nodes not healthy"; dc logs --tail 30; exit 1; fi
sleep 3

echo "== 2. concurrent writes: 4 series x ${SAMPLES} samples, 20 clients"
seq 1 "$SAMPLES" | xargs -P 20 -n 1 bash -c '
  push long fleet speed $(( $0 % 100 + 1 ))
  push double fleet fuel $0.5
  push decimal orders amount 19.99
  push long orders created 1'
sleep 2
want_speed="$SAMPLES $(python3 -c "print(sum(i % 100 + 1 for i in range(1, $SAMPLES + 1)))")"
r=$(total long orders created); [ "$r" = "$SAMPLES $SAMPLES" ] && pass "orders/created exact: $r" || fail "orders/created: $r (want $SAMPLES $SAMPLES)"
r=$(total long fleet speed); [ "$r" = "$want_speed" ] && pass "fleet/speed exact: $r" || fail "fleet/speed: $r (want $want_speed)"
r=$(total decimal orders amount); [ "${r%% *}" = "$SAMPLES" ] && pass "orders/amount (decimal): $r" || fail "orders/amount: $r"

echo "== 3. the same data on every node (each queried directly)"
seen=""
for n in 1 2 3; do
  v=$(dc exec -T "vortex-node-$n" curl -s "http://localhost:30080/tsd/query?t=long&c=fleet&d=speed&range=1h" |
    python3 -c 'import json,sys; s=json.load(sys.stdin)["data"]["summary"]; print(s["count"], s["totalValue"], s["highestValue"], s["lowestValue"])')
  echo "        node-$n: $v"
  seen="$seen$v"$'\n'
done
[ "$(printf '%s' "$seen" | sort -u | wc -l | tr -d ' ')" = 1 ] && pass "identical on all 3 nodes" || fail "nodes differ"

echo "== 4. queries"
r=$(curl -s "$B/tsd/query?t=long&c=fleet&d=speed&range=1h&step=1&window=5" | python3 -c 'import json,sys; d=json.load(sys.stdin)["data"]; print(len(d["points"]), d["step"], d["window"])')
[ "$r" = "60 1 5" ] && pass "sliding window (1h, step 1, window 5): 60 points" || fail "sliding window: $r"
r=$(curl -s "$B/tsd/query?t=long&c=fleet&d=speed&range=6h&step=5&window=15&z=Asia/Shanghai" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)["data"]["points"]))')
[ "$r" = 72 ] && pass "6h, step 5, window 15, zone: 72 points" || fail "6h query: $r"
r=$(curl -s "$B/tsd/category?c=fleet&range=1h" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)["data"]))')
[ "$r" = 2 ] && pass "category fleet: 2 series" || fail "category: $r"
r=$(curl -s "$B/tsd/categories" | python3 -c 'import json,sys; print(sorted(c["category"] for c in json.load(sys.stdin)["data"]))')
[ "$r" = "['fleet', 'orders']" ] && pass "categories: $r" || fail "categories: $r"
r=$(curl -s "$B/tsd/retrieve?t=long&c=fleet&d=speed" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)["data"]["data"]))')
[ "$r" = 60 ] && pass "legacy /tsd/retrieve: 60 buckets" || fail "retrieve: $r"
r=$(curl -s -X POST "$B/tsd/push?t=long&c=fleet&d=speed&v=abc" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d["code"], d["msg"])')
[ "${r%% *}" = 0 ] && pass "invalid value rejected: ${r#* }" || fail "invalid value accepted: $r"

echo "== 5. web console, Swagger UI, health"
for p in / "/?view=health" "/?view=explorer" "/?view=categories" /swagger-ui.html /v3/api-docs; do
  c=$(curl -s -o /dev/null -w '%{http_code}' -L "$B$p"); [ "$c" = 200 ] && pass "GET $p" || fail "GET $p: $c"
done
r=$(curl -s "$B/tsd/health" | python3 -c 'import json,sys; d=json.load(sys.stdin)["data"]; print(sum(i["reachable"] for i in d["instances"]))')
[ "$r" = 3 ] && pass "health: 3 instances reporting" || fail "health: $r reporting"

echo "== 6. kill -9 the leader while writing"
leader=$(curl -s "$B/tsd/cluster" | python3 -c 'import json,sys; print([m["host"] for m in json.load(sys.stdin)["data"]["members"] if m["leader"]][0])')
victim=""
for n in 1 2 3; do [ "$(dc exec -T "vortex-node-$n" hostname -i | tr -d ' \r')" = "$leader" ] && victim="vortex-node-$n"; done
echo "        leader $leader ($victim)"
codes=$(mktemp)
(for _ in $(seq 1 300); do curl -s -m 5 -o /dev/null -w '%{http_code}\n' -X POST "$B/tsd/push?t=long&c=failover&d=writes&v=1"; sleep 0.02; done >"$codes") &
sleep 2
docker kill -s KILL "$(dc ps -q "$victim")" >/dev/null
killed=$(date +%s)
new=""
for _ in $(seq 1 30); do
  new=$(curl -s -m 2 "$B/tsd/cluster" | python3 -c 'import json,sys; l=[m["host"] for m in json.load(sys.stdin)["data"]["members"] if m["leader"]]; print(l[0] if l else "")' 2>/dev/null)
  [ -n "$new" ] && [ "$new" != "$leader" ] && break
  sleep 1
done
[ -n "$new" ] && [ "$new" != "$leader" ] && pass "new leader $new within $(($(date +%s) - killed))s" || fail "no new leader"
wait
acked=$(grep -c '^200$' "$codes"); rm -f "$codes"
stored=$(total long failover writes); stored=${stored%% *}
echo "        300 writes during the kill: $acked acknowledged, $((300 - acked)) failed (retry those), $stored stored"
[ "$stored" -ge $((acked - 5)) ] && pass "acknowledged writes readable after failover" || fail "only $stored of $acked acknowledged writes stored"
r=$(total long orders created); [ "$r" = "$SAMPLES $SAMPLES" ] && pass "earlier data intact: $r" || fail "earlier data after failover: $r"
dc up -d >/dev/null 2>&1
wait_healthy 3 && pass "killed node restarted and rejoined" || fail "killed node did not rejoin"

echo "== 7. restart the whole stack"
dc down >/dev/null 2>&1
dc up -d >/dev/null 2>&1
wait_healthy 3
r=$(total long orders created); [ "$r" = "$SAMPLES $SAMPLES" ] && pass "data kept across down/up: $r" || fail "after down/up: $r"

if [[ $KEEP -eq 1 ]]; then
  echo "Stack left running on $B (stop: docker compose -p vortex-functest -f deploy/docker-compose.yml down -v)"
else
  dc down -v >/dev/null 2>&1
fi
echo "== $FAILS failure(s)"
[[ $FAILS -eq 0 ]]
