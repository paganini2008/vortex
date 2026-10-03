import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NOW, cluster, metric, snapshot } from "@/test/fixtures";
import { Sidebar } from "./Sidebar";
import { ClusterCard } from "./ClusterCard";
import { KpiCards } from "./KpiCards";
import { RangeGauge } from "./RangeGauge";
import { DimensionPanel } from "./DimensionPanel";
import { DisplayControls, DisplayHeader } from "./DisplayHeader";
import { PointTooltip, SamplesChart, TrendChart, toChartPoints } from "./TrendCharts";

vi.mock("recharts", async (importOriginal) => {
  const actual = await importOriginal<typeof import("recharts")>();
  const { FixedSizeContainer } = await import("@/test/recharts");
  return { ...actual, ResponsiveContainer: FixedSizeContainer };
});

describe("Sidebar", () => {
  const cat = (category: string, samplesPerMinute: number, lastSeen = 0) => ({ category, series: 2, lastSeen, samplesPerMinute });
  const base = { view: "display" as const, onView: () => {}, pins: [] as string[], active: null as string | null, onSelect: () => {}, onSearch: () => {}, cluster: null, clusterError: null, now: NOW };

  it("lists the busiest with their rate and reports a choice", async () => {
    const onSelect = vi.fn();
    render(<Sidebar {...base} categories={[cat("car", 120, NOW - 1000), cat("server", 0.5, NOW - 3_600_000)]} active="car" onSelect={onSelect} />);
    const nav = screen.getByRole("navigation", { name: "Categories" });
    expect(within(nav).getByRole("button", { name: /car/ })).toHaveAttribute("aria-current", "page");
    expect(within(nav).getByText("120/min")).toBeInTheDocument();
    expect(within(nav).getAllByLabelText("receiving samples")).toHaveLength(1);
    expect(within(nav).getByRole("button", { name: "All categories (2)" })).toBeInTheDocument();
    await userEvent.click(within(nav).getByRole("button", { name: /server/ }));
    expect(onSelect).toHaveBeenCalledWith("server");
  });

  it("shows the top ten, pinned first, the open one, and a way to the rest", async () => {
    const many = Array.from({ length: 14 }, (_, i) => cat(`c${String(i).padStart(2, "0")}`, 100 - i));
    const onView = vi.fn();
    const onSearch = vi.fn();
    render(<Sidebar {...base} categories={many} pins={["c13", "gone"]} active="c12" onView={onView} onSearch={onSearch} />);
    const nav = screen.getByRole("navigation", { name: "Categories" });
    // Pinned c13, then the ten busiest of the rest, then the open c12
    expect(within(nav).getAllByRole("button", { name: /^c\d/ }).map((b) => b.textContent?.slice(0, 3))).toEqual([
      "c13", "c00", "c01", "c02", "c03", "c04", "c05", "c06", "c07", "c08", "c09", "c12",
    ]);
    await userEvent.click(within(nav).getByRole("button", { name: "Browse all (14)…" }));
    expect(onView).toHaveBeenCalledWith("categories");
    await userEvent.click(screen.getByRole("button", { name: /Jump to/ }));
    expect(onSearch).toHaveBeenCalled();
  });

  it("switches to the general views", async () => {
    const onView = vi.fn();
    render(<Sidebar {...base} view="health" onView={onView} categories={[]} />);
    const general = screen.getByRole("navigation", { name: "General" });
    expect(within(general).getByRole("button", { name: "System health" })).toHaveAttribute("aria-current", "page");
    await userEvent.click(within(general).getByRole("button", { name: "Query explorer" }));
    expect(onView).toHaveBeenCalledWith("explorer");
  });

  it("says when it is loading, and when there is nothing yet", () => {
    const { rerender } = render(<Sidebar {...base} categories={null} />);
    expect(screen.getByText("Loading…")).toBeInTheDocument();
    rerender(<Sidebar {...base} categories={[]} />);
    expect(screen.getByText("No data yet.")).toBeInTheDocument();
  });
});

