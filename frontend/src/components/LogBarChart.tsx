import React, { useState } from 'react';
import { TimeSlot } from '@/api/logSearch';

interface LogBarChartProps {
  timeSlots: TimeSlot[];
  timeRange: string | undefined;
  onTimeSlotClick: (slot: TimeSlot) => void;
}

const LogBarChart: React.FC<LogBarChartProps> = ({ 
  timeSlots, 
  timeRange,
  onTimeSlotClick 
}) => {
  const [hoveredSlot, setHoveredSlot] = useState<TimeSlot | null>(null);

  // Find the maximum count to normalize bar heights
  const maxCount = Math.max(...timeSlots.map(slot => slot.count), 1);

  // Format the timestamp based on the time range
  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);

    if (timeRange === '1h') {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (timeRange === '3h' || timeRange === '12h') {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (timeRange === '24h') {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (timeRange === 'custom') {
      // For custom ranges, adapt based on the range duration
      const firstSlot = timeSlots[0]?.time;
      const lastSlot = timeSlots[timeSlots.length - 1]?.time;
      const rangeDuration = lastSlot && firstSlot ? lastSlot - firstSlot : 0;

      if (rangeDuration < 24 * 60 * 60 * 1000) { // Less than a day
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      } else if (rangeDuration < 7 * 24 * 60 * 60 * 1000) { // Less than a week
        return date.toLocaleDateString([], { weekday: 'short', hour: '2-digit', minute: '2-digit' });
      } else {
        return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
      }
    }

    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="mt-6 mb-8">
      <h3 className="text-lg font-medium mb-2">Log Distribution</h3>

      {timeSlots.length === 0 ? (
        <div className="text-center py-4 text-muted-foreground">
          No data available for the selected time range
        </div>
      ) : (
        <div className="relative h-40">
          {/* Tooltip for hovered bar */}
          {hoveredSlot && (
            <div 
              className="absolute bg-background border rounded-md shadow-md p-2 z-10 text-sm"
              style={{ 
                left: `${(timeSlots.indexOf(hoveredSlot) / timeSlots.length) * 100}%`,
                bottom: '100%',
                transform: 'translateX(-50%)',
                marginBottom: '8px'
              }}
            >
              <div className="font-medium">{formatTime(hoveredSlot.time)}</div>
              <div>{hoveredSlot.count} log{hoveredSlot.count !== 1 ? 's' : ''}</div>
            </div>
          )}

          {/* Bar chart */}
          <div className="flex items-end h-32 gap-1 border-b border-l relative">
            {timeSlots.map((slot, index) => {
              const height = slot.count > 0 ? (slot.count / maxCount) * 100 : 0;

              return (
                <div 
                  key={index}
                  className="flex-1 flex flex-col items-center"
                  onMouseEnter={() => setHoveredSlot(slot)}
                  onMouseLeave={() => setHoveredSlot(null)}
                  onDoubleClick={() => onTimeSlotClick(slot)}
                >
                  <div 
                    className={`w-full ${height > 0 ? 'bg-primary/80 hover:bg-primary cursor-pointer' : ''}`}
                    style={{ height: `${height}%` }}
                    title={`${formatTime(slot.time)}: ${slot.count} logs`}
                  />

                  {/* Only show some x-axis labels to avoid overcrowding */}
                  {(index === 0 || index === timeSlots.length - 1 || index % Math.ceil(timeSlots.length / 5) === 0) && (
                    <div className="text-xs text-muted-foreground mt-1 transform -rotate-45 origin-top-left whitespace-nowrap">
                      {formatTime(slot.time)}
                    </div>
                  )}
                </div>
              );
            })}

            {/* Y-axis labels */}
            <div className="absolute -left-8 bottom-0 h-full flex flex-col justify-between text-xs text-muted-foreground">
              <div>Max: {maxCount}</div>
              <div>0</div>
            </div>
          </div>

          <div className="mt-2 text-xs text-center text-muted-foreground">
            Double-click on a bar to zoom into that time period
          </div>
        </div>
      )}
    </div>
  );
};

export default LogBarChart;
