import { describe, expect, it, vi } from "vitest";
import { ApiError, categorySnapshot, clusterInfo, listSeries, push, pushRandom, seriesId } from "./api";

function respond(body: unknown, status = 200) {
  // A fresh Response per call: a body can be read only once
  return vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async () => new Response(typeof body === "string" ? body : JSON.stringify(body), { status }));
}

const series = { dataType: "long" as const, category: "car", dimension: "speed" };

describe("api", () => {
  it("unwraps a successful result", async () => {
    const f = respond({ code: 1, msg: "ok", data: [{ category: "car" }] });
    await expect(listSeries()).resolves.toEqual([{ category: "car" }]);
    expect(f).toHaveBeenCalledWith("/tsd/series", expect.objectContaining({ cache: "no-store" }));
  });

  it("raises the server's message on failure", async () => {
    respond({ code: 0, msg: "Not a number: x" }, 400);
    await expect(push(series, "x")).rejects.toThrow(new ApiError("Not a number: x"));
  });

  it("falls back to a generic message when the failure has none", async () => {
    respond({ code: 0, msg: "" }, 400);
    await expect(clusterInfo()).rejects.toThrow("Request failed (400).");
  });

  it("explains an unreachable backend", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new TypeError("fetch failed"));
    await expect(clusterInfo()).rejects.toThrow(/Can't reach the TSDB/);
    respond("<html>bad gateway</html>", 502);
    await expect(clusterInfo()).rejects.toThrow(/Can't reach the TSDB/);
    respond("not json", 404);
    await expect(clusterInfo()).rejects.toThrow("The TSDB answered 404 without a result.");
  });

  it("builds the query strings", async () => {
    const f = respond({ code: 1, msg: "ok", data: {} });
    await push(series, "44");
    expect(f).toHaveBeenLastCalledWith("/tsd/push?t=long&c=car&d=speed&v=44", expect.objectContaining({ method: "POST" }));
    await pushRandom(series);
    expect(f).toHaveBeenLastCalledWith("/tsd/test?t=long&c=car&d=speed", expect.objectContaining({ method: "POST" }));
    await categorySnapshot("car", { range: "6h", step: 5, window: 25, timeZone: "UTC" });
    expect(f).toHaveBeenLastCalledWith("/tsd/category?c=car&range=6h&z=UTC&step=5&window=25", expect.anything());
    await categorySnapshot("car", { range: "1h", timeZone: "UTC" });
    expect(f).toHaveBeenLastCalledWith("/tsd/category?c=car&range=1h&z=UTC", expect.anything());
  });

  it("identifies a series by type, category and dimension", () => {
    expect(seriesId(series)).toBe("long|car|speed");
  });
});
