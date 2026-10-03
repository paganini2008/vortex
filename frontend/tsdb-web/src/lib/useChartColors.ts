"use client";

import { useSyncExternalStore } from "react";

export interface ChartColors {
  series: string;
  band: string;
  track: string;
  grid: string;
  axis: string;
  panel: string;
}

const TOKENS: Record<keyof ChartColors, string> = {
  series: "--primary",
  band: "--series-band",
  track: "--track",
  grid: "--grid",
  axis: "--muted",
  panel: "--panel",
};

export const FALLBACK_COLORS: ChartColors = {
  series: "#8b81f4",
  band: "rgba(139,129,244,0.22)",
  track: "#232340",
  grid: "#1d1f31",
  axis: "#7f8199",
  panel: "#11121d",
};

let cached: ChartColors = FALLBACK_COLORS;
let cachedKey = "";

export function readChartColors(): ChartColors {
  const style = getComputedStyle(document.documentElement);
  const next = Object.fromEntries(
    (Object.keys(TOKENS) as (keyof ChartColors)[]).map((k) => [
      k,
      style.getPropertyValue(TOKENS[k]).trim() || FALLBACK_COLORS[k],
    ]),
  ) as unknown as ChartColors;
  const key = JSON.stringify(next);
  // useSyncExternalStore needs the same object back until something changes
  if (key !== cachedKey) {
    cached = next;
    cachedKey = key;
  }
  return cached;
}

/** The theme is the data-theme attribute on <html>, so watch that. */
function subscribe(onChange: () => void) {
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
  return () => observer.disconnect();
}

/**
 * Chart colors resolved from the CSS tokens. Recharts writes colors into SVG presentation
 * attributes, where var() is not reliably resolved, so they are read out here instead and
 * read again when the theme changes.
 */
export function useChartColors(): ChartColors {
  return useSyncExternalStore(subscribe, readChartColors, () => FALLBACK_COLORS);
}
