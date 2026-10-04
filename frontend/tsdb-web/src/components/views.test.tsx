import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { CacheHealth, ClusterHealth, InstanceHealth, SpreaderSnapshot } from "@/lib/api";
import { NOW, snapshot } from "@/test/fixtures";
import { HealthView, InstanceCard, formatBytes, statusOf } from "./HealthView";
import { ExplorerView } from "./ExplorerView";
import { ZoomDialog } from "./ZoomDialog";

vi.mock("recharts", async (importOriginal) => {
  const actual = await importOriginal<typeof import("recharts")>();
  const { FixedSizeContainer } = await import("@/test/recharts");
  return { ...actual, ResponsiveContainer: FixedSizeContainer };
});

function spreader(cache: CacheHealth, splitting = false): SpreaderSnapshot {
  return {
    cluster: { splitBrain: { healthy: !splitting, splitting, everSplit: splitting, occurrences: splitting ? 1 : 0 } },
    channels: {
      "spreader.cache": {
        throughput: { tps: 120, sentTps: 60, receivedTps: 60, peakTps: 200 },
        rates: { errorRate: 0, sendErrorRate: 0, receiveErrorRate: 0, retryRate: 0 },
        concurrency: { current: 1, peak: 4 },
        counters: { sent: 900, sendFailures: 3, retries: 6, received: 1800, receiveFailures: 0, duplicates: 0 },
        latencyMillis: { outbound: { count: 10, min: 1, avg: 2, max: 9, p50: 1.8, p95: 3, p99: 3.2 }, inbound: { count: 10, min: 0, avg: 0, max: 1, p50: 0, p95: 0, p99: 0.1 } },
      },
    },
    components: {
      cache,
      mutex: { acquired: 5, released: 5, contended: 1, contentionRate: 0.2, avgWaitMillis: 1.5, heldNow: 0, acquireTimeouts: 0 },
      scheduled: { "c/svc/vortex-tsd-catalog-prune": { executed: 3, failed: 1, skipped: 0, avgElapsedMs: 2, lastExecutedAtMs: NOW - 30_000 } },
    },
  };
}

function instance(over: Partial<InstanceHealth> = {}): InstanceHealth {
  return {
    id: "1",
    host: "10.0.0.1",
    serverPort: "30080",
    state: "ALIVE",
    startTime: NOW - 120_000,
    leader: true,
    reportedAt: NOW - 2_000,
    reachable: true,
    error: null,
    spreader: spreader({ appliedVersion: 1000, keyCount: 42, approxBytes: 2048, outboxDepth: 0, outboxOverflow: 0, resyncCount: 0, evicted: 0, bufferedUpdates: 0, syncing: false }),
    http: { qps: 85.4, errorRate: 0, serverErrorRate: 0, avgLatencyMs: 1.5, requests: 900, clientErrors: 0, serverErrors: 0 },
    jvm: {
      status: "UP",
      heapUsed: 64 * 1024 * 1024,
      heapCommitted: 128 * 1024 * 1024,
      heapMax: 1024 * 1024 * 1024,
      nonHeapUsed: 90 * 1024 * 1024,
      processCpu: 0.25,
      systemCpu: 0.5,
      processors: 4,
      gcPauses: 2,
      gcPauseMs: 7.5,
      threads: 60,
      peakThreads: 70,
    },
    ...over,
  };
}

function ok(data: unknown) {
  return new Response(JSON.stringify({ code: 1, msg: "ok", data }));
}

describe("health status", () => {
  it("names what makes an instance degraded or unreachable", () => {
    expect(statusOf(instance()).status).toBe("healthy");
    expect(statusOf(instance({ reachable: false, error: "no report for 20s" }))).toEqual({ status: "unreachable", reasons: ["no report for 20s"] });
    expect(statusOf(instance({ reachable: false, error: null })).reasons).toEqual(["not reporting"]);
    const busy = instance({
      spreader: spreader({ outboxOverflow: 2, syncing: true, spillFailures: 1 }, true),
      http: { qps: 1, errorRate: 0.2, serverErrorRate: 0.05, avgLatencyMs: 1, requests: 1, clientErrors: 0, serverErrors: 1 },
    });
    expect(statusOf(busy)).toEqual({
      status: "degraded",
      reasons: ["split brain", "broadcast queue overflowed", "resynchronising", "spill failures", "server errors"],
    });
    expect(statusOf(instance({ spreader: {} })).status).toBe("healthy");
    const jvmDown = instance();
    jvmDown.jvm = { ...jvmDown.jvm!, status: "DOWN", heapUsed: 950, heapMax: 1000 };
    expect(statusOf(jvmDown).reasons).toEqual(["Spring health DOWN", "heap nearly full"]);
    const clientErrors = instance({ http: { qps: 1, errorRate: 0.1, serverErrorRate: 0, avgLatencyMs: 1, requests: 1, clientErrors: 1, serverErrors: 0 } });
    expect(statusOf(clientErrors).reasons).toEqual(["request errors"]);
  });

  it("formats sizes", () => {
    expect(formatBytes(undefined)).toBe("0 B");
    expect(formatBytes(512)).toBe("512 B");
    expect(formatBytes(2048)).toBe("2.0 KB");
    expect(formatBytes(5 * 1024 * 1024)).toBe("5.0 MB");
  });
});

