"use client";

import { useSyncExternalStore, type ReactNode } from "react";
import { RANGES, SMOOTHING_STEPS } from "@/lib/format";
import type { Theme } from "@/lib/theme";
import { BookIcon, MoonIcon, PlusIcon, SunIcon } from "./Icons";

interface Props {
  title: string;
  subtitle: string;
  /** The view's own controls, centred in the bar */
  children?: ReactNode;
  timeZone: string;
  live: boolean;
  onLive: (live: boolean) => void;
  theme: Theme;
  onTheme: (theme: Theme) => void;
  onSend: (() => void) | null;
  now: number;
}

const noSubscription = () => () => {};

/** False while the page is pre-rendered or hydrated, true once it runs in the browser. */
function useInBrowser() {
  return useSyncExternalStore(noSubscription, () => true, () => false);
}

function Clock({ now, timeZone }: { now: number; timeZone: string }) {
  // The page is pre-rendered at build time: a time rendered there would be the build's, and
  // hydration keeps it. So the clock starts blank and fills in once in the browser
  const inBrowser = useInBrowser();
  const time = inBrowser
    ? new Intl.DateTimeFormat("en-GB", { hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23", timeZone }).format(now)
    : "--:--:--";
  const date = inBrowser ? new Intl.DateTimeFormat("en-GB", { weekday: "short", day: "numeric", month: "short", year: "numeric", timeZone }).format(now) : "\u00a0";
  return (
    <div className="text-right">
      <div className="readout text-3xl font-bold leading-none" aria-label="Current time">
        {time}
      </div>
      <div className="mt-1 text-xs text-muted">{date}</div>
    </div>
  );
}

const iconButton = "grid size-10 place-items-center rounded-xl border border-edge text-ink-2 hover:text-ink";

/** The title bar every view shares: what is on screen, its controls, and the time now. */
export function DisplayHeader(p: Props) {
  return (
    <header className="tile flex flex-wrap items-center gap-x-6 gap-y-4 rounded-[22px] px-5 py-4">
      <div className="min-w-0">
        <h1 className="truncate text-2xl font-bold tracking-tight sm:text-3xl">{p.title}</h1>
        <p className="mt-0.5 text-sm text-muted">{p.subtitle}</p>
      </div>

      <div className="flex flex-1 flex-wrap items-center justify-center gap-3 text-sm">{p.children}</div>

      <div className="ml-auto flex items-center gap-4">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => p.onLive(!p.live)}
            aria-pressed={p.live}
            className="flex h-10 items-center gap-2 rounded-xl border border-edge px-3 text-sm font-medium"
          >
            <span aria-hidden className={`size-2 rounded-full ${p.live ? "pulse bg-good" : "bg-muted"}`} />
            {p.live ? "Live" : "Paused"}
          </button>
          {p.onSend && (
            <button type="button" onClick={p.onSend} className={iconButton} title="Send a sample">
              <PlusIcon />
              <span className="sr-only">Send a sample</span>
            </button>
          )}
          <a href="/swagger-ui.html" target="_blank" rel="noreferrer" className={iconButton} title="API docs">
            <BookIcon />
            <span className="sr-only">API docs</span>
          </a>
          <button
            type="button"
            onClick={() => p.onTheme(p.theme === "dark" ? "light" : "dark")}
            className={iconButton}
            title={p.theme === "dark" ? "Light theme" : "Dark theme"}
          >
            {p.theme === "dark" ? <SunIcon /> : <MoonIcon />}
            <span className="sr-only">{p.theme === "dark" ? "Switch to the light theme" : "Switch to the dark theme"}</span>
          </button>
        </div>
        <Clock now={p.now} timeZone={p.timeZone} />
      </div>
    </header>
  );
}

interface ControlsProps {
  range: string;
  onRange: (range: string) => void;
  smooth: boolean;
  onSmooth: (smooth: boolean) => void;
  step: number;
  timeZone: string;
  zones: string[];
  onTimeZone: (zone: string) => void;
}

export function ZoneSelect({ timeZone, zones, onTimeZone }: Pick<ControlsProps, "timeZone" | "zones" | "onTimeZone">) {
  return (
    <label className="flex items-center gap-2 text-ink-2">
      <span className="sr-only">Time zone</span>
      <select
        value={timeZone}
        onChange={(e) => onTimeZone(e.target.value)}
        className="h-10 max-w-44 rounded-xl border border-edge bg-panel-2 px-2 text-ink"
      >
        {zones.map((z) => (
          <option key={z}>{z}</option>
        ))}
      </select>
    </label>
  );
}

/** The wall display's controls: range, smoothing and zone. */
export function DisplayControls(p: ControlsProps) {
  return (
    <>
      <div role="group" aria-label="Range" className="flex rounded-xl bg-panel-2 p-1">
        {RANGES.map((r) => (
          <button
            key={r.range}
            type="button"
            aria-pressed={r.range === p.range}
            onClick={() => p.onRange(r.range)}
            className={`rounded-lg px-3 py-1.5 font-medium ${r.range === p.range ? "bg-primary text-primary-ink" : "text-ink-2 hover:text-ink"}`}
          >
            {r.range}
          </button>
        ))}
      </div>
      <label className="flex items-center gap-2 rounded-xl bg-panel-2 px-3 py-2 text-ink-2">
        <input type="checkbox" checked={p.smooth} onChange={(e) => p.onSmooth(e.target.checked)} className="accent-[var(--primary)]" />
        Smooth ({p.step * SMOOTHING_STEPS}-min moving window)
      </label>
      <ZoneSelect timeZone={p.timeZone} zones={p.zones} onTimeZone={p.onTimeZone} />
    </>
  );
}
