# Vortex TSDB web UI

A wall display for the TSDB: categories down the left, and for the chosen category a card per
dimension with its instant value, then a tile per dimension with its trend (average through the
lowest-to-highest band), samples per point, and where the instant value sits in the range.
Refreshed every 3 seconds. Category, range and smoothing are in the URL, so a screen can be
pointed at a fixed view (`/?c=server&range=6h&smooth=1`). Dark by default; the header switches
to a light theme.

Next.js, React, TypeScript, Tailwind and Recharts.

The browser only calls this app's own origin. Behind the Traefik gateway started by
`../../run-docker.sh`, `/tsd/*` is routed to the nodes directly. Run on its own,
`src/app/tsd/[...path]/route.ts` proxies `/tsd/*` to the nodes in `VORTEX_API_URLS` (comma
separated, read at runtime), trying the next node when one cannot be reached.

```bash
npm install
VORTEX_API_URLS=http://localhost:30081,http://localhost:30082 npm run dev
npm run coverage    # tests, failing below 80% coverage
```

`npm run build` produces a standalone server (`.next/standalone/server.js`), which is what the
Docker image runs.
