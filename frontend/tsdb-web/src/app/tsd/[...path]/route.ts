import type { NextRequest } from "next/server";

// Proxies /tsd/* to the TSDB nodes. Read per request rather than at build time, so one image
// can be pointed at any cluster with an environment variable.
//
// VORTEX_API_URLS lists nodes, comma separated. Every node holds a full replica, so any of them
// can answer; when one cannot be reached the next is tried, which keeps the page working while
// a node is down or the leader is changing.
function backends(): string[] {
  const raw = process.env.VORTEX_API_URLS ?? process.env.VORTEX_API_URL ?? "http://localhost:9080";
  return raw
    .split(",")
    .map((s) => s.trim().replace(/\/+$/, ""))
    .filter(Boolean);
}

let next = 0;

async function forward(req: NextRequest, path: string[]) {
  const nodes = backends();
  const target = `/tsd/${path.map(encodeURIComponent).join("/")}${req.nextUrl.search}`;
  const body = req.method === "GET" || req.method === "HEAD" ? undefined : await req.arrayBuffer();
  const start = next++ % nodes.length;
  for (let i = 0; i < nodes.length; i++) {
    const node = nodes[(start + i) % nodes.length];
    try {
      const res = await fetch(node + target, {
        method: req.method,
        headers: { "content-type": req.headers.get("content-type") ?? "application/json" },
        body,
        cache: "no-store",
        signal: AbortSignal.timeout(15000),
      });
      return new Response(res.body, {
        status: res.status,
        headers: { "content-type": res.headers.get("content-type") ?? "application/json" },
      });
    } catch {
      // Unreachable; try the next node
    }
  }
  return Response.json(
    { code: 0, msg: `No TSDB node reachable (${nodes.join(", ")})`, data: null },
    { status: 502 },
  );
}

export async function GET(req: NextRequest, ctx: RouteContext<"/tsd/[...path]">) {
  return forward(req, (await ctx.params).path);
}

export async function POST(req: NextRequest, ctx: RouteContext<"/tsd/[...path]">) {
  return forward(req, (await ctx.params).path);
}
