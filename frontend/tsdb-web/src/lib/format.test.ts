import { describe, expect, it } from "vitest";
import {
  formatAge,
  formatAverage,
  formatCompact,
  formatTime,
  formatUptime,
  formatValue,
  formatRate,
  positionInRange,
  rangeOf,
} from "./format";

describe("format", () => {
  it("formats values by data type", () => {
    expect(formatValue(1234.6, "long")).toBe("1,235");
    expect(formatValue(0.123456, "double")).toBe("0.1235");
    expect(formatValue(null, "long")).toBe("–");
    expect(formatValue(undefined, "decimal")).toBe("–");
    expect(formatValue(Number.NaN, "double")).toBe("–");
  });

  it("keeps integer averages short", () => {
    expect(formatAverage(2714.1406, "long")).toBe("2,714.14");
    expect(formatAverage(0.78431, "double")).toBe("0.7843");
    expect(formatAverage(null, "long")).toBe("–");
  });

  it("compacts only large readouts", () => {
    expect(formatCompact(99_999, "long")).toBe("99,999");
    expect(formatCompact(12_345_678, "long")).toBe("12.35M");
    expect(formatCompact(null, "long")).toBe("–");
  });

  it("formats times in the requested zone", () => {
    const ts = Date.UTC(2026, 9, 3, 9, 5);
    expect(formatTime(ts, "UTC")).toBe("09:05");
    expect(formatTime(ts, "Australia/Sydney")).toBe("19:05");
  });

  it("describes ages and uptimes", () => {
    const now = 1_000_000_000;
    expect(formatAge(now - 2_000, now)).toBe("just now");
    expect(formatAge(now - 30_000, now)).toBe("30s ago");
    expect(formatAge(now - 5 * 60_000, now)).toBe("5m ago");
    expect(formatAge(now - 3 * 3_600_000, now)).toBe("3h ago");
    expect(formatAge(now - 2 * 86_400_000, now)).toBe("2d ago");
    expect(formatUptime(now - 45_000, now)).toBe("45s");
    expect(formatUptime(now - 7 * 60_000, now)).toBe("7m");
    expect(formatUptime(now - (2 * 3600 + 5 * 60) * 1000, now)).toBe("2h 5m");
    expect(formatUptime(now - 3 * 86_400_000, now)).toBe("3d");
  });

  it("formats rates for the sidebar", () => {
    expect(formatRate(0)).toBe("0/min");
    expect(formatRate(0.4)).toBe("<1/min");
    expect(formatRate(1234.4)).toBe("1,234/min");
    expect(formatRate(56_000)).toBe("56k/min");
  });

  it("places a value within its range", () => {
    expect(positionInRange(5, 0, 10)).toBe(0.5);
    expect(positionInRange(15, 0, 10)).toBe(1);
    expect(positionInRange(-1, 0, 10)).toBe(0);
    expect(positionInRange(3, 3, 3)).toBe(0.5);
    expect(positionInRange(null, 0, 10)).toBeNull();
    expect(positionInRange(1, null, 10)).toBeNull();
  });

  it("falls back to the first range", () => {
    expect(rangeOf("6h").step).toBe(5);
    expect(rangeOf("nonsense").range).toBe("1h");
    expect(rangeOf(null).range).toBe("1h");
  });
});
