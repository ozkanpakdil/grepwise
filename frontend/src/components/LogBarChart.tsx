import React, { useState, useEffect } from 'react';
import { TimeSlot } from '@/api/logSearch';

interface LogBarChartProps {
  timeSlots: TimeSlot[];
  timeRange: string | undefined;
  onTimeSlotClick: (slot: TimeSlot) => void;
}

// Helper function to format numbers in a compact way (e.g., 1000 -> 1K)
const formatCompactNumber = (num: number): string => {
  if (num < 1000) return num.toString();
  if (num < 1000000) return (num / 1000).toFixed(1) + 'K';
  return (num / 1000000).toFixed(1) + 'M';
};

const LogBarChart: React.FC<LogBarChartProps> = ({ 
  timeSlots, 
  timeRange,
  onTimeSlotClick 
}) => {
  const [hoveredSlot, setHoveredSlot] = useState<TimeSlot | null>(null);
  const [screenWidth, setScreenWidth] = useState<number>(typeof window !== 'undefined' ? window.innerWidth : 1024);
  
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
        <div className={`relative ${screenWidth < 480 ? 'h-36' : 'h-40'}`}>
          {/* Tooltip for hovered bar */}
          {hoveredSlot && (
            <div 
              className={`absolute bg-background border rounded-md shadow-md p-2 z-10 ${screenWidth < 640 ? 'text-[10px]' : 'text-sm'}`}
              style={{ 
                left: `${(timeSlots.indexOf(hoveredSlot) / timeSlots.length) * 100}%`,
                bottom: '100%',
                transform: 'translateX(-50%)',
                marginBottom: '8px',
                maxWidth: screenWidth < 480 ? '120px' : '200px'
              }}
            >
              <div className="font-medium truncate">{formatTime(hoveredSlot.time)}</div>
              <div>{hoveredSlot.count} log{hoveredSlot.count !== 1 ? 's' : ''}</div>
            </div>
          )}

          {/* Bar chart */}
          <div className={`flex items-end ${screenWidth < 480 ? 'h-28' : 'h-32'} ${screenWidth < 640 ? 'gap-0.5' : 'gap-1'} border-b border-l relative`}>
            {timeSlots.map((slot, index) => {
              const height = slot.count > 0 ? (slot.count / maxCount) * 100 : 0;
              
              // Calculate how many labels to show based on screen width
              const labelDivisor = screenWidth < 480 ? 10 : 
                                 screenWidth < 640 ? 8 : 
                                 screenWidth < 768 ? 6 : 5;
              
              // Determine if this bar should show a label
              const showLabel = index === 0 || 
                               index === timeSlots.length - 1 || 
                               index % Math.ceil(timeSlots.length / labelDivisor) === 0;

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
                  {showLabel && (
                    <div 
                      className={`${screenWidth < 640 ? 'text-[10px]' : 'text-xs'} text-muted-foreground mt-1 ${screenWidth < 640 ? 'transform -rotate-45 origin-top-left' : ''} whitespace-nowrap`}
                    >
                      {screenWidth < 480 && formatTime(slot.time).length > 6 
                        ? formatTime(slot.time).substring(0, 6) + '...' 
                        : formatTime(slot.time)}
                    </div>
                  )}
                </div>
              );
            })}

            {/* Y-axis labels */}
            <div className={`absolute ${screenWidth < 480 ? '-left-6' : '-left-8'} bottom-0 h-full flex flex-col justify-between ${screenWidth < 640 ? 'text-[10px]' : 'text-xs'} text-muted-foreground`}>
              <div>Max: {screenWidth < 480 ? formatCompactNumber(maxCount) : maxCount}</div>
              <div>0</div>
            </div>
          </div>

          <div className={`mt-2 ${screenWidth < 640 ? 'text-[10px]' : 'text-xs'} text-center text-muted-foreground`}>
            {screenWidth < 480 ? 'Double-tap to zoom' : 'Double-click on a bar to zoom into that time period'}
          </div>
        </div>
      )}
    </div>
  );
};

export default LogBarChart;
