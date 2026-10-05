# Development & tests

## Layout

| Path | Contents |
|---|---|
| `backend/tsdb-service/` | Spring Boot 4.1 service: API, storage on openspreader, Swagger UI |
| `frontend/tsdb-web/` | Next.js 16, React 19, TypeScript, Tailwind 4 web console |
| `deploy/` | `docker-compose.yml` for the published images; Docker Hub descriptions |
| `run-docker.sh` | Builds the images from source and runs a cluster on Docker |
| `test/` | `functest.sh` (images, end to end), `loadtest.sh` + `load/load.js` (k6) |
| `.github/workflows/` | On a `v*` tag: tests, then both images to Docker Hub and GHCR |
| `docs/` | This documentation, the GitHub Pages site and the blog posts |

## Run from source

| Way | Command | Notes |
|---|---|---|
| Docker, from source | `./run-docker.sh` | 3 nodes + Redis + console + gateway on `:9080` |
| A node | `cd backend/tsdb-service && mvn spring-boot:run` | Free port in 50000-60000, logged; nodes on one machine form a cluster |
| The console | `cd frontend/tsdb-web && npm install && npm run dev` | Proxies `/tsd/*` to `VORTEX_API_URLS` |

Needs JDK 17+, Maven 3.9, Node.js 20+.

## Unit tests

| Side | Command | Gate | Report |
|---|---|---|---|
| Backend | `cd backend/tsdb-service && mvn verify` | JaCoCo lines + branches ≥ 80% | `target/site/jacoco` |
| Frontend | `cd frontend/tsdb-web && npm run coverage` | Vitest statements, branches, functions, lines ≥ 80% | `coverage/` |

The Redis overflow tests use a Redis on `localhost:6379` (`VORTEX_TEST_REDIS_HOST`, `_PORT`, `_PASSWORD`) and are skipped without one.

## Functional test

Runs the published images end to end in their own stack (`vortex-functest-*`, gateway `:9180`), so a cluster on `:9080` is untouched.

| Command | Tests |
|---|---|
| `test/functest.sh` | Docker Hub, `latest` |
| `test/functest.sh --registry ghcr.io/paganini2008` | GHCR |
| `test/functest.sh --tag 1.0.0-rc1 --keep` | One version; leave the stack running |

| Step | Checks |
|---|---|
| 1 | 3 nodes healthy |
| 2 | 4 series × 500 concurrent writes: counts and totals exact |
| 3 | Every node, queried directly, holds the same data |
| 4 | Sliding and tumbling queries, categories, legacy `/tsd/retrieve`, invalid input rejected |
| 5 | Web console pages, Swagger UI, `/tsd/health` |
| 6 | `kill -9` the leader while writing: new leader, acknowledged writes kept, node rejoins |
| 7 | `down` + `up`: data kept |

Exits non-zero on a failure.

## Load test

Runs [`test/load/load.js`](../test/load/load.js) against a running cluster with k6 (or the `grafana/k6` image).

| Command | Load |
|---|---|
| `test/loadtest.sh` | 200 → 500 → 800 samples/s, 8 min each |
| `RATES=100,300 HOLD=2m test/loadtest.sh` | Other rates and durations |
| `BASE=http://10.0.0.5:9080 test/loadtest.sh` | Another cluster |
| `DISPLAYS=20 test/loadtest.sh` | 20 wall displays reading |

| Traffic | Shape |
|---|---|
| Writers | 13 series in 5 categories, wave-shaped values |
| Displays | `/tsd/category` + `/tsd/categories` every 3 s each |
| Explorer | 2 sliding-window queries per second |

Watch it on the console's System health page. Fails when more than 1% of writes fail or push p95 exceeds 500 ms; the summary is saved to `test/load/summary.json`.

## Releasing

| Step | |
|---|---|
| 1 | Push to `main` |
| 2 | Tag: `git tag v1.0.0-rc2 && git push origin v1.0.0-rc2` |
| 3 | The workflow tests, then pushes `1.0.0-rc2` and `latest` to Docker Hub and GHCR (release tags also get `X.Y`) |
| 4 | Paste `deploy/dockerhub/*.md` into the Docker Hub overviews when they change |
