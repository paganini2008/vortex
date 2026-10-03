"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { CloseIcon } from "./Icons";

/**
 * Shows one card enlarged over the page. Escape, the close button or a click on the backdrop
 * closes it; focus moves into it on opening and back where it was on closing.
 */
export function ZoomDialog({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  const panel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const before = document.activeElement as HTMLElement | null;
    panel.current?.focus();
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = overflow;
      before?.focus?.();
    };
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto bg-black/60 p-4 backdrop-blur-sm sm:p-8"
      onMouseDown={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        className="relative mx-auto max-w-[1400px] outline-none"
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute -top-2 right-0 z-10 grid size-10 -translate-y-full place-items-center rounded-full bg-panel text-ink-2 hover:text-ink sm:right-0"
        >
          <CloseIcon />
          <span className="sr-only">Close</span>
        </button>
        <div className="mt-10">{children}</div>
      </div>
    </div>
  );
}
