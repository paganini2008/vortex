// Load test of a running Vortex TSDB, through its gateway. Run it with test/loadtest.sh.
//
// writers   push wave-shaped metrics into 5 categories, at rates stepping up through RATES
// displays  wall displays: a category and the category list every 3 s each
// explorer  6-hour sliding-window queries, 2 per second
import http from "k6/http";
import { check, sleep } from "k6";

const BASE = __ENV.BASE || "http://localhost:9080";
const RATES = (__ENV.RATES || "200,500,800").split(",").map(Number);
const HOLD = __ENV.HOLD || "8m";
const DISPLAYS = Number(__ENV.DISPLAYS || 8);

const T0 = Date.now();
const wave = (periodSeconds, amp, base, noise) =>
  base + amp * Math.sin((2 * Math.PI * (Date.now() - T0)) / 1000 / periodSeconds) + (Math.random() - 0.5) * 2 * noise;

const SERIES = [
  ["long", "checkout-api", "qps", () => Math.max(0, Math.round(wave(600, 180, 520, 35)))],
  ["double", "checkout-api", "latency_ms", () => Math.max(1, wave(420, 18, 62, 9) + (Math.random() < 0.03 ? 80 + Math.random() * 160 : 0)).toFixed(2)],
  ["long", "checkout-api", "errors", () => Math.round(Math.max(0, -Math.log(Math.random()) * 1.6))],
  ["long", "orders", "created", () => Math.max(0, Math.round(wave(900, 9, 24, 4)))],
  ["decimal", "orders", "amount", () => Math.max(1, Math.exp(4.2 + 0.6 * (Math.random() * 2 - 1))).toFixed(2)],
  ["double", "iot-greenhouse", "temperature", () => wave(1200, 2.4, 24.5, 0.25).toFixed(2)],
  ["double", "iot-greenhouse", "humidity", () => wave(1500, 6, 61, 0.8).toFixed(2)],
  ["long", "iot-greenhouse", "co2_ppm", () => Math.round(wave(800, 60, 640, 12))],
  ["long", "fleet", "speed_kmh", () => Math.max(0, Math.round(wave(300, 25, 62, 8)))],
  ["double", "fleet", "fuel_pct", () => Math.max(5, 92 - ((Date.now() - T0) / 60000) * 0.8 + (Math.random() - 0.5) * 0.6).toFixed(2)],
  ["long", "fleet", "rpm", () => Math.max(700, Math.round(wave(300, 600, 2100, 120)))],
  ["long", "payments", "success", () => Math.round(wave(500, 40, 300, 15))],
  ["long", "payments", "declined", () => Math.max(0, Math.round(wave(700, 4, 9, 3)))],
];
const CATEGORIES = [...new Set(SERIES.map((s) => s[1]))];

// One minute to reach each rate, then HOLD at it; one minute down at the end
const stages = RATES.flatMap((r) => [{ target: r, duration: "1m" }, { target: r, duration: HOLD }]);
stages.push({ target: 0, duration: "1m" });
const minutes = (d) => (d.endsWith("m") ? Number(d.slice(0, -1)) : d.endsWith("s") ? Number(d.slice(0, -1)) / 60 : Number(d));
const total = Math.ceil(stages.reduce((m, s) => m + minutes(s.duration), 0));

export const options = {
  discardResponseBodies: true,
  scenarios: {
    writers: {
      executor: "ramping-arrival-rate", exec: "write", startRate: 10, timeUnit: "1s",
      preAllocatedVUs: 50, maxVUs: 500, stages,
    },
    displays: { executor: "constant-vus", exec: "display", vus: DISPLAYS, duration: `${total}m` },
    explorer: { executor: "constant-arrival-rate", exec: "explore", rate: 2, timeUnit: "1s", duration: `${total}m`, preAllocatedVUs: 4 },
  },
  thresholds: {
    "http_req_failed{scenario:writers}": ["rate<0.01"],
    "http_req_duration{name:push}": ["p(95)<500"],
  },
};

export function write() {
  const [t, c, d, value] = SERIES[Math.floor(Math.random() * SERIES.length)];
  const r = http.post(`${BASE}/tsd/push?t=${t}&c=${c}&d=${d}&v=${value()}`, null, { tags: { name: "push" } });
  check(r, { "push 200": (x) => x.status === 200 });
}

export function display() {
  const c = CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)];
  http.get(`${BASE}/tsd/category?c=${c}&range=1h`, { tags: { name: "category" } });
  http.get(`${BASE}/tsd/categories`, { tags: { name: "categories" } });
  sleep(3);
}

export function explore() {
  const [t, c, d] = SERIES[Math.floor(Math.random() * SERIES.length)];
  http.get(`${BASE}/tsd/query?t=${t}&c=${c}&d=${d}&range=6h&step=5&window=15`, { tags: { name: "query" } });
}
