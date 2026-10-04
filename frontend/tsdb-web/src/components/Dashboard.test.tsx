import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NOW, cluster, snapshot } from "@/test/fixtures";
import type { SeriesInfo, SeriesSnapshot } from "@/lib/api";
import { Dashboard } from "./Dashboard";
import Page from "@/app/page";

vi.mock("recharts", async (importOriginal) => {
  const actual = await importOriginal<typeof import("recharts")>();
  const { FixedSizeContainer } = await import("@/test/recharts");
  return { ...actual, ResponsiveContainer: FixedSizeContainer };
});

const catalog: SeriesInfo[] = [
  { dataType: "long", category: "car", dimension: "speed", lastSeen: NOW },
  { dataType: "double", category: "car", dimension: "fuel", lastSeen: NOW },
  { dataType: "long", category: "server", dimension: "requests", lastSeen: NOW },
];

const byCategory: Record<string, SeriesSnapshot[]> = {
  car: [snapshot({ dimension: "fuel", dataType: "double" }), snapshot()],
  server: [snapshot({ category: "server", dimension: "requests" })],
};

interface Backend {
  series?: SeriesInfo[];
  failCategory?: boolean;
  failCluster?: boolean;
}

function backend(b: Backend = {}) {
  const ok = (data: unknown) => new Response(JSON.stringify({ code: 1, msg: "ok", data }));
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = new URL(String(input), "http://web");
    switch (url.pathname) {
      case "/tsd/series":
        return ok(b.series ?? catalog);
      case "/tsd/cluster":
        if (b.failCluster) throw new TypeError("refused");
        return ok(cluster());
      case "/tsd/categories": {
        const names = [...new Set((b.series ?? catalog).map((c) => c.category))];
        return ok(names.map((category, i) => ({ category, series: 1, lastSeen: NOW, samplesPerMinute: 10 - i })));
      }
      case "/tsd/health":
        return ok({ clusterName: "c", leader: "10.0.0.1", maxReplicationLag: 0, replicationLag: {}, instances: [] });
      case "/tsd/category":
        if (b.failCategory) return new Response(JSON.stringify({ code: 0, msg: "range 2d exceeds the retention of 24h" }), { status: 400 });
        return ok(byCategory[url.searchParams.get("c") ?? ""] ?? []);
      default:
        return ok({ value: 1 });
    }
  });
}

function categoryCalls(f: ReturnType<typeof backend>) {
  return f.mock.calls.map((c) => new URL(String(c[0]), "http://web")).filter((u) => u.pathname === "/tsd/category");
}

beforeEach(() => {
  window.history.replaceState(null, "", "/");
  // The fixtures are dated: keep the clock at their time, so "live" does not expire with the date.
  // Only Date is faked; timers stay real for the polling and findBy* waits
  vi.useFakeTimers({ toFake: ["Date"] });
  vi.setSystemTime(NOW);
});

afterEach(() => {
  vi.useRealTimers();
  delete document.documentElement.dataset.theme;
  localStorage.clear();
});

