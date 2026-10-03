"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  ApiError,
  categorySnapshot,
  clusterInfo,
  listCategories,
  listSeries,
  type CategoryInfo,
  seriesId,
  type ClusterView,
  type SeriesInfo,
  type SeriesSnapshot,
} from "@/lib/api";
import { LIVE_WITHIN_MS, RANGES, SMOOTHING_STEPS, rangeOf } from "@/lib/format";
import { usePins } from "@/lib/pins";
import { applyTheme, storedTheme, type Theme } from "@/lib/theme";
import { Sidebar } from "./Sidebar";
import { DisplayControls, DisplayHeader, ZoneSelect } from "./DisplayHeader";
import { HealthView } from "./HealthView";
import { ExplorerView } from "./ExplorerView";
import type { View } from "./Sidebar";
import { KpiCards } from "./KpiCards";
import { DimensionPanel } from "./DimensionPanel";
import { SendSampleDialog } from "./SendSampleDialog";
import { ZoomDialog } from "./ZoomDialog";
import { CategoriesView } from "./CategoriesView";
import { CommandPalette, type Target } from "./CommandPalette";

export const REFRESH_MS = 3000; // as the first version's page did
const CATALOG_REFRESH_MS = 15000;
const CLUSTER_REFRESH_MS = 5000;

function usePolling(fn: () => void, ms: number, enabled = true) {
  useEffect(() => {
    if (!enabled) return;
    fn();
    const id = setInterval(fn, ms);
    return () => clearInterval(id);
  }, [fn, ms, enabled]);
}

function zones(): string[] {
  try {
    return Intl.supportedValuesOf("timeZone");
  } catch {
    return ["UTC"];
  }
}

interface ViewState {
  view: View;
  category: string | null;
  range: string;
  smooth: boolean;
}

function readUrl(): ViewState {
  const q = new URLSearchParams(window.location.search);
  const v = q.get("view");
  return {
    view: v === "health" || v === "explorer" || v === "categories" ? v : "display",
    category: q.get("c"),
    range: rangeOf(q.get("range")).range,
    smooth: q.get("smooth") === "1",
  };
}

function writeUrl(v: ViewState) {
  const q = new URLSearchParams();
  if (v.view !== "display") q.set("view", v.view);
  if (v.category) q.set("c", v.category);
  if (v.range !== RANGES[0].range) q.set("range", v.range);
  if (v.smooth) q.set("smooth", "1");
  const s = q.toString();
  window.history.replaceState(null, "", s ? `?${s}` : window.location.pathname);
}

/**
 * The wall display: categories down the left, the chosen category's dimensions as tiles, all
 * refreshed every few seconds. Which category, range and smoothing are in the URL, so a
 * screen can be pointed at a fixed view.
 */
