"use client";

import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from "react";
import type { CategoryInfo } from "@/lib/api";
import { formatRate } from "@/lib/format";
import type { View } from "./Sidebar";

export type Target = { kind: "category"; category: string } | { kind: "view"; view: View };

interface Entry {
  label: string;
  hint: string;
  target: Target;
}

const VIEWS: Entry[] = [
  { label: "System health", hint: "View", target: { kind: "view", view: "health" } },
  { label: "Query explorer", hint: "View", target: { kind: "view", view: "explorer" } },
  { label: "All categories", hint: "View", target: { kind: "view", view: "categories" } },
];

/** How many matches the palette lists; typing narrows them. */
const LIMIT = 12;

/**
 * Jump to any category or view from the keyboard (⌘K or Ctrl+K): type to narrow, arrows to
 * move, Enter to open, Escape to close.
 */
export function CommandPalette({ categories, onPick, onClose }: { categories: CategoryInfo[]; onPick: (t: Target) => void; onClose: () => void }) {
  const [q, setQ] = useState("");
  const [index, setIndex] = useState(0);
  const input = useRef<HTMLInputElement>(null);

  useEffect(() => {
    input.current?.focus();
  }, []);

  const entries = useMemo(() => {
    const needle = q.trim().toLowerCase();
    const cats: Entry[] = categories.map((c) => ({
      label: c.category,
      hint: `${c.series} series, ${formatRate(c.samplesPerMinute)}`,
      target: { kind: "category", category: c.category },
    }));
    const all = [...cats, ...VIEWS];
    if (!needle) return all.slice(0, LIMIT);
    // Names starting with what was typed come before names merely containing it
    return all
      .filter((e) => e.label.toLowerCase().includes(needle))
      .sort((a, b) => Number(!a.label.toLowerCase().startsWith(needle)) - Number(!b.label.toLowerCase().startsWith(needle)))
      .slice(0, LIMIT);
  }, [categories, q]);

  const current = Math.min(index, Math.max(0, entries.length - 1));

  function onKey(e: KeyboardEvent) {
    if (e.key === "Escape") onClose();
    else if (e.key === "ArrowDown") {
      e.preventDefault();
      setIndex((current + 1) % Math.max(1, entries.length));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setIndex((current - 1 + entries.length) % Math.max(1, entries.length));
    } else if (e.key === "Enter" && entries[current]) {
      onPick(entries[current].target);
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/50 p-4 pt-[12vh] backdrop-blur-sm" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div role="dialog" aria-modal="true" aria-label="Jump to" className="tile mx-auto max-w-xl overflow-hidden rounded-[22px] shadow-2xl">
        <input
          ref={input}
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setIndex(0);
          }}
          onKeyDown={onKey}
          placeholder="Jump to a category or view"
          role="combobox"
          aria-expanded="true"
          aria-controls="palette-list"
          aria-activedescendant={entries[current] ? `palette-${current}` : undefined}
          className="h-14 w-full border-b border-rule bg-transparent px-5 text-base text-ink outline-none placeholder:text-muted"
        />
        <ul id="palette-list" role="listbox" className="max-h-[50vh] overflow-y-auto p-2">
          {entries.length === 0 && <li className="px-3 py-2 text-sm text-muted">Nothing matches “{q}”.</li>}
          {entries.map((e, i) => (
            <li
              key={`${e.target.kind}-${e.label}`}
              id={`palette-${i}`}
              role="option"
              aria-selected={i === current}
              onMouseEnter={() => setIndex(i)}
              onMouseDown={(ev) => {
                ev.preventDefault();
                onPick(e.target);
              }}
              className={`flex cursor-pointer items-center justify-between gap-3 rounded-xl px-3 py-2.5 text-sm ${i === current ? "bg-primary-soft text-ink" : "text-ink-2"}`}
            >
              <span className="truncate font-medium">{e.label}</span>
              <span className="shrink-0 text-xs text-muted">{e.hint}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
