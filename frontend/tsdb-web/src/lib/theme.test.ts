import { afterEach, describe, expect, it, vi } from "vitest";
import { applyTheme, storedTheme } from "./theme";

afterEach(() => {
  localStorage.clear();
  delete document.documentElement.dataset.theme;
});

describe("theme", () => {
  it("defaults to dark and remembers the choice", () => {
    expect(storedTheme()).toBe("dark");
    applyTheme("light");
    expect(document.documentElement.dataset.theme).toBe("light");
    expect(storedTheme()).toBe("light");
    applyTheme("dark");
    expect(document.documentElement.dataset.theme).toBeUndefined();
    expect(storedTheme()).toBe("dark");
  });

  it("still works where storage is refused", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("denied");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("denied");
    });
    expect(storedTheme()).toBe("dark");
    applyTheme("light");
    expect(document.documentElement.dataset.theme).toBe("light");
  });
});
