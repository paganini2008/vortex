"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiError, DATA_TYPES, push, pushRandom, type DataType, type Series } from "@/lib/api";
import { formatValue } from "@/lib/format";
import { CloseIcon } from "./Icons";

interface Props {
  category: string;
  /** The category's existing series, offered as suggestions */
  known: Series[];
  initial?: Series | null;
  onClose: () => void;
  onSent: () => void;
}

export function SendSampleDialog({ category, known, initial, onClose, onSent }: Props) {
  const [dimension, setDimension] = useState(initial?.dimension ?? "");
  const [dataType, setDataType] = useState<DataType>(initial?.dataType ?? "long");
  const [value, setValue] = useState("");
  const [status, setStatus] = useState<{ ok: boolean; text: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const first = useRef<HTMLInputElement>(null);

  useEffect(() => {
    first.current?.focus();
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  function chooseDimension(d: string) {
    setDimension(d);
    // An existing series keeps its type; mixing types under one name makes two series
    const match = known.find((k) => k.dimension === d);
    if (match) setDataType(match.dataType);
  }

  async function run(action: (s: Series) => Promise<Record<string, unknown>>) {
    const series = { dataType, category, dimension: dimension.trim() };
    if (!series.dimension) {
      setStatus({ ok: false, text: "Enter a dimension." });
      return;
    }
    setBusy(true);
    setStatus(null);
    try {
      const receipt = await action(series);
      setStatus({ ok: true, text: `Sent ${formatValue(Number(receipt.value), dataType)} to ${series.dimension}` });
      onSent();
    } catch (e) {
      setStatus({ ok: false, text: e instanceof ApiError ? e.message : "Sending failed." });
    } finally {
      setBusy(false);
    }
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!value.trim()) {
      setStatus({ ok: false, text: "Enter a value." });
      return;
    }
    run((s) => push(s, value.trim()));
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div role="dialog" aria-modal="true" aria-labelledby="send-title" className="w-full max-w-md rounded-[22px] bg-panel p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 id="send-title" className="text-lg font-semibold">
              Send a sample
            </h2>
            <p className="text-sm text-muted">to {category}</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-full p-1.5 text-muted hover:bg-panel-2">
            <CloseIcon />
            <span className="sr-only">Close</span>
          </button>
        </div>

        <form onSubmit={submit} className="mt-5 grid gap-4">
          <label className="grid gap-1.5 text-sm">
            <span className="text-ink-2">Dimension</span>
            <input
              ref={first}
              list="send-dimensions"
              value={dimension}
              onChange={(e) => chooseDimension(e.target.value)}
              placeholder="speed"
              className="h-11 rounded-xl border border-rule bg-panel-2 px-3"
            />
            <datalist id="send-dimensions">
              {known.map((k) => (
                <option key={`${k.dataType}|${k.dimension}`} value={k.dimension} />
              ))}
            </datalist>
          </label>
          <div className="grid grid-cols-[8rem_1fr] gap-3">
            <label className="grid gap-1.5 text-sm">
              <span className="text-ink-2">Data type</span>
              <select
                value={dataType}
                onChange={(e) => setDataType(e.target.value as DataType)}
                className="h-11 rounded-xl border border-rule bg-panel-2 px-3"
              >
                {DATA_TYPES.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </select>
            </label>
            <label className="grid gap-1.5 text-sm">
              <span className="text-ink-2">Value</span>
              <input
                inputMode="decimal"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder="44"
                className="h-11 rounded-xl border border-rule bg-panel-2 px-3"
              />
            </label>
          </div>
          <p role="status" className={`min-h-5 text-sm ${status?.ok === false ? "text-critical" : "text-muted"}`}>
            {status?.text}
          </p>
          <div className="flex flex-wrap justify-end gap-2">
            <button
              type="button"
              disabled={busy}
              onClick={() => run(pushRandom)}
              className="h-11 rounded-xl border border-primary px-4 text-sm font-semibold text-primary disabled:opacity-50"
            >
              Send random
            </button>
            <button type="submit" disabled={busy} className="h-11 rounded-xl bg-primary px-5 text-sm font-semibold text-primary-ink disabled:opacity-50">
              Send
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
