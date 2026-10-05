#!/usr/bin/env bash
#
# Load test of a running Vortex TSDB (test/load/load.js), through its gateway.
#
#   test/loadtest.sh                                  200, 500, 800 samples/s, 8 minutes each
#   RATES=100,300 HOLD=2m test/loadtest.sh            other rates, 2 minutes each
#   BASE=http://10.0.0.5:9080 test/loadtest.sh        another cluster
#
#   BASE      gateway (or node) address                     http://localhost:9080
#   RATES     write rates to step through, samples/s        200,500,800
#   HOLD      how long each rate is held                    8m
#   DISPLAYS  wall displays polling at the same time        8
#
# Uses k6 when installed, otherwise the grafana/k6 Docker image. Watch the run on the web console
# (System health). The summary is printed at the end and saved to test/load/summary.json.

set -euo pipefail
DIR=$(cd "$(dirname "$0")/load" && pwd)
BASE=${BASE:-http://localhost:9080}
export RATES=${RATES:-200,500,800} HOLD=${HOLD:-8m} DISPLAYS=${DISPLAYS:-8}

curl -fsS -m 5 -o /dev/null "$BASE/tsd/cluster" || { echo "No Vortex TSDB answers at $BASE" >&2; exit 1; }
echo "Load test against $BASE: rates $RATES samples/s, $HOLD each, $DISPLAYS displays"

if command -v k6 >/dev/null; then
  BASE=$BASE k6 run --summary-export "$DIR/summary.json" "$DIR/load.js"
else
  # Inside the container, localhost is the container itself: reach the host instead
  base=${BASE/localhost/host.docker.internal}
  base=${base/127.0.0.1/host.docker.internal}
  docker run --rm -i --add-host host.docker.internal:host-gateway -v "$DIR":/scripts \
    -e BASE="$base" -e RATES -e HOLD -e DISPLAYS \
    grafana/k6 run --summary-export /scripts/summary.json /scripts/load.js
fi
