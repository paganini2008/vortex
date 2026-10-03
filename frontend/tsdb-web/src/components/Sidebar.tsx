"use client";

import type { CategoryInfo, ClusterView } from "@/lib/api";
import { LIVE_WITHIN_MS, formatAge, formatRate } from "@/lib/format";
import { BookIcon, ChartIcon, GridIcon, NodesIcon, SearchIcon, StarIcon } from "./Icons";
import { ClusterCard } from "./ClusterCard";

export type View = "display" | "health" | "explorer" | "categories";

/** How many of the busiest categories the sidebar lists; the rest are a click away. */
export const BUSIEST_LIMIT = 10;

interface Props {
  view: View;
  onView: (view: View) => void;
  /** Busiest first; null while the first list is loading */
  categories: CategoryInfo[] | null;
  pins: string[];
  active: string | null;
  onSelect: (category: string) => void;
  onSearch: () => void;
  cluster: ClusterView | null;
  clusterError: string | null;
  now: number;
}

const item = "relative flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-[15px] transition-colors";
const itemOn = "bg-primary-soft font-semibold text-ink";
const itemOff = "text-ink-2 hover:bg-panel-2";

function CategoryItem({ c, selected, pinned, now, onSelect }: { c: CategoryInfo; selected: boolean; pinned: boolean; now: number; onSelect: (c: string) => void }) {
  const live = now - c.lastSeen < LIVE_WITHIN_MS;
  return (
    <li>
      <button
        type="button"
        onClick={() => onSelect(c.category)}
        aria-current={selected ? "page" : undefined}
        title={`${c.series} series, last sample ${formatAge(c.lastSeen, now)}`}
        className={`${item} ${selected ? itemOn : itemOff}`}
      >
        {selected && <span aria-hidden className="absolute -left-4 top-2 bottom-2 w-1 rounded-r-full bg-primary" />}
        {pinned ? <StarIcon filled className="text-primary" /> : <ChartIcon className={selected ? "text-primary" : "text-muted"} />}
        <span className="min-w-0 flex-1 truncate">{c.category}</span>
        <span className="flex items-center gap-1.5 text-xs text-muted">
          {live && <span aria-label="receiving samples" className="size-1.5 rounded-full bg-good" />}
          {formatRate(c.samplesPerMinute)}
        </span>
      </button>
    </li>
  );
}

export function Sidebar({ view, onView, categories: loaded, pins, active, onSelect, onSearch, cluster, clusterError, now }: Props) {
  const categories = loaded ?? [];
  const pinned = pins.map((p) => categories.find((c) => c.category === p)).filter((c): c is CategoryInfo => !!c);
  const rest = categories.filter((c) => !pins.includes(c.category));
  const busiest = rest.slice(0, BUSIEST_LIMIT);
  // The open category stays visible even when it is neither pinned nor among the busiest
  const extra = view === "display" ? rest.slice(BUSIEST_LIMIT).find((c) => c.category === active) : undefined;
  const general: [View, string, typeof NodesIcon][] = [
    ["health", "System health", NodesIcon],
    ["explorer", "Query explorer", SearchIcon],
  ];
  const isOpen = (c: string) => view === "display" && c === active;

  return (
    <aside className="tile flex flex-col gap-5 rounded-[22px] p-4 md:sticky md:top-4 md:h-[calc(100vh-2rem)] md:w-60 md:shrink-0">
      <div className="flex items-center gap-2.5 px-2 pt-1">
        <span aria-hidden className="grid size-9 place-items-center rounded-xl bg-primary text-primary-ink">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
            <path d="M4 15l4-6 4 4 4-7 4 5" />
          </svg>
        </span>
        <span className="text-xl font-bold tracking-tight">vortex</span>
      </div>

      <button
        type="button"
        onClick={onSearch}
        className="flex h-10 items-center gap-2 rounded-xl bg-panel-2 px-3 text-sm text-muted hover:text-ink"
      >
        <SearchIcon width={16} height={16} />
        <span className="flex-1 text-left">Jump to…</span>
        <kbd className="rounded border border-edge px-1.5 text-[11px]">⌘K</kbd>
      </button>

      <div className="min-h-0 flex-1 overflow-y-auto">
        <nav aria-label="Categories">
          {pinned.length > 0 && (
            <>
              <h2 className="px-2 text-sm text-muted">Pinned</h2>
              <ul className="mb-4 mt-2 space-y-1">
                {pinned.map((c) => (
                  <CategoryItem key={c.category} c={c} selected={isOpen(c.category)} pinned now={now} onSelect={onSelect} />
                ))}
              </ul>
            </>
          )}
          <h2 className="px-2 text-sm text-muted">Busiest</h2>
          {loaded === null ? (
            <p className="mt-3 px-2 text-sm text-muted">Loading…</p>
          ) : categories.length === 0 ? (
            <p className="mt-3 px-2 text-sm text-muted">No data yet.</p>
          ) : (
            <ul className="mt-2 space-y-1">
              {busiest.map((c) => (
                <CategoryItem key={c.category} c={c} selected={isOpen(c.category)} pinned={false} now={now} onSelect={onSelect} />
              ))}
              {extra && <CategoryItem c={extra} selected pinned={false} now={now} onSelect={onSelect} />}
            </ul>
          )}
          {categories.length > 0 && (
            <button
              type="button"
              onClick={() => onView("categories")}
              aria-current={view === "categories" ? "page" : undefined}
              className={`${item} mt-1 ${view === "categories" ? itemOn : "text-primary hover:bg-panel-2"}`}
            >
              <GridIcon className={view === "categories" ? "text-primary" : ""} />
              {rest.length > BUSIEST_LIMIT ? `Browse all (${categories.length})…` : `All categories (${categories.length})`}
            </button>
          )}
        </nav>

        <nav aria-label="General" className="mt-5">
          <h2 className="px-2 text-sm text-muted">General</h2>
          <ul className="mt-2 space-y-1">
            {general.map(([v, label, Icon]) => (
              <li key={v}>
                <button type="button" onClick={() => onView(v)} aria-current={view === v ? "page" : undefined} className={`${item} ${view === v ? itemOn : itemOff}`}>
                  {view === v && <span aria-hidden className="absolute -left-4 top-2 bottom-2 w-1 rounded-r-full bg-primary" />}
                  <Icon className={view === v ? "text-primary" : "text-muted"} />
                  {label}
                </button>
              </li>
            ))}
            <li>
              <a href="/swagger-ui.html" target="_blank" rel="noreferrer" className={`${item} ${itemOff}`}>
                <BookIcon className="text-muted" />
                API docs
              </a>
            </li>
          </ul>
        </nav>
      </div>

      <ClusterCard cluster={cluster} error={clusterError} now={now} />
    </aside>
  );
}
