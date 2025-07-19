import React, { useState, useEffect } from 'react';
import { DashboardWidget } from '@/api/dashboard';
import { 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  ResponsiveContainer 
} from 'recharts';

interface AreaChartWidgetProps {
  data: any;
  widget: DashboardWidget;
}

// Helper function to format numbers in a compact way (e.g., 1000 -> 1K)
const formatCompactNumber = (num: number): string => {
  if (num < 1000) return num.toString();
  if (num < 1000000) return (num / 1000).toFixed(1) + 'K';
  return (num / 1000000).toFixed(1) + 'M';
};

const AreaChartWidget: React.FC<AreaChartWidgetProps> = ({ data, widget: _widget }) => {
  const [hoveredPoint, setHoveredPoint] = useState<any>(null);
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

  // Process the data for area chart visualization
  const processData = () => {
    if (!data || !data.timeSlots) {
      return [];
    }

    // Use time slots if available (for time-based queries)
    if (data.timeSlots && data.timeSlots.length > 0) {
      return data.timeSlots.map((slot: any) => ({
        time: new Date(slot.time).toLocaleTimeString([], { 
          hour: '2-digit', 
          minute: '2-digit' 
        }),
        count: slot.count,
        timestamp: slot.time,
      }));
    }

    // For stats queries, try to extract time-series data
    if (data.results && data.results.length > 0) {
      const result = data.results[0];
      const chartData = [];
      
      // Look for time-based fields
      if (result._time || result.timestamp) {
        const timeField = result._time ? '_time' : 'timestamp';
        const timeValue = result[timeField];
        
        // Extract numeric fields for the area chart
        for (const [key, value] of Object.entries(result)) {
          if (key !== timeField && typeof value === 'number') {
            chartData.push({
              time: new Date(timeValue).toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit' 
              }),
              [key]: value,
              timestamp: timeValue,
            });
          }
        }
        
        if (chartData.length > 0) {
          return chartData;
        }
      }
      
      // If no time-based fields, create a simple area chart with numeric values
      const chartData2 = [];
      let index = 0;
      
      for (const [key, value] of Object.entries(result)) {
        if (key !== '_time' && key !== 'timestamp' && typeof value === 'number') {
          chartData2.push({
            name: key,
            value: value,
            index: index++,
          });
        }
      }
      
      if (chartData2.length > 0) {
        return chartData2.slice(0, 20); // Limit to 20 points for readability
      }
    }

    return [];
  };

  const chartData = processData();

  if (chartData.length === 0) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-sm text-muted-foreground">
          No data available for area chart visualization
        </div>
      </div>
    );
  }

  // Determine which fields to display in the area chart
  const getAreaDataKeys = () => {
    if (chartData.length === 0) return [];
    
    const firstItem = chartData[0];
    const keys = Object.keys(firstItem).filter(key => 
      key !== 'time' && key !== 'timestamp' && key !== 'name' && key !== 'index'
    );
    
    return keys;
  };

  const dataKeys = getAreaDataKeys();
  const colors = [
    '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
    '#06b6d4', '#f97316', '#84cc16', '#ec4899', '#6366f1'
  ];

  // Determine x-axis data key
  const xAxisDataKey = chartData[0].time ? 'time' : (chartData[0].name ? 'name' : 'index');

  // Responsive chart configuration based on screen width
  const getResponsiveConfig = () => {
    if (screenWidth < 480) {
      return {
        margin: { top: 5, right: 5, left: 0, bottom: 0 },
        fontSize: 10,
        activeDotRadius: 5,
        maxLabelLength: 6,
        hideEveryNthTick: 2
      };
    } else if (screenWidth < 640) {
      return {
        margin: { top: 8, right: 8, left: 0, bottom: 0 },
        fontSize: 11,
        activeDotRadius: 6,
        maxLabelLength: 8,
        hideEveryNthTick: 1
      };
    } else {
      return {
        margin: { top: 10, right: 10, left: 0, bottom: 0 },
        fontSize: 12,
        activeDotRadius: 8,
        maxLabelLength: 10,
        hideEveryNthTick: 0
      };
    }
  };

  const config = getResponsiveConfig();

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart
            data={chartData}
            margin={config.margin}
            onMouseMove={(e) => {
              if (e.activePayload) {
                setHoveredPoint(e.activePayload[0].payload);
              }
            }}
            onMouseLeave={() => setHoveredPoint(null)}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis 
              dataKey={xAxisDataKey} 
              tick={{ fontSize: config.fontSize }}
              tickFormatter={(value) => {
                // Truncate long labels based on screen size
                return typeof value === 'string' && value.length > config.maxLabelLength 
                  ? `${value.substring(0, config.maxLabelLength)}...` 
                  : value;
              }}
              // On small screens, hide some ticks to prevent overcrowding
              interval={config.hideEveryNthTick}
            />
            <YAxis 
              tick={{ fontSize: config.fontSize }} 
              // Format large numbers in a compact way on small screens
              tickFormatter={screenWidth < 480 ? formatCompactNumber : undefined}
            />
            <Tooltip 
              formatter={(value, name) => [value, name]}
              labelFormatter={(label) => `Time: ${label}`}
              contentStyle={{ fontSize: config.fontSize }}
            />
            <Legend wrapperStyle={{ fontSize: config.fontSize }} />
            {dataKeys.map((key, index) => (
              <Area
                key={key}
                type="monotone"
                dataKey={key}
                stroke={colors[index % colors.length]}
                fill={`${colors[index % colors.length]}33`} // Add transparency to fill color
                activeDot={{ r: config.activeDotRadius }}
                name={key}
                stackId={dataKeys.length > 1 ? "1" : undefined} // Stack areas if multiple series
              />
            ))}
          </AreaChart>
        </ResponsiveContainer>
      </div>

      {/* Summary */}
      <div className={`${screenWidth < 640 ? 'text-[10px]' : 'text-xs'} text-muted-foreground text-center pt-2 border-t`}>
        {chartData.length} data points • {dataKeys.length} series
      </div>
    </div>
  );
};

export default AreaChartWidget;