describe("ClusterCard", () => {
  it("shows the members and the leader", () => {
    render(<ClusterCard cluster={cluster()} error={null} now={NOW} />);
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("nodes")).toBeInTheDocument();
    expect(screen.getByText("leader")).toBeInTheDocument();
    expect(screen.getAllByText("up 10m")).toHaveLength(2);
  });

  it("covers a lone node, a vacant leadership, loading and errors", () => {
    const c = cluster();
    const lone = { ...c, members: [{ ...c.members[0], leader: false }] };
    const { rerender } = render(<ClusterCard cluster={lone} error={null} now={NOW} />);
    expect(screen.getByText("node")).toBeInTheDocument();
    expect(screen.getByText("Electing a leader…")).toBeInTheDocument();
    rerender(<ClusterCard cluster={null} error={null} now={NOW} />);
    expect(screen.getByText("Connecting…")).toBeInTheDocument();
    rerender(<ClusterCard cluster={null} error="Can't reach the TSDB." now={NOW} />);
    expect(screen.getByText("Can't reach the TSDB.")).toBeInTheDocument();
  });
});

describe("KpiCards", () => {
  const cards = [
    snapshot(),
    snapshot({ dimension: "fuel", dataType: "double", last: null }),
    snapshot({ dimension: "odometer", last: { value: 12_345_678, timestamp: NOW } }),
  ];

  it("fills the first card until one is chosen, and reports clicks", async () => {
    const onSelect = vi.fn();
    const { rerender } = render(<KpiCards snapshots={cards} selected={null} onSelect={onSelect} rangeLabel="1 hour" now={NOW} />);
    const first = screen.getByRole("button", { name: /speed/ });
    expect(first.className).toContain("bg-primary");
    expect(first).toHaveAttribute("aria-pressed", "false");
    expect(screen.getByText("No sample within the retention period")).toBeInTheDocument();
    expect(screen.getByText("12.35M")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /fuel/ }));
    expect(onSelect).toHaveBeenCalledWith("double|car|fuel");
    rerender(<KpiCards snapshots={cards} selected="double|car|fuel" onSelect={onSelect} rangeLabel="1 hour" now={NOW} />);
    expect(screen.getByRole("button", { name: /fuel/ })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: /speed/ }).className).not.toContain("bg-primary");
  });
});

describe("RangeGauge", () => {
  it("states the position, or that there is none", () => {
    const { rerender, container } = render(<RangeGauge position={0.42} caption="c" />);
    expect(screen.getByRole("img", { name: "42% of the way from lowest to highest" })).toBeInTheDocument();
    expect(container.querySelectorAll("path")).toHaveLength(2);
    rerender(<RangeGauge position={0} caption="c" />);
    expect(container.querySelectorAll("path")).toHaveLength(1);
    rerender(<RangeGauge position={null} caption="c" />);
    expect(screen.getByRole("img", { name: "No value to place" })).toBeInTheDocument();
  });
});

describe("TrendCharts", () => {
  const points = toChartPoints(snapshot().points, "UTC");

  it("turns metrics into chart points with a band only where there are samples", () => {
    expect(points[0]).toMatchObject({ label: "09:25", count: 0, range: null });
    expect(points[1]).toMatchObject({ count: 5, average: 51, range: [41, 61] });
  });

  it("explains a point in a tooltip", () => {
    const { rerender } = render(<PointTooltip active label="09:26" points={points} dataType="long" />);
    expect(screen.getByText("Highest")).toBeInTheDocument();
    expect(screen.getByText("61")).toBeInTheDocument();
    rerender(<PointTooltip active label="09:25" points={points} dataType="long" />);
    expect(screen.queryByText("Highest")).not.toBeInTheDocument();
    expect(screen.getByText("Samples")).toBeInTheDocument();
    rerender(<PointTooltip active={false} label="09:26" points={points} dataType="long" />);
    expect(screen.queryByText("Samples")).not.toBeInTheDocument();
  });

  it("draws both charts", () => {
    const { container } = render(
      <>
        <TrendChart points={points} dataType="long" syncId="t" />
        <SamplesChart points={points} dataType="long" syncId="t" />
      </>,
    );
    expect(container.querySelectorAll("svg.recharts-surface").length).toBe(2);
  });
});

