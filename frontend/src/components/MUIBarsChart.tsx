import { HistogramData } from '@/api/logSearch';
import { FC, useRef, useState } from 'react';

interface Props {
  data: HistogramData[];
  onBarDoubleClick?: (start: number, end: number) => void;
}

// Convert ISO timestamp to a readable label (local time). If day changes, show Mon DD.
const formatLabel = (iso: string) => {
  const d = new Date(iso);
  // If bucket represents a date boundary (00:00), show day label for more meaningful axis
  if (d.getHours() === 0 && d.getMinutes() === 0) {
    const mon = d.toLocaleString(undefined, { month: 'short' });
    return `${mon} ${d.getDate()}`;
  }
  return d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
};

export const MUIBarsChart: FC<Props> = ({ data, onBarDoubleClick }) => {
  const values = data.map((d) => d.count);
  const max = Math.max(1, ...values);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [mousePos, setMousePos] = useState<{ x: number; y: number } | null>(null);

  // Determine bottom label frequency to reduce overlap
  const bucketCount = data.length;
  const labelEvery = bucketCount <= 12 ? 1 : bucketCount <= 18 ? 2 : 3;

  return (
    <div className="w-full h-64 overflow-hidden">
      <div
        ref={containerRef}
        className="relative w-full h-full flex items-end gap-[2px] overflow-hidden"
        role="list"
        // Keep a small bottom padding so rotated labels can be visible without pushing bars too far up
        style={{ paddingRight: 2, paddingBottom: 2 }}
      >
        {data.map((d, idx) => {
          const heightPct = (d.count / max) * 100;
          const start = new Date(d.timestamp).getTime();
          // infer slot size from next or previous bucket
          let slotSizeMs = 60 * 1000; // default 1m
          if (idx < data.length - 1) slotSizeMs = Math.max(1000, new Date(data[idx + 1].timestamp).getTime() - start);
          else if (idx > 0) slotSizeMs = Math.max(1000, start - new Date(data[idx - 1].timestamp).getTime());

          // Decide whether to show top labels (counts). Show when number of bars is reasonable (<= 60).
          const showTopLabels = data.length <= 60;

          return (
            <div
              key={d.timestamp}
              role="listitem"
              // Remove native title to avoid browser tooltip delay; we render our own instant tooltip below
              onDoubleClick={() => onBarDoubleClick?.(start, start + slotSizeMs)}
              onMouseEnter={(e) => {
                setHoveredIndex(idx);
                const rect = containerRef.current?.getBoundingClientRect();
                if (rect) setMousePos({ x: e.clientX - rect.left, y: e.clientY - rect.top });
              }}
              onMouseMove={(e) => {
                const rect = containerRef.current?.getBoundingClientRect();
                if (rect) setMousePos({ x: e.clientX - rect.left, y: e.clientY - rect.top });
              }}
              onMouseLeave={() => {
                setHoveredIndex(null);
                setMousePos(null);
              }}
              className="flex-1 h-full flex flex-col justify-end cursor-pointer relative"
              style={{ minWidth: `${Math.max(2, 100 / Math.max(1, data.length))}%` }}
            >
              {/* Bar */}
              <div
                className="bg-blue-500 transition-all duration-150"
                style={{ height: `${heightPct}%`, minHeight: d.count > 0 ? 1 : 0 }}
              />
              {/* Top count label */}
              {showTopLabels && (
                <div
                  className="pointer-events-none absolute text-[10px] text-foreground/90 px-0.5 rounded"
                  style={{
                    left: '50%',
                    transform: 'translateX(-50%)',
                    bottom: `calc(${heightPct}% + 2px)`,
                  }}
                >
                  {d.count}
                </div>
              )}
              {/* Bottom tick label when few buckets (rotate to avoid overlap) */}
              {data.length <= 14 && idx % labelEvery === 0 && (
                <div
                  className="pointer-events-none absolute text-[10px] select-none"
                  style={{
                    left: '50%',
                    transform: 'translateX(-50%) rotate(-50deg)',
                    transformOrigin: 'top left',
                    whiteSpace: 'nowrap',
                    bottom: 4,
                  }}
                >
                  {formatLabel(d.timestamp)}
                </div>
              )}
            </div>
          );
        })}
        {hoveredIndex !== null &&
          hoveredIndex >= 0 &&
          hoveredIndex < data.length &&
          mousePos &&
          (() => {
            const hovered = data[hoveredIndex];
            const ts = new Date(hovered.timestamp).toLocaleString();
            const rect = containerRef.current?.getBoundingClientRect();
            const cw = rect?.width ?? 0;
            const ch = rect?.height ?? 0;
            const tooltipWidth = 160;
            const estHeight = 54; // rough estimate for clamping
            // Prefer showing below-right of cursor, clamp to container bounds
            const desiredLeft = mousePos.x + 12;
            const desiredTop = mousePos.y + 12;
            const left = Math.max(4, Math.min(desiredLeft, cw ? cw - tooltipWidth - 4 : desiredLeft));
            const top = Math.max(4, Math.min(desiredTop, ch ? ch - estHeight - 4 : desiredTop));
            return (
              <div
                className="pointer-events-none absolute text-[11px] bg-background/95 px-2 py-1 rounded border shadow"
                style={{
                  left,
                  top,
                  width: tooltipWidth,
                  textAlign: 'center',
                }}
              >
                <div className="font-medium">{hovered.count} logs</div>
                <div className="opacity-80">{ts}</div>
              </div>
            );
          })()}
      </div>
    </div>
  );
};

export default MUIBarsChart;
