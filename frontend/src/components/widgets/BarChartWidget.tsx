import React, { useState } from 'react';
import { DashboardWidget } from '@/api/dashboard';

interface BarChartWidgetProps {
  data: any;
  widget: DashboardWidget;
}

const BarChartWidget: React.FC<BarChartWidgetProps> = ({ data, widget: _widget }) => {
  const [hoveredBar, setHoveredBar] = useState<any>(null);

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
            className="absolute bg-black text-white text-xs rounded px-2 py-1 z-10 pointer-events-none"
            style={{ 
              left: `${(chartData.indexOf(hoveredBar) / chartData.length) * 100}%`,
              bottom: '100%',
              transform: 'translateX(-50%)',
              marginBottom: '8px'
            }}
          >
            <div className="font-medium">{hoveredBar.label}</div>
            <div>{hoveredBar.value}</div>
          </div>
        )}

        {/* Bar Chart */}
        <div className="flex items-end h-full gap-1 p-2">
          {chartData.map((item: any, index: number) => {
            const height = (item.value / maxValue) * 100;
            
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
                
                {/* X-axis label (show only for smaller datasets or every nth item) */}
                {(chartData.length <= 10 || index % Math.ceil(chartData.length / 8) === 0) && (
                  <div className="text-xs text-muted-foreground mt-1 truncate w-full text-center">
                    {item.label}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Y-axis labels */}
        <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-xs text-muted-foreground pr-1">
          <div>{maxValue}</div>
          <div>{Math.round(maxValue / 2)}</div>
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