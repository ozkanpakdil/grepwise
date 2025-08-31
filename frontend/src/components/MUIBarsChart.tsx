import * as React from 'react';
import { HistogramData } from '@/api/logSearch';

interface Props {
  data: HistogramData[];
  onBarDoubleClick?: (start: number, end: number) => void;
}

// Convert ISO timestamp to a readable label (UTC)
const formatLabel = (iso: string) => {
  const d = new Date(iso);
  return d.toUTCString().split(' ')[4]; // HH:MM:SS
};

export const MUIBarsChart: React.FC<Props> = ({ data, onBarDoubleClick }) => {
  const values = data.map((d) => d.count);
  const max = Math.max(1, ...values);
  const [hoveredIndex, setHoveredIndex] = React.useState<number | null>(null);

  return (
    <div className="w-full h-64">
      <div className="relative w-full h-full flex items-end gap-[2px]" role="list">
        {data.map((d, idx) => {
          const heightPct = (d.count / max) * 100;
          const title = `${new Date(d.timestamp).toUTCString()}\nLogs: ${d.count}`;
          const start = new Date(d.timestamp).getTime();
          // infer slot size from next or previous bucket
          let slotSizeMs = 60 * 1000; // default 1m
          if (idx < data.length - 1) slotSizeMs = Math.max(1000, new Date(data[idx + 1].timestamp).getTime() - start);
          else if (idx > 0) slotSizeMs = Math.max(1000, start - new Date(data[idx - 1].timestamp).getTime());

          return (
            <div
              key={d.timestamp}
              role="listitem"
              title={title}
              onDoubleClick={() => onBarDoubleClick?.(start, start + slotSizeMs)}
              onMouseEnter={() => setHoveredIndex(idx)}
              onMouseLeave={() => setHoveredIndex(null)}
              className="flex-1 h-full flex flex-col justify-end cursor-pointer"
              style={{ minWidth: `${Math.max(2, 100 / Math.max(1, data.length))}%` }}
            >
              <div className="bg-blue-500 transition-all duration-150" style={{ height: `${heightPct}%` }} />
              {data.length <= 24 && (
                <div className="text-[10px] text-center mt-1 select-none">{formatLabel(d.timestamp)}</div>
              )}
            </div>
          );
        })}
        {hoveredIndex !== null &&
          hoveredIndex >= 0 &&
          hoveredIndex < data.length &&
          (() => {
            const hovered = data[hoveredIndex];
            const heightPct = (hovered.count / max) * 100;
            return (
              <div
                className="pointer-events-none absolute text-[10px] bg-background/90 px-1 rounded border"
                style={{
                  left: `calc(${((hoveredIndex + 0.5) * 100) / Math.max(1, data.length)}% - 16px)`,
                  bottom: `calc(${heightPct}% + 2px)`,
                }}
              >
                {hovered.count}
              </div>
            );
          })()}
      </div>
    </div>
  );
};

export default MUIBarsChart;