describe("InstanceCard", () => {
  it("shows the API, cache and replication figures and enlarges", async () => {
    const onZoom = vi.fn();
    render(<InstanceCard i={instance({ leader: false })} lag={3} qpsHistory={[1, 2, 3]} now={NOW} onZoom={onZoom} />);
    const card = screen.getByRole("region", { name: "10.0.0.1:30080" });
    expect(within(card).getByText("Healthy")).toBeInTheDocument();
    expect(within(card).getByText(/Follower, up 2m, reported just now/)).toBeInTheDocument();
    expect(within(card).getByText("85")).toBeInTheDocument();
    expect(within(card).getByText("2.0 KB")).toBeInTheDocument();
    expect(within(card).getByText("3")).toBeInTheDocument();
    expect(within(card).getByText("1.8 ms / 3.2 ms")).toBeInTheDocument();
    expect(within(card).getByText("3 (6 retries)")).toBeInTheDocument();
    expect(within(card).getByText("vortex-tsd-catalog-prune")).toBeInTheDocument();
    expect(within(card).getByText("3 runs, 1 failed, last 30s ago")).toBeInTheDocument();
    expect(within(card).getByText("64.0 MB of 1.0 GB")).toBeInTheDocument();
    expect(within(card).getByRole("meter", { name: "Heap used" })).toHaveAttribute("aria-valuenow", "6");
    await userEvent.click(within(card).getByRole("button", { name: "Enlarge 10.0.0.1" }));
    expect(onZoom).toHaveBeenCalled();
  });

  it("shows only the reason for an unreachable instance", () => {
    render(<InstanceCard i={instance({ reachable: false, error: "no report yet", reportedAt: 0, serverPort: null, http: null, jvm: null, spreader: {} })} lag={undefined} qpsHistory={[]} now={NOW} large />);
    expect(screen.getByText("Unreachable")).toBeInTheDocument();
    expect(screen.getByText("no report yet")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "10.0.0.1:?" })).toBeInTheDocument();
    expect(screen.queryByText("Requests per second")).not.toBeInTheDocument();
  });
});

describe("HealthView", () => {
  const health = (version: number, at: number): ClusterHealth => ({
    clusterName: "c",
    leader: "10.0.0.1",
    maxReplicationLag: 3,
    replicationLag: { "2": 3 },
    instances: [
      instance({ spreader: spreader({ appliedVersion: version, keyCount: 42 }), reportedAt: at }),
      instance({ id: "2", host: "10.0.0.2", leader: false }),
      instance({ id: "3", host: "10.0.0.3", leader: false, reachable: false, error: "no report for 30s" }),
    ],
  });

  it("sums the cluster and enlarges an instance", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(ok(health(1000, NOW - 5_000))).mockResolvedValue(ok(health(6000, NOW)));
    const onSummary = vi.fn();
    const { rerender } = render(<HealthView live={false} now={NOW} onSummary={onSummary} />);
    expect(await screen.findByText("2 / 3")).toBeInTheDocument();
    expect(screen.getByText("171")).toBeInTheDocument();
    expect(screen.getByText("3 versions")).toBeInTheDocument();
    expect(onSummary).toHaveBeenCalledWith("2 of 3 instances reporting, leader 10.0.0.1");
    // A second reading gives the leader's write rate
    rerender(<HealthView live now={NOW} onSummary={onSummary} />);
    const writes = screen.getByText("Cache writes per second").parentElement!;
    await waitFor(() => expect(within(writes).getByText("1,000")).toBeInTheDocument());

    await userEvent.click(screen.getByRole("button", { name: "Enlarge 10.0.0.2" }));
    expect(screen.getByRole("dialog", { name: "10.0.0.2:30080" })).toBeInTheDocument();
    await userEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Close" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("folds an instance to a line of figures, and all of them at once", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(ok(health(1000, NOW)));
    render(<HealthView live={false} now={NOW} />);
    const leader = await screen.findByRole("region", { name: "10.0.0.1:30080" });
    const toggle = within(leader).getByRole("button", { expanded: true });
    expect(within(leader).getByText("Replication")).toBeInTheDocument();

    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(within(leader).queryByText("Replication")).not.toBeInTheDocument();
    expect(within(leader).getByText("QPS")).toBeInTheDocument();
    expect(within(leader).getByText("42")).toBeInTheDocument();
    await userEvent.click(toggle);
    expect(within(leader).getByText("Replication")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Collapse all" }));
    expect(screen.queryByText("Replication")).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Expand all" }));
    expect(screen.getAllByText("Replication")).toHaveLength(2);
  });

  it("reports a failure to load", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new TypeError("refused"));
    render(<HealthView live={false} now={NOW} />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Can't reach the TSDB");
  });
});

