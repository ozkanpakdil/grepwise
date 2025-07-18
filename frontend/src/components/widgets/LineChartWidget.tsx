import React, { useState } from 'react';
import { DashboardWidget } from '@/api/dashboard';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  ResponsiveContainer 
} from 'recharts';

interface LineChartWidgetProps {
  data: any;
  widget: DashboardWidget;
}

const LineChartWidget: React.FC<LineChartWidgetProps> = ({ data, widget: _widget }) => {
  const [hoveredPoint, setHoveredPoint] = useState<any>(null);

  // Process the data for line chart visualization
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
        
        // Extract numeric fields for the line chart
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
      
      // If no time-based fields, create a simple line chart with numeric values
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
          No data available for line chart visualization
        </div>
      </div>
    );
  }

  // Determine which fields to display in the line chart
  const getLineDataKeys = () => {
    if (chartData.length === 0) return [];
    
    const firstItem = chartData[0];
    const keys = Object.keys(firstItem).filter(key => 
      key !== 'time' && key !== 'timestamp' && key !== 'name' && key !== 'index'
    );
    
    return keys;
  };

  const dataKeys = getLineDataKeys();
  const colors = [
    '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
    '#06b6d4', '#f97316', '#84cc16', '#ec4899', '#6366f1'
  ];

  // Determine x-axis data key
  const xAxisDataKey = chartData[0].time ? 'time' : (chartData[0].name ? 'name' : 'index');

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={chartData}
            margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
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
              tick={{ fontSize: 12 }}
              tickFormatter={(value) => {
                // Truncate long labels
                return typeof value === 'string' && value.length > 10 
                  ? `${value.substring(0, 10)}...` 
                  : value;
              }}
            />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip 
              formatter={(value, name) => [value, name]}
              labelFormatter={(label) => `Time: ${label}`}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            {dataKeys.map((key, index) => (
              <Line
                key={key}
                type="monotone"
                dataKey={key}
                stroke={colors[index % colors.length]}
                activeDot={{ r: 8 }}
                dot={{ r: 4 }}
                name={key}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Summary */}
      <div className="text-xs text-muted-foreground text-center pt-2 border-t">
        {chartData.length} data points • {dataKeys.length} series
      </div>
    </div>
  );
};

export default LineChartWidget;