export function Dashboard() {
  const [view, setView] = useState<ViewState>({ view: "display", category: null, range: RANGES[0].range, smooth: false });
  const [healthSummary, setHealthSummary] = useState("Gathering every instance's report");
  const [timeZone, setTimeZone] = useState("UTC");
  const [theme, setTheme] = useState<Theme>("dark");
  const [live, setLive] = useState(true);
  const [catalog, setCatalog] = useState<SeriesInfo[] | null>(null);
  const [categories, setCategories] = useState<CategoryInfo[] | null>(null);
  const [palette, setPalette] = useState(false);
  const [snapshots, setSnapshots] = useState<SeriesSnapshot[] | null>(null);
  const [queryError, setQueryError] = useState<string | null>(null);
  const [cluster, setCluster] = useState<ClusterView | null>(null);
  const [clusterError, setClusterError] = useState<string | null>(null);
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const [selected, setSelected] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [zoomed, setZoomed] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const inFlight = useRef(false);

  // Browser-only state, read once after hydration
  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect -- reading browser state after hydration */
    setView(readUrl());
    setTimeZone(Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC");
    const t = storedTheme();
    setTheme(t);
    applyTheme(t);
    /* eslint-enable react-hooks/set-state-in-effect */
  }, []);

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  const refreshCatalog = useCallback(() => {
    listSeries()
      .then(setCatalog)
      .catch(() => setCatalog((c) => c ?? []));
    listCategories()
      .then(setCategories)
      .catch(() => setCategories((c) => c ?? []));
  }, []);
  usePolling(refreshCatalog, CATALOG_REFRESH_MS);

  const refreshCluster = useCallback(() => {
    clusterInfo()
      .then((c) => {
        setCluster(c);
        setClusterError(null);
      })
      .catch((e) => {
        setCluster(null);
        setClusterError(e instanceof ApiError ? e.message : "Cluster status unavailable.");
      });
  }, []);
  usePolling(refreshCluster, CLUSTER_REFRESH_MS);

  const { pins, toggle: togglePin, isPinned } = usePins();
  // Without a choice in the URL, show the first category
  // Without a choice in the URL: the first pinned category, else the busiest
  const active =
    view.category ?? pins.find((p) => (categories ?? []).some((c) => c.category === p)) ?? categories?.[0]?.category ?? null;
  const range = rangeOf(view.range);
  const step = range.step;
  const windowSize = view.smooth ? step * SMOOTHING_STEPS : undefined;

  const refreshCategory = useCallback(async () => {
    if (!active || inFlight.current) return;
    inFlight.current = true;
    try {
      setSnapshots(await categorySnapshot(active, { range: range.range, step, window: windowSize, timeZone }));
      setQueryError(null);
    } catch (e) {
      setQueryError(e instanceof ApiError ? e.message : "The query failed.");
    } finally {
      inFlight.current = false;
    }
  }, [active, range.range, step, windowSize, timeZone]);
  usePolling(refreshCategory, REFRESH_MS, live && active !== null && view.view === "display");

  // A paused display still reloads when what it shows changes
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- one fetch for the new view
    if (!live && view.view === "display") refreshCategory();
  }, [live, refreshCategory, view.view]);

  function update(next: Partial<ViewState>) {
    const v = { ...view, ...next };
    setView(v);
    writeUrl(v);
  }

  function selectCategory(c: string) {
    if (c !== active) {
      setSnapshots(null);
      setSelected(null);
      setCollapsed(new Set());
    }
    update({ category: c, view: "display" });
  }

  /** A card opens its dimension enlarged; the tile below is marked as the one chosen. */
  function selectDimension(id: string) {
    setSelected(id);
    setZoomed(id);
    setCollapsed((s) => {
      const n = new Set(s);
      n.delete(id);
      return n;
    });
  }

  const closeZoom = useCallback(() => setZoomed(null), []);

  // ⌘K / Ctrl+K opens the quick switcher from anywhere
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setPalette((p) => !p);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  function jump(t: Target) {
    setPalette(false);
    if (t.kind === "category") selectCategory(t.category);
    else update({ view: t.view });
  }

  function toggle(id: string) {
    setCollapsed((s) => {
      const n = new Set(s);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });
  }

  function changeTheme(t: Theme) {
    setTheme(t);
    applyTheme(t);
  }

  const shown = (snapshots ?? []).filter((s) => s.category === active);
  const allCollapsed = shown.length > 0 && shown.every((s) => collapsed.has(seriesId(s)));
  const liveCount = shown.filter((s) => s.last && now - s.last.timestamp < LIVE_WITHIN_MS).length;

  const zoomedSnapshot = view.view === "display" ? shown.find((s) => seriesId(s) === zoomed) : undefined;

  const shared = { timeZone, live, onLive: setLive, theme, onTheme: changeTheme, onSend: null, now };

  return (
    <div className="mx-auto flex max-w-[1920px] flex-col gap-4 p-4 md:flex-row">
      <Sidebar
        view={view.view}
        onView={(v) => update({ view: v })}
        categories={categories}
        pins={pins}
        active={active}
        onSelect={selectCategory}
        onSearch={() => setPalette(true)}
        cluster={cluster}
        clusterError={clusterError}
        now={now}
      />

      <div className="flex min-w-0 flex-1 flex-col gap-4">
        {view.view === "health" ? (
          <DisplayHeader title="System health" subtitle={healthSummary} {...shared}>
            {null}
          </DisplayHeader>
        ) : view.view === "categories" ? (
          <DisplayHeader title="Categories" subtitle={`${categories?.length ?? 0} categories, busiest first; star one to pin it`} {...shared}>
            {null}
          </DisplayHeader>
        ) : view.view === "explorer" ? (
          <DisplayHeader title="Query explorer" subtitle="Any series, any range, step and window" {...shared}>
            <ZoneSelect timeZone={timeZone} zones={zones()} onTimeZone={setTimeZone} />
          </DisplayHeader>
        ) : (
          <DisplayHeader
            title={active ?? "Vortex Metrics"}
            subtitle={
              active
                ? `${shown.length} ${shown.length === 1 ? "dimension" : "dimensions"}, ${liveCount} live, refreshed every ${REFRESH_MS / 1000} s`
                : "Waiting for the first sample"
            }
            {...shared}
            onSend={active ? () => setSending(true) : null}
          >
            <DisplayControls
              range={range.range}
              onRange={(r) => update({ range: r })}
              smooth={view.smooth}
              onSmooth={(smooth) => update({ smooth })}
              step={step}
              timeZone={timeZone}
              zones={zones()}
              onTimeZone={setTimeZone}
            />
          </DisplayHeader>
        )}

        <main className="flex flex-col gap-4">
          {view.view === "health" ? (
            <HealthView live={live} now={now} onSummary={setHealthSummary} />
          ) : view.view === "categories" ? (
            <CategoriesView categories={categories ?? []} isPinned={isPinned} onPin={togglePin} onOpen={selectCategory} now={now} />
          ) : view.view === "explorer" ? (
            <ExplorerView catalog={catalog ?? []} timeZone={timeZone} />
          ) : categories === null ? (
            <p className="tile rounded-[22px] p-8 text-muted">Loading…</p>
          ) : !active ? (
            <section className="tile rounded-[22px] p-8 sm:p-10">
              <h2 className="text-2xl font-bold tracking-tight">No data yet</h2>
              <p className="mt-2 max-w-xl text-ink-2">
                Every category you push to appears on the left with its own display. Push a first sample:
              </p>
              <pre className="mt-5 overflow-x-auto rounded-xl bg-night p-4 text-sm text-night-ink">
                curl -X POST &apos;{typeof location === "undefined" ? "" : location.origin}/tsd/push?t=long&amp;c=car&amp;d=speed&amp;v=44&apos;
              </pre>
              {clusterError && <p className="mt-4 text-sm text-critical">{clusterError}</p>}
            </section>
          ) : (
            <>
              {queryError && (
                <p role="alert" className="tile rounded-xl border-critical px-4 py-3 text-sm text-critical">
                  {queryError}
                </p>
              )}

              {snapshots === null && !queryError ? (
                <p className="tile rounded-[22px] p-8 text-muted">Loading {active}…</p>
              ) : shown.length === 0 && snapshots !== null ? (
                <p className="tile rounded-[22px] p-8 text-muted">{active} has no series within the retention period.</p>
              ) : (
                <>
                  <KpiCards snapshots={shown} selected={selected} onSelect={selectDimension} rangeLabel={range.label} now={now} />
                  <div className="flex items-center justify-between px-1">
                    <h2 className="text-sm text-muted">Last {range.label}</h2>
                    <button
                      type="button"
                      onClick={() => setCollapsed(allCollapsed ? new Set() : new Set(shown.map(seriesId)))}
                      className="rounded-lg px-2 py-1 text-sm font-medium text-primary"
                    >
                      {allCollapsed ? "Expand all" : "Collapse all"}
                    </button>
                  </div>
                  <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,440px),1fr))] items-start gap-4">
                    {shown.map((s) => {
                      const id = seriesId(s);
                      return (
                        <DimensionPanel
                          key={id}
                          snapshot={s}
                          timeZone={timeZone}
                          rangeLabel={range.label}
                          expanded={!collapsed.has(id)}
                          highlighted={id === selected}
                          onToggle={() => toggle(id)}
                          onZoom={() => setZoomed(id)}
                          now={now}
                        />
                      );
                    })}
                  </div>
                </>
              )}
            </>
          )}
        </main>

        <footer className="pb-2 text-center text-xs text-muted">Copyright © 2017-2026 Fred Feng. All rights reserved.</footer>
      </div>

      {zoomedSnapshot && (
        <ZoomDialog title={`${zoomedSnapshot.category} / ${zoomedSnapshot.dimension}`} onClose={closeZoom}>
          <DimensionPanel
            snapshot={zoomedSnapshot}
            timeZone={timeZone}
            rangeLabel={range.label}
            expanded
            highlighted={false}
            onToggle={() => {}}
            now={now}
            large
          />
        </ZoomDialog>
      )}

      {palette && <CommandPalette categories={categories ?? []} onPick={jump} onClose={() => setPalette(false)} />}

      {sending && active && (
        <SendSampleDialog
          category={active}
          known={shown}
          initial={shown.find((s) => seriesId(s) === selected) ?? null}
          onClose={() => setSending(false)}
          onSent={() => {
            refreshCategory();
            refreshCatalog();
          }}
        />
      )}
    </div>
  );
}
