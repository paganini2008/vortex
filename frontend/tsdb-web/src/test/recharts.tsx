import { cloneElement, isValidElement, type ReactElement, type ReactNode } from "react";

// ResponsiveContainer measures its parent, which is 0x0 under jsdom, and then draws nothing.
// Tests give the chart a fixed size instead so the SVG is really rendered.
export function FixedSizeContainer({ children }: { children: ReactNode }) {
  return isValidElement(children)
    ? cloneElement(children as ReactElement<{ width?: number; height?: number }>, { width: 600, height: 200 })
    : null;
}