describe("ExplorerView", () => {
  const catalog = [
    { dataType: "long" as const, category: "car", dimension: "speed", lastSeen: NOW },
    { dataType: "double" as const, category: "server", dimension: "cpu", lastSeen: NOW },
  ];

  it("queries a picked series and shows the request, chart, table and JSON", async () => {
    const f = vi.spyOn(globalThis, "fetch").mockImplementation(async () => ok(snapshot({ step: 5, window: 25 })));
    render(<ExplorerView catalog={catalog} timeZone="UTC" />);
    await userEvent.type(screen.getByPlaceholderText("Filter series"), "spe");
    expect(within(screen.getByRole("list", { name: "Series" })).getAllByRole("button")).toHaveLength(1);
    await userEvent.click(screen.getByRole("button", { name: /car \/ speed/ }));
    expect(await screen.findByText("/tsd/query?t=long&c=car&d=speed&range=1h&step=1&z=UTC")).toBeInTheDocument();
    expect(f.mock.calls[0][0]).toBe("/tsd/query?t=long&c=car&d=speed&range=1h&step=1&z=UTC");
    expect(screen.getByText(/6 points, one every 5 min, each over 25 min \(sliding\)/)).toBeInTheDocument();
    expect(within(screen.getByRole("region", { name: "Points" })).getAllByRole("row")).toHaveLength(7);
    expect(screen.getByText("Raw response")).toBeInTheDocument();

    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { value: { writeText }, configurable: true });
    await userEvent.click(screen.getByRole("button", { name: "Copy curl" }));
    expect(writeText).toHaveBeenCalledWith("curl 'http://localhost:3000/tsd/query?t=long&c=car&d=speed&range=1h&step=1&z=UTC'");
    expect(await screen.findByRole("button", { name: "Copied" })).toBeInTheDocument();
  });

  it("builds a query from the form, with a window", async () => {
    const f = vi.spyOn(globalThis, "fetch").mockImplementation(async () => ok(snapshot()));
    render(<ExplorerView catalog={[]} timeZone="Asia/Shanghai" />);
    expect(screen.getByText("No series.")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Type"), { target: { value: "double" } });
    await userEvent.type(screen.getByPlaceholderText("car"), "server");
    await userEvent.type(screen.getByPlaceholderText("speed"), "cpu");
    fireEvent.change(screen.getByLabelText("Range"), { target: { value: "6h" } });
    fireEvent.change(screen.getByLabelText("Step (min)"), { target: { value: "5" } });
    await userEvent.type(screen.getByPlaceholderText("= step"), "15");
    await userEvent.click(screen.getByRole("button", { name: "Run query" }));
    await waitFor(() => expect(f).toHaveBeenCalled());
    expect(f.mock.calls[0][0]).toBe("/tsd/query?t=double&c=server&d=cpu&range=6h&step=5&window=15&z=Asia%2FShanghai");
  });

  it("asks for a series and shows query errors", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ code: 0, msg: "step must divide an hour" }), { status: 400 }));
    render(<ExplorerView catalog={catalog} timeZone="UTC" />);
    await userEvent.click(screen.getByRole("button", { name: "Run query" }));
    expect(screen.getByRole("alert")).toHaveTextContent("Choose a series");
    await userEvent.click(screen.getByRole("button", { name: /server \/ cpu/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent("step must divide an hour");
  });

  it("survives a refused clipboard and unexpected failures", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() => {
      throw new Error("boom");
    });
    render(<ExplorerView catalog={catalog} timeZone="UTC" />);
    await userEvent.click(screen.getByRole("button", { name: /car \/ speed/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Can't reach the TSDB");
    Object.defineProperty(navigator, "clipboard", { value: { writeText: vi.fn().mockRejectedValue(new Error("denied")) }, configurable: true });
    await userEvent.click(screen.getByRole("button", { name: "Copy curl" }));
    expect(screen.getByRole("button", { name: "Copy curl" })).toBeInTheDocument();
  });
});

describe("ZoomDialog", () => {
  it("takes focus, locks scrolling and closes on Escape and the backdrop", async () => {
    const onClose = vi.fn();
    const { container, unmount } = render(
      <ZoomDialog title="Zoomed" onClose={onClose}>
        <p>content</p>
      </ZoomDialog>,
    );
    expect(screen.getByRole("dialog", { name: "Zoomed" })).toHaveFocus();
    expect(document.body.style.overflow).toBe("hidden");
    await userEvent.keyboard("{Escape}");
    fireEvent.mouseDown(container.firstElementChild!);
    fireEvent.mouseDown(screen.getByText("content"));
    expect(onClose).toHaveBeenCalledTimes(2);
    unmount();
    expect(document.body.style.overflow).toBe("");
  });
});
