import React, { useState, useEffect } from 'react';
import { DashboardWidget } from '@/api/dashboard';

interface BarChartWidgetProps {
  data: any;
  widget: DashboardWidget;
}

// Helper function to format numbers in a compact way (e.g., 1000 -> 1K)
const formatCompactNumber = (num: number): string => {
  if (num < 1000) return num.toString();
  if (num < 1000000) return (num / 1000).toFixed(1) + 'K';
  return (num / 1000000).toFixed(1) + 'M';
};

const BarChartWidget: React.FC<BarChartWidgetProps> = ({ data, widget: _widget }) => {
  const [hoveredBar, setHoveredBar] = useState<any>(null);
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

  // Process the data for bar chart visualization
  const processData = () => {
    if (!data || !data.timeSlots) {
      return [];
    }

    // Use time slots if available (for time-based queries)
    if (data.timeSlots && data.timeSlots.length > 0) {
      return data.timeSlots.map((slot: any) => ({
        label: new Date(slot.time).toLocaleTimeString([], { 
          hour: '2-digit', 
          minute: '2-digit' 
        }),
        value: slot.count,
        fullData: slot,
      }));
    }

    // For stats queries, try to extract key-value pairs
    if (data.results && data.results.length > 0) {
      const result = data.results[0];
      const chartData = [];
      
      // Look for common field patterns in stats results
      for (const [key, value] of Object.entries(result)) {
        if (key !== 'timestamp' && key !== '_time' && typeof value === 'number') {
          chartData.push({
            label: key,
            value: value,
            fullData: { key, value },
          });
        }
      }
      
      if (chartData.length > 0) {
        return chartData.slice(0, 20); // Limit to 20 bars for readability
      }
    }

    return [];
  };

  const chartData = processData();
  const maxValue = Math.max(...chartData.map((item: any) => item.value), 1);

  if (chartData.length === 0) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-sm text-muted-foreground">
          No data available for chart visualization
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* Chart Area */}
      <div className="flex-1 relative">
        {/* Tooltip */}
        {hoveredBar && (
          <div 
            className={`absolute bg-black text-white ${screenWidth < 640 ? 'text-[10px]' : 'text-xs'} rounded px-2 py-1 z-10 pointer-events-none`}
            style={{ 
              left: `${(chartData.indexOf(hoveredBar) / chartData.length) * 100}%`,
              bottom: '100%',
              transform: 'translateX(-50%)',
              marginBottom: '8px',
              // Ensure tooltip stays within viewport on small screens
              maxWidth: screenWidth < 480 ? '120px' : '200px'
            }}
          >
            <div className="font-medium truncate">{hoveredBar.label}</div>
            <div>{hoveredBar.value}</div>
          </div>
        )}

        {/* Bar Chart */}
        <div className={`flex items-end h-full ${screenWidth < 640 ? 'gap-0.5' : 'gap-1'} p-2`}>
          {chartData.map((item: any, index: number) => {
            const height = (item.value / maxValue) * 100;
            
            // Calculate how many labels to show based on screen width
            const labelDivisor = screenWidth < 480 ? 12 : 
                               screenWidth < 640 ? 10 : 
                               screenWidth < 768 ? 8 : 6;
            
            // Determine if this bar should show a label
            const showLabel = chartData.length <= (screenWidth < 640 ? 6 : 10) || 
                             index % Math.ceil(chartData.length / labelDivisor) === 0 ||
                             index === 0 || 
                             index === chartData.length - 1;
            
            return (
              <div 
                key={index}
                className="flex-1 flex flex-col items-center min-w-0"
                onMouseEnter={() => setHoveredBar(item)}
                onMouseLeave={() => setHoveredBar(null)}
              >
                <div 
                  className="w-full bg-blue-500 hover:bg-blue-600 cursor-pointer transition-colors rounded-t"
                  style={{ height: `${Math.max(height, 2)}%` }}
                  title={`${item.label}: ${item.value}`}
                />
                
                {/* X-axis label (adaptive based on screen size) */}
                {showLabel && (
                  <div className={`${screenWidth < 640 ? 'text-[10px]' : 'text-xs'} text-muted-foreground mt-1 truncate w-full text-center ${screenWidth < 480 ? 'transform -rotate-45 origin-top-left' : ''}`}>
                    {screenWidth < 480 && item.label.length > 6 ? item.label.substring(0, 6) + '...' : item.label}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Y-axis labels */}
        <div className={`absolute left-0 top-0 h-full flex flex-col justify-between ${screenWidth < 640 ? 'text-[10px]' : 'text-xs'} text-muted-foreground pr-1`}>
          <div>{screenWidth < 480 ? formatCompactNumber(maxValue) : maxValue}</div>
          {screenWidth >= 480 && <div>{Math.round(maxValue / 2)}</div>}
          <div>0</div>
        </div>
      </div>

      {/* Summary */}
      <div className="text-xs text-muted-foreground text-center pt-2 border-t">
        {chartData.length} data points • Max: {maxValue}
      </div>
    </div>
  );
};

export default BarChartWidget;