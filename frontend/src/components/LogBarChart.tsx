import React, { useEffect, useState } from 'react';
import { TimeSlot } from '@/api/logSearch';

interface LogBarChartProps {
  timeSlots: TimeSlot[];
  timeRange: string | undefined;
  onTimeSlotClick: (slot: TimeSlot) => void; // Will be called on double-click per requirements
}

// Helper function to format numbers in a compact way (e.g., 1000 -> 1K)
// (unused helper removed to satisfy noUnusedLocals)

const LogBarChart: React.FC<LogBarChartProps> = ({ timeSlots, onTimeSlotClick }) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  // screenWidth used to trigger responsive layout recalculation
    const [screenWidth, setScreenWidth] = useState<number>(typeof window !== 'undefined' ? window.innerWidth : 1024);

  // use it to satisfy TS noUnusedLocals for responsive recalculation
  void screenWidth;

  // Handle screen resize
  useEffect(() => {
    const handleResize = () => {
      setScreenWidth(window.innerWidth);
    };

    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  // Find the maximum count to normalize bar heights
  const maxCount = Math.max(...timeSlots.map((slot) => slot.count), 1);

  // Format the timestamp label (UTC hours/mins for consistency)
  const formatUtcTime = (timestamp: number) => {
    const d = new Date(timestamp);
    return d.toUTCString().split(' ')[4]; // HH:MM:SS in UTC
  };

  return (
    <div className="mt-6 mb-8">
      <div className="flex justify-between items-center mb-2">
        <h3 className="text-lg font-medium">Log Distribution</h3>
        {/* Subtle optional hint */}
        <span className="text-xs text-muted-foreground">Double-click a bar to zoom</span>
      </div>
      {timeSlots.length === 0 ? (
        <div className="text-center py-4 text-muted-foreground">No data available for the selected time range</div>
      ) : (
        <div className="w-full h-64">
          <div className="relative w-full h-full flex items-end gap-[2px]" role="list">
            {timeSlots.map((slot, idx) => {
              const heightPct = (slot.count / maxCount) * 100;
              const title = `${new Date(slot.time).toUTCString()}\nLogs: ${slot.count}`;
              return (
                <div
                  key={slot.time}
                  role="listitem"
                  title={title}
                  onDoubleClick={() => onTimeSlotClick(slot)}
                  onMouseEnter={() => setHoveredIndex(idx)}
                  onMouseLeave={() => setHoveredIndex(null)}
                  className="flex-1 h-full flex flex-col justify-end cursor-pointer"
                  style={{ minWidth: `${Math.max(2, 100 / Math.max(1, timeSlots.length))}%` }}
                >
                  <div className="bg-blue-500 transition-all duration-150" style={{ height: `${heightPct}%` }} />
                  {/* X-axis label in UTC below each bar for sparse data; hide if too many */}
                  {timeSlots.length <= 24 && (
                    <div className="text-[10px] text-center mt-1 select-none">{formatUtcTime(slot.time)}</div>
                  )}
                </div>
              );
            })}
            {/* Hover tooltip (non-intrusive) */}
            {hoveredIndex !== null && hoveredIndex >= 0 && hoveredIndex < timeSlots.length && (
              <>
                <div className="pointer-events-none absolute -top-1 right-2 text-[10px] text-muted-foreground bg-background/80 px-1 rounded">
                  Double-click to zoom
                </div>
                {/* Show count near hovered bar */}
                {(() => {
                  const hovered = timeSlots[hoveredIndex];
                  const heightPct = (hovered.count / maxCount) * 100;
                  return (
                    <div
                      className="pointer-events-none absolute text-[10px] bg-background/90 px-1 rounded border"
                      style={{
                        left: `calc(${((hoveredIndex + 0.5) * 100) / Math.max(1, timeSlots.length)}% - 16px)`,
                        bottom: `calc(${heightPct}% + 2px)`,
                      }}
                    >
                      {hovered.count}
                    </div>
                  );
                })()}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default LogBarChart;
