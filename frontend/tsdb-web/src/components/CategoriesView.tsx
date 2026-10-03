"use client";

import { useMemo, useState } from "react";
import type { CategoryInfo } from "@/lib/api";
import { LIVE_WITHIN_MS, formatAge } from "@/lib/format";
import { StarIcon } from "./Icons";

type SortKey = "category" | "series" | "samplesPerMinute" | "lastSeen";

const COLUMNS: [SortKey, string][] = [
  ["category", "Category"],
  ["series", "Dimensions"],
  ["samplesPerMinute", "Samples / min"],
  ["lastSeen", "Last sample"],
];

interface Props {
  categories: CategoryInfo[];
  isPinned: (category: string) => boolean;
  onPin: (category: string) => void;
  onOpen: (category: string) => void;
  now: number;
}

/** Every category in one table: search it, sort it, pin some, open one. */
export function CategoriesView({ categories, isPinned, onPin, onOpen, now }: Props) {
  const [filter, setFilter] = useState("");
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({ key: "samplesPerMinute", desc: true });

  const rows = useMemo(() => {
    const q = filter.trim().toLowerCase();
    const listed = categories.filter((c) => c.category.toLowerCase().includes(q));
    const dir = sort.desc ? -1 : 1;
    return [...listed].sort((a, b) => {
      const x = a[sort.key];
      const y = b[sort.key];
      const cmp = typeof x === "string" ? x.localeCompare(y as string) : (x as number) - (y as number);
      // Ties as the server orders them: most recently active, then by name
      return cmp * dir || b.lastSeen - a.lastSeen || a.category.localeCompare(b.category);
    });
  }, [categories, filter, sort]);

  function sortBy(key: SortKey) {
    // Names read best A to Z; numbers, largest first
    setSort((s) => (s.key === key ? { key, desc: !s.desc } : { key, desc: key !== "category" }));
  }

  return (
    <section aria-label="All categories" className="tile overflow-hidden rounded-[22px]">
      <div className="flex flex-wrap items-center gap-3 border-b border-rule p-4">
        <label className="min-w-0 flex-1">
          <span className="sr-only">Filter categories</span>
          <input
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Filter categories"
            className="h-10 w-full max-w-sm rounded-xl border border-edge bg-panel-2 px-3 text-sm text-ink"
          />
        </label>
        <span className="text-sm text-muted">
          {rows.length} of {categories.length}
        </span>
      </div>
      <div className="max-h-[70vh] overflow-auto">
        <table className="w-full text-sm">
          <thead className="sticky top-0 bg-panel text-left text-muted">
            <tr className="border-b border-rule">
              <th scope="col" className="w-12 px-3 py-2 font-medium">
                <span className="sr-only">Pinned</span>
              </th>
              {COLUMNS.map(([key, label]) => (
                <th
                  key={key}
                  scope="col"
                  aria-sort={sort.key === key ? (sort.desc ? "descending" : "ascending") : "none"}
                  className={`px-3 py-2 font-medium ${key === "category" ? "" : "text-right"}`}
                >
                  <button type="button" onClick={() => sortBy(key)} className="inline-flex items-center gap-1 hover:text-ink">
                    {label}
                    <span aria-hidden>{sort.key === key ? (sort.desc ? "↓" : "↑") : ""}</span>
                  </button>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-rule">
            {rows.map((c) => {
              const pinned = isPinned(c.category);
              const live = now - c.lastSeen < LIVE_WITHIN_MS;
              return (
                <tr key={c.category} className="hover:bg-panel-2">
                  <td className="px-3 py-1.5">
                    <button
                      type="button"
                      onClick={() => onPin(c.category)}
                      aria-pressed={pinned}
                      className={`grid size-8 place-items-center rounded-lg ${pinned ? "text-primary" : "text-muted hover:text-ink"}`}
                    >
                      <StarIcon filled={pinned} width={16} height={16} />
                      <span className="sr-only">
                        {pinned ? "Unpin" : "Pin"} {c.category}
                      </span>
                    </button>
                  </td>
                  <td className="px-3 py-1.5">
                    <button type="button" onClick={() => onOpen(c.category)} className="flex items-center gap-2 font-semibold hover:text-primary">
                      <span aria-hidden className={`size-2 rounded-full ${live ? "bg-good" : "bg-muted"}`} />
                      {c.category}
                    </button>
                  </td>
                  <td className="px-3 py-1.5 text-right">{c.series.toLocaleString("en-US")}</td>
                  <td className="px-3 py-1.5 text-right">{c.samplesPerMinute.toLocaleString("en-US")}</td>
                  <td className="px-3 py-1.5 text-right text-ink-2">{formatAge(c.lastSeen, now)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {rows.length === 0 && <p className="p-6 text-sm text-muted">No category matches “{filter}”.</p>}
      </div>
    </section>
  );
}
