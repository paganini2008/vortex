import { afterEach, describe, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";
import { GET, POST } from "./route";

const ctx = (path: string[]) => ({ params: Promise.resolve({ path }) }) as unknown as RouteContext<"/tsd/[...path]">;

afterEach(() => {
  delete process.env.VORTEX_API_URLS;
  delete process.env.VORTEX_API_URL;
});

describe("/tsd proxy", () => {
  it("forwards a GET with its query string", async () => {
    process.env.VORTEX_API_URLS = "http://node-1:30080/";
    const f = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response('{"code":1}', { headers: { "content-type": "application/json" } }));
    const res = await GET(new NextRequest("http://web/tsd/series?x=1"), ctx(["series"]));
    expect(res.status).toBe(200);
    expect(await res.text()).toBe('{"code":1}');
    expect(f.mock.calls[0][0]).toBe("http://node-1:30080/tsd/series?x=1");
  });

  it("forwards a POST and its body", async () => {
    process.env.VORTEX_API_URL = "http://single:30080";
    const f = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("{}", { status: 400 }));
    const req = new NextRequest("http://web/tsd/push?v=1", { method: "POST", body: "payload" });
    const res = await POST(req, ctx(["push"]));
    expect(res.status).toBe(400);
    const [url, init] = f.mock.calls[0];
    expect(url).toBe("http://single:30080/tsd/push?v=1");
    expect(init?.method).toBe("POST");
    expect(new TextDecoder().decode(init?.body as ArrayBuffer)).toBe("payload");
  });

  it("tries the next node when one cannot be reached", async () => {
    process.env.VORTEX_API_URLS = "http://a:1, http://b:2";
    const f = vi
      .spyOn(globalThis, "fetch")
      .mockRejectedValueOnce(new TypeError("refused"))
      .mockResolvedValue(new Response("{}"));
    const res = await GET(new NextRequest("http://web/tsd/cluster"), ctx(["cluster"]));
    expect(res.status).toBe(200);
    expect(f).toHaveBeenCalledTimes(2);
  });

  it("answers 502 when no node can be reached", async () => {
    process.env.VORTEX_API_URLS = "http://a:1,http://b:2";
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new TypeError("refused"));
    const res = await GET(new NextRequest("http://web/tsd/cluster"), ctx(["cluster"]));
    expect(res.status).toBe(502);
    expect((await res.json()).msg).toMatch(/No TSDB node reachable/);
  });

  it("defaults to the local gateway", async () => {
    const f = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("{}"));
    await GET(new NextRequest("http://web/tsd/series"), ctx(["series"]));
    expect(f.mock.calls[0][0]).toBe("http://localhost:9080/tsd/series");
  });
});
