"use client";

/**
 * A half-circle gauge: where the instant value sits between the range's lowest and highest.
 * The arc is the track; the filled part runs from the lowest to the value.
 */
export function RangeGauge({ position, caption }: { position: number | null; caption: string }) {
  const r = 70;
  const length = Math.PI * r;
  const filled = position === null ? 0 : position * length;
  const percent = position === null ? null : Math.round(position * 100);
  return (
    <figure className="flex min-w-0 flex-col items-center">
      <svg
        viewBox="0 0 180 104"
        className="w-full max-w-[180px]"
        role="img"
        aria-label={percent === null ? "No value to place" : `${percent}% of the way from lowest to highest`}
      >
        <path d="M20 90 A70 70 0 0 1 160 90" fill="none" stroke="var(--track)" strokeWidth="18" strokeLinecap="round" />
        {position !== null && position > 0 && (
          <path
            d="M20 90 A70 70 0 0 1 160 90"
            fill="none"
            stroke="var(--primary)"
            strokeWidth="18"
            strokeLinecap="round"
            strokeDasharray={`${filled} ${length}`}
          />
        )}
        <text x="90" y="82" textAnchor="middle" fontSize="28" fontWeight="700" fill="var(--ink)">
          {percent === null ? "–" : `${percent}%`}
        </text>
      </svg>
      <figcaption className="-mt-1 text-center text-xs text-muted">{caption}</figcaption>
    </figure>
  );
}