describe("Dashboard", () => {
  it("opens the first category with a card and a tile per dimension", async () => {
    backend();
    render(<Page />);
    expect(await screen.findByRole("heading", { level: 1, name: "car" })).toBeInTheDocument();
    expect(await screen.findByRole("region", { name: "speed" })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "fuel" })).toBeInTheDocument();
    expect(screen.getByText("2 dimensions, 2 live, refreshed every 3 s")).toBeInTheDocument();
    expect(await screen.findByText("leader")).toBeInTheDocument();
  });

  it("switches category from the sidebar and keeps it in the URL", async () => {
    const f = backend();
    render(<Dashboard />);
    const nav = await screen.findByRole("navigation", { name: "Categories" });
    await userEvent.click(await within(nav).findByRole("button", { name: /server/ }));
    expect(await screen.findByRole("region", { name: "requests" })).toBeInTheDocument();
    expect(window.location.search).toBe("?c=server");
    expect(categoryCalls(f).at(-1)?.searchParams.get("c")).toBe("server");
  });

  it("asks for the chosen range and a moving window when smoothing", async () => {
    const f = backend();
    render(<Dashboard />);
    await screen.findByRole("region", { name: "speed" });
    await userEvent.click(screen.getByRole("button", { name: "6h" }));
    await waitFor(() => expect(categoryCalls(f).at(-1)?.searchParams.get("step")).toBe("5"));
    await userEvent.click(screen.getByRole("checkbox"));
    await waitFor(() => expect(categoryCalls(f).at(-1)?.searchParams.get("window")).toBe("25"));
    expect(window.location.search).toBe("?range=6h&smooth=1");
  });

  it("restores the view from the URL", async () => {
    window.history.replaceState(null, "", "/?c=server&range=24h&smooth=1");
    const f = backend();
    render(<Dashboard />);
    await screen.findByRole("region", { name: "requests" });
    const q = categoryCalls(f).at(-1)!.searchParams;
    expect([q.get("c"), q.get("range"), q.get("step"), q.get("window")]).toEqual(["server", "24h", "30", "150"]);
  });

  it("collapses every tile and opens one from its card", async () => {
    backend();
    render(<Dashboard />);
    await screen.findByRole("region", { name: "speed" });
    await userEvent.click(screen.getByRole("button", { name: "Collapse all" }));
    expect(screen.getAllByRole("button", { expanded: false })).toHaveLength(2);
    await userEvent.click(screen.getByRole("button", { name: "Expand all" }));
    expect(screen.getAllByRole("button", { expanded: true })).toHaveLength(2);

    const tileHeader = within(screen.getByRole("region", { name: "speed" })).getByRole("button", { expanded: true });
    await userEvent.click(tileHeader);
    expect(tileHeader).toHaveAttribute("aria-expanded", "false");
    // A card opens its dimension enlarged, and marks and reopens its tile
    await userEvent.click(screen.getAllByRole("button", { pressed: false, name: /speed/ })[0]);
    const dialog = screen.getByRole("dialog", { name: "car / speed" });
    expect(within(dialog).getByText("Samples per point")).toBeInTheDocument();
    expect(tileHeader).toHaveAttribute("aria-expanded", "true");
    // The page's tile, not the enlarged copy in the dialog
    expect(screen.getAllByRole("region", { name: "speed" })[0].className).toContain("ring-2");
    await userEvent.keyboard("{Escape}");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Enlarge fuel" }));
    expect(screen.getByRole("dialog", { name: "car / fuel" })).toBeInTheDocument();
  });

  it("switches to the health and explorer views and back", async () => {
    backend();
    render(<Dashboard />);
    await screen.findByRole("region", { name: "speed" });
    const general = screen.getByRole("navigation", { name: "General" });
    await userEvent.click(within(general).getByRole("button", { name: "System health" }));
    expect(window.location.search).toBe("?view=health");
    expect(await screen.findByRole("heading", { level: 1, name: "System health" })).toBeInTheDocument();
    await userEvent.click(within(general).getByRole("button", { name: "Query explorer" }));
    expect(await screen.findByRole("heading", { level: 1, name: "Query explorer" })).toBeInTheDocument();
    await userEvent.click(within(screen.getByRole("navigation", { name: "Categories" })).getByRole("button", { name: /car/ }));
    expect(await screen.findByRole("heading", { level: 1, name: "car" })).toBeInTheDocument();
    expect(window.location.search).toBe("?c=car");
  });

  it("explains an empty database", async () => {
    backend({ series: [] });
    render(<Dashboard />);
    expect(await screen.findByRole("heading", { name: "No data yet" })).toBeInTheDocument();
    expect(screen.getByText(/\/tsd\/push\?t=long&c=car&d=speed&v=44/)).toBeInTheDocument();
  });

  it("shows query and cluster errors", async () => {
    backend({ failCategory: true, failCluster: true });
    render(<Dashboard />);
    expect(await screen.findByRole("alert")).toHaveTextContent("range 2d exceeds the retention of 24h");
    expect(await screen.findByText("Can't reach the TSDB. Check that a node is running.")).toBeInTheDocument();
  });

  it("says when a category has no series left", async () => {
    window.history.replaceState(null, "", "/?c=gone");
    backend();
    render(<Dashboard />);
    expect(await screen.findByText("gone has no series within the retention period.")).toBeInTheDocument();
  });

  it("pauses, switches theme and opens the sample dialog", async () => {
    const f = backend();
    render(<Dashboard />);
    await screen.findByRole("region", { name: "speed" });
    await userEvent.click(screen.getByRole("button", { name: "Live" }));
    expect(screen.getByRole("button", { name: "Paused" })).toBeInTheDocument();
    const before = categoryCalls(f).length;
    // A paused display still fetches once when the view changes
    await userEvent.click(screen.getByRole("button", { name: "24h" }));
    await waitFor(() => expect(categoryCalls(f).length).toBeGreaterThan(before));

    await userEvent.click(screen.getByRole("button", { name: "Switch to the light theme" }));
    expect(document.documentElement.dataset.theme).toBe("light");

    await userEvent.click(screen.getByRole("button", { name: "Send a sample" }));
    const dialog = screen.getByRole("dialog");
    await userEvent.click(within(dialog).getByRole("button", { name: "Send random" }));
    await userEvent.click(within(dialog).getByRole("button", { name: "Close" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