describe("DimensionPanel", () => {
  it("collapses and expands, and describes its windows", async () => {
    const onToggle = vi.fn();
    const s = snapshot({ step: 5, window: 25 });
    const { rerender } = render(
      <DimensionPanel snapshot={s} timeZone="UTC" rangeLabel="6 hours" expanded highlighted onToggle={onToggle} now={NOW} />,
    );
    const header = screen.getByRole("button", { name: /speed/ });
    expect(header).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("Moving 25-min window, every 5 min")).toBeInTheDocument();
    expect(screen.getByText(/long, live/)).toBeInTheDocument();
    await userEvent.click(header);
    expect(onToggle).toHaveBeenCalled();
    rerender(<DimensionPanel snapshot={s} timeZone="UTC" rangeLabel="6 hours" expanded={false} highlighted={false} onToggle={onToggle} now={NOW} />);
    expect(screen.queryByText("Samples per point")).not.toBeInTheDocument();
  });

  it("tells live, idle and empty series apart", () => {
    const idle = snapshot({ last: { value: 1, timestamp: NOW - 10 * 60_000 } });
    const { rerender } = render(
      <DimensionPanel snapshot={idle} timeZone="UTC" rangeLabel="1 hour" expanded highlighted={false} onToggle={() => {}} now={NOW} />,
    );
    expect(screen.getByText(/idle, last 10m ago/)).toBeInTheDocument();
    expect(screen.getByText("One point every minute")).toBeInTheDocument();
    const empty = snapshot({ last: null, summary: metric(0, 0, NOW), step: 30, window: 30 });
    rerender(<DimensionPanel snapshot={empty} timeZone="UTC" rangeLabel="1 hour" expanded highlighted={false} onToggle={() => {}} now={NOW} />);
    expect(screen.getByText(/no samples/)).toBeInTheDocument();
    expect(screen.getByText("One point every 30 min")).toBeInTheDocument();
  });
});

describe("DisplayHeader", () => {
  function setup(over: Partial<Parameters<typeof DisplayHeader>[0]> = {}) {
    const props = {
      title: "car",
      subtitle: "3 dimensions",
      range: "1h",
      onRange: vi.fn(),
      smooth: false,
      onSmooth: vi.fn(),
      step: 1,
      timeZone: "UTC",
      zones: ["UTC", "Asia/Shanghai"],
      onTimeZone: vi.fn(),
      live: true,
      onLive: vi.fn(),
      theme: "dark" as const,
      onTheme: vi.fn(),
      onSend: vi.fn(),
      now: NOW,
      ...over,
    };
    render(
      <DisplayHeader {...props}>
        <DisplayControls {...props} />
      </DisplayHeader>,
    );
    return props;
  }

  it("shows the time in the chosen zone", () => {
    setup({ timeZone: "Asia/Shanghai" });
    expect(screen.getByLabelText("Current time")).toHaveTextContent("17:30:00");
  });

  it("reports every control", async () => {
    const p = setup();
    await userEvent.click(screen.getByRole("button", { name: "6h" }));
    expect(p.onRange).toHaveBeenCalledWith("6h");
    await userEvent.click(screen.getByRole("checkbox"));
    expect(p.onSmooth).toHaveBeenCalledWith(true);
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "Asia/Shanghai" } });
    expect(p.onTimeZone).toHaveBeenCalledWith("Asia/Shanghai");
    await userEvent.click(screen.getByRole("button", { name: "Live" }));
    expect(p.onLive).toHaveBeenCalledWith(false);
    await userEvent.click(screen.getByRole("button", { name: "Send a sample" }));
    expect(p.onSend).toHaveBeenCalled();
    await userEvent.click(screen.getByRole("button", { name: "Switch to the light theme" }));
    expect(p.onTheme).toHaveBeenCalledWith("light");
  });

  it("offers the dark theme back and hides sending without a category", () => {
    setup({ theme: "light", live: false, onSend: null });
    expect(screen.getByRole("button", { name: "Switch to the dark theme" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Paused" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Send a sample" })).not.toBeInTheDocument();
  });
});
