import { describe, expect, it } from "vitest";
import { renderHook } from "@testing-library/react";
import { FALLBACK_COLORS, readChartColors, useChartColors } from "./useChartColors";

describe("chart colors", () => {
  it("reads the CSS tokens and falls back where one is missing", () => {
    document.documentElement.style.setProperty("--primary", "#123456");
    const c = readChartColors();
    expect(c.series).toBe("#123456");
    expect(c.track).toBe(FALLBACK_COLORS.track);
    // The same object until something changes, as useSyncExternalStore requires
    expect(readChartColors()).toBe(c);
    document.documentElement.style.setProperty("--primary", "#654321");
    expect(readChartColors()).not.toBe(c);
    document.documentElement.style.removeProperty("--primary");
  });

  it("is available as a hook", () => {
    const { result, unmount } = renderHook(() => useChartColors());
    expect(result.current.series).toBeTruthy();
    unmount();
  });
});
