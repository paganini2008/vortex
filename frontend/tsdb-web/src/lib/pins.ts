"use client";

import { useCallback, useEffect, useState } from "react";

const KEY = "vortex-pinned-categories";

function read(): string[] {
  try {
    const v = JSON.parse(localStorage.getItem(KEY) ?? "[]");
    return Array.isArray(v) ? v.filter((x) => typeof x === "string") : [];
  } catch {
    return [];
  }
}

/**
 * The categories this viewer has pinned, kept in the browser: a screen's own choice, nothing
 * to share. Pinned categories are listed first in the sidebar.
 */
export function usePins() {
  const [pins, setPins] = useState<string[]>([]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- browser storage, read after hydration
    setPins(read());
  }, []);

  const toggle = useCallback((category: string) => {
    setPins((prev) => {
      const next = prev.includes(category) ? prev.filter((c) => c !== category) : [...prev, category];
      try {
        localStorage.setItem(KEY, JSON.stringify(next));
      } catch {
        // Storage refused (a private window, say); the pin lasts for this visit
      }
      return next;
    });
  }, []);

  return { pins, toggle, isPinned: (c: string) => pins.includes(c) };
}
