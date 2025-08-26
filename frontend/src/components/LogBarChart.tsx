import React, { useState, useEffect } from 'react';
import { TimeSlot } from '@/api/logSearch';
import { BarChart } from '@mui/x-charts/BarChart';

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
        <div className="w-full h-64">
          <BarChart
            height={240}
            series={[{ data: timeSlots.map(s => s.count), label: 'Logs' }]}
            xAxis={[{
              scaleType: 'band',
              data: timeSlots.map(s => new Date(s.time).toISOString()),
              valueFormatter: v => new Date(String(v)).toUTCString().split(' ')[4]
            }]}
            yAxis={[{ min: 0, max: Math.max(1, ...timeSlots.map(s => s.count)) }]}
            slotProps={{ legend: { hidden: true } }}
            margin={{ top: 10, right: 10, bottom: 40, left: 50 }}
          />
        </div>
      )}
    </div>
  );
};

export default LogBarChart;
