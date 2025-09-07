import { HistogramData } from '@/api/logSearch';
import { FC, useMemo, useRef, useState } from 'react';

interface Props {
  data: HistogramData[];
  onBarDoubleClick?: (start: number, end: number) => void;
}

// Convert ISO timestamp to a readable label (local time). If day changes, show Mon DD.
const formatLabel = (iso: string) => {
  const d = new Date(iso);
  if (d.getHours() === 0 && d.getMinutes() === 0) {
    const mon = d.toLocaleString(undefined, { month: 'short' });
    return `${mon} ${d.getDate()}`;
  }
  return d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
};

type MousePos = { x: number; y: number } | null;

function useRelativeMouse(containerRef: React.RefObject<HTMLDivElement>, setMousePos: (p: MousePos) => void) {
  return (e: React.MouseEvent) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (rect) setMousePos({ x: e.clientX - rect.left, y: e.clientY - rect.top });
  };
}

function Tooltip({
  containerRef,
  mousePos,
  item,
}: {
  containerRef: React.RefObject<HTMLDivElement>;
  mousePos: { x: number; y: number };
  item: HistogramData;
}) {
  const ts = new Date(item.timestamp).toLocaleString();
  const rect = containerRef.current?.getBoundingClientRect();
  const cw = rect?.width ?? 0;
  const ch = rect?.height ?? 0;
  const tooltipWidth = 160;
  const estHeight = 54;
  const desiredLeft = mousePos.x + 12;
  const desiredTop = mousePos.y + 12;
  const left = Math.max(4, Math.min(desiredLeft, cw ? cw - tooltipWidth - 4 : desiredLeft));
  const top = Math.max(4, Math.min(desiredTop, ch ? ch - estHeight - 4 : desiredTop));
  return (
    <div
      className="pointer-events-none absolute text-[11px] bg-background/95 px-2 py-1 rounded border shadow"
      style={{ left, top, width: tooltipWidth, textAlign: 'center' }}
    >
      <div className="font-medium">{item.count} logs</div>
      <div className="opacity-80">{ts}</div>
    </div>
  );
}

export const MUIBarsChart: FC<Props> = ({ data, onBarDoubleClick }) => {
  const values = useMemo(() => data.map((d) => d.count), [data]);
  const max = useMemo(() => Math.max(1, ...values), [values]);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [mousePos, setMousePos] = useState<MousePos>(null);

  const bucketCount = data.length;
  const labelEvery = useMemo(() => (bucketCount <= 12 ? 1 : bucketCount <= 18 ? 2 : 3), [bucketCount]);
  const showTopLabels = bucketCount <= 60;

  // Precompute slot sizes (ms) for each bar
  const slotSizes = useMemo(() => {
    if (data.length === 0) return [] as number[];
    const starts = data.map((d) => new Date(d.timestamp).getTime());
    return data.map((_, idx) => {
      let size = 60 * 1000; // default 1m
      if (idx < data.length - 1) size = Math.max(1000, starts[idx + 1] - starts[idx]);
      else if (idx > 0) size = Math.max(1000, starts[idx] - starts[idx - 1]);
      return size;
    });
  }, [data]);

  const updateMouse = useRelativeMouse(containerRef, setMousePos);

  return (
    <div className="w-full h-64 overflow-hidden">
      <div
        ref={containerRef}
        className="relative w-full h-full overflow-hidden"
        role="list"
        style={{ paddingBottom: 2, display: 'grid', gridAutoFlow: 'column', gridAutoColumns: '1fr', columnGap: '1px' }}
      >
        {data.map((d, idx) => {
          const heightPct = (d.count / max) * 100;
          const start = new Date(d.timestamp).getTime();
          const slotSizeMs = slotSizes[idx] ?? 60 * 1000;

          return (
            <div
              key={d.timestamp}
              role="listitem"
              onDoubleClick={() => onBarDoubleClick?.(start, start + slotSizeMs)}
              onMouseEnter={(e) => {
                setHoveredIndex(idx);
                updateMouse(e);
              }}
              onMouseMove={updateMouse}
              onMouseLeave={() => {
                setHoveredIndex(null);
                setMousePos(null);
              }}
              className="h-full flex flex-col justify-end cursor-pointer relative"
              style={{ minWidth: 0 }}
            >
              <div
                className="bg-blue-500 transition-all duration-150"
                style={{ height: `${heightPct}%`, minHeight: 1 }}
              />
              {showTopLabels && (
                <div
                  className="pointer-events-none absolute text-[10px] text-foreground/90 px-0.5 rounded"
                  style={{ left: '50%', transform: 'translateX(-50%)', bottom: `calc(${heightPct}% + 2px)` }}
                >
                  {d.count}
                </div>
              )}
              {bucketCount <= 14 && idx % labelEvery === 0 && (
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
        {hoveredIndex !== null && hoveredIndex >= 0 && hoveredIndex < data.length && mousePos && (
          <Tooltip containerRef={containerRef} mousePos={mousePos} item={data[hoveredIndex]} />
        )}
      </div>
    </div>
  );
};

export default MUIBarsChart;
