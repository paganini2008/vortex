import { afterEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NOW } from "@/test/fixtures";
import { usePins } from "@/lib/pins";
import { CategoriesView } from "./CategoriesView";
import { CommandPalette } from "./CommandPalette";

const categories = [
  { category: "car", series: 3, lastSeen: NOW - 1_000, samplesPerMinute: 50 },
  { category: "server", series: 5, lastSeen: NOW - 600_000, samplesPerMinute: 900 },
  { category: "sensor", series: 1, lastSeen: NOW - 30_000, samplesPerMinute: 0 },
];

afterEach(() => localStorage.clear());

describe("CategoriesView", () => {
  const names = () => within(screen.getByRole("table")).getAllByRole("button", { name: /^(car|server|sensor)$/ }).map((b) => b.textContent);

  it("sorts, filters, pins and opens", async () => {
    const onPin = vi.fn();
    const onOpen = vi.fn();
    render(<CategoriesView categories={categories} isPinned={(c) => c === "car"} onPin={onPin} onOpen={onOpen} now={NOW} />);
    expect(names()).toEqual(["server", "car", "sensor"]);
    await userEvent.click(screen.getByRole("button", { name: /Samples \/ min/ }));
    expect(names()).toEqual(["sensor", "car", "server"]);
    await userEvent.click(screen.getByRole("button", { name: /^Category/ }));
    expect(names()).toEqual(["car", "sensor", "server"]);
    expect(screen.getByRole("columnheader", { name: /Category/ })).toHaveAttribute("aria-sort", "ascending");
    await userEvent.click(screen.getByRole("button", { name: /Dimensions/ }));
    expect(names()).toEqual(["server", "car", "sensor"]);
    await userEvent.click(screen.getByRole("button", { name: /Last sample/ }));
    expect(names()).toEqual(["car", "sensor", "server"]);

    expect(screen.getByRole("button", { name: "Unpin car" })).toHaveAttribute("aria-pressed", "true");
    await userEvent.click(screen.getByRole("button", { name: "Pin server" }));
    expect(onPin).toHaveBeenCalledWith("server");
    await userEvent.click(screen.getByRole("button", { name: "server" }));
    expect(onOpen).toHaveBeenCalledWith("server");

    await userEvent.type(screen.getByPlaceholderText("Filter categories"), "se");
    expect(screen.getByText("2 of 3")).toBeInTheDocument();
    await userEvent.type(screen.getByPlaceholderText("Filter categories"), "x");
    expect(screen.getByText("No category matches “sex”.")).toBeInTheDocument();
  });
});

describe("CommandPalette", () => {
  it("narrows, moves with the arrows and opens with Enter", async () => {
    const onPick = vi.fn();
    render(<CommandPalette categories={categories} onPick={onPick} onClose={() => {}} />);
    const input = screen.getByRole("combobox");
    expect(input).toHaveFocus();
    expect(screen.getAllByRole("option")).toHaveLength(6);
    await userEvent.type(input, "se");
    // Names starting with "se" come before names merely containing it
    expect(screen.getAllByRole("option").map((o) => o.textContent?.split(/\d/)[0])).toEqual(["server", "sensor"]);
    // Up wraps to the last; down wraps back to the first
    await userEvent.keyboard("{ArrowUp}{ArrowUp}{ArrowDown}{Enter}");
    expect(onPick).toHaveBeenCalledWith({ kind: "category", category: "sensor" });
    await userEvent.clear(input);
    await userEvent.type(input, "health");
    fireEvent.mouseDown(screen.getAllByRole("option")[0]);
    expect(onPick).toHaveBeenLastCalledWith({ kind: "view", view: "health" });
  });

  it("says when nothing matches and closes", async () => {
    const onClose = vi.fn();
    const { container } = render(<CommandPalette categories={categories} onPick={() => {}} onClose={onClose} />);
    await userEvent.type(screen.getByRole("combobox"), "zzz");
    expect(screen.getByText("Nothing matches “zzz”.")).toBeInTheDocument();
    await userEvent.keyboard("{Enter}{Escape}");
    fireEvent.mouseDown(container.firstElementChild!);
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});

describe("usePins", () => {
  it("remembers pins in the browser", () => {
    localStorage.setItem("vortex-pinned-categories", JSON.stringify(["car", 7]));
    const { result } = renderHook(() => usePins());
    expect(result.current.pins).toEqual(["car"]);
    act(() => result.current.toggle("server"));
    act(() => result.current.toggle("car"));
    expect(result.current.isPinned("server")).toBe(true);
    expect(JSON.parse(localStorage.getItem("vortex-pinned-categories")!)).toEqual(["server"]);
  });

  it("copes with unreadable or refused storage", () => {
    localStorage.setItem("vortex-pinned-categories", "{oops");
    expect(renderHook(() => usePins()).result.current.pins).toEqual([]);
    localStorage.setItem("vortex-pinned-categories", JSON.stringify({ not: "a list" }));
    const { result } = renderHook(() => usePins());
    expect(result.current.pins).toEqual([]);
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("denied");
    });
    act(() => result.current.toggle("car"));
    expect(result.current.pins).toEqual(["car"]);
  });
});
