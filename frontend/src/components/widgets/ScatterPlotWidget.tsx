import React, { useState } from 'react';
import { DashboardWidget } from '@/api/dashboard';
import { 
  ScatterChart, 
  Scatter, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  ResponsiveContainer,
  ZAxis
} from 'recharts';

interface ScatterPlotWidgetProps {
  data: any;
  widget: DashboardWidget;
}

const ScatterPlotWidget: React.FC<ScatterPlotWidgetProps> = ({ data, widget: _widget }) => {
  const [hoveredPoint, setHoveredPoint] = useState<any>(null);

  // Process the data for scatter plot visualization
  const processData = () => {
    if (!data) {
      return { series: [], xLabel: '', yLabel: '' };
    }

    // For log entries, try to extract numeric fields for scatter plot
    if (data.results && data.results.length > 0) {
      const results = data.results;
      const numericFields: string[] = [];
      
      // Find numeric fields in the first result
      if (results[0]) {
        for (const [key, value] of Object.entries(results[0])) {
          if (typeof value === 'number' && key !== 'timestamp' && key !== '_time') {
            numericFields.push(key);
          }
        }
      }
      
      // Need at least 2 numeric fields for a scatter plot
      if (numericFields.length >= 2) {
        const xField = numericFields[0];
        const yField = numericFields[1];
        const zField = numericFields.length > 2 ? numericFields[2] : null;
        
        // Create scatter plot data
        const scatterData = results.map((result: any) => ({
          x: result[xField],
          y: result[yField],
          z: zField ? result[zField] : 10, // Use third field for point size if available
          name: result.name || result.id || '',
        }));
        
        return {
          series: [{ name: 'Data Points', data: scatterData }],
          xLabel: xField,
          yLabel: yField,
          zLabel: zField
        };
      }
      
      // If we don't have enough numeric fields, try to create a frequency scatter plot
      const frequencyMap: Record<string, Record<string, number>> = {};
      
      // Count occurrences of field combinations
      results.forEach((result: any) => {
        const fields = Object.keys(result).filter(key => 
          key !== 'timestamp' && key !== '_time' && key !== 'id'
        );
        
        if (fields.length >= 2) {
          const field1 = fields[0];
          const field2 = fields[1];
          
          const value1 = String(result[field1]);
          const value2 = String(result[field2]);
          
          if (!frequencyMap[value1]) {
            frequencyMap[value1] = {};
          }
          
          if (!frequencyMap[value1][value2]) {
            frequencyMap[value1][value2] = 0;
          }
          
          frequencyMap[value1][value2]++;
        }
      });
      
      // Convert frequency map to scatter data
      const scatterData: any[] = [];
      
      Object.entries(frequencyMap).forEach(([x, yValues]) => {
        Object.entries(yValues).forEach(([y, count]) => {
          scatterData.push({
            x,
            y,
            z: count,
            name: `${x}, ${y} (${count})`,
          });
        });
      });
      
      if (scatterData.length > 0) {
        return {
          series: [{ name: 'Frequency', data: scatterData }],
          xLabel: fields[0],
          yLabel: fields[1],
          zLabel: 'Count'
        };
      }
    }
    
    // For time slots, create a scatter plot of time vs count
    if (data.timeSlots && data.timeSlots.length > 0) {
      const scatterData = data.timeSlots.map((slot: any) => ({
        x: new Date(slot.time).getTime(),
        y: slot.count,
        z: 10,
        name: new Date(slot.time).toLocaleString(),
      }));
      
      return {
        series: [{ name: 'Time Slots', data: scatterData }],
        xLabel: 'Time',
        yLabel: 'Count',
        zLabel: ''
      };
    }

    return { series: [], xLabel: '', yLabel: '' };
  };

  const { series, xLabel, yLabel, zLabel } = processData();

  if (series.length === 0 || series[0].data.length === 0) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-sm text-muted-foreground">
          No suitable data available for scatter plot visualization
        </div>
      </div>
    );
  }

  const colors = [
    '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
    '#06b6d4', '#f97316', '#84cc16', '#ec4899', '#6366f1'
  ];

  // Format x-axis ticks for time values
  const formatXAxis = (value: any) => {
    if (xLabel === 'Time' && typeof value === 'number') {
      const date = new Date(value);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return value;
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1">
        <ResponsiveContainer width="100%" height="100%">
          <ScatterChart
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
              dataKey="x" 
              name={xLabel}
              type={xLabel === 'Time' ? 'number' : 'category'}
              tickFormatter={formatXAxis}
              label={{ value: xLabel, position: 'insideBottom', offset: -5 }}
              tick={{ fontSize: 12 }}
            />
            <YAxis 
              dataKey="y" 
              name={yLabel}
              label={{ value: yLabel, angle: -90, position: 'insideLeft' }}
              tick={{ fontSize: 12 }}
            />
            <ZAxis dataKey="z" range={[40, 160]} name={zLabel} />
            <Tooltip 
              cursor={{ strokeDasharray: '3 3' }}
              formatter={(value, name) => [value, name === 'z' ? zLabel : (name === 'y' ? yLabel : xLabel)]}
              labelFormatter={() => 'Data Point'}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            {series.map((s, index) => (
              <Scatter 
                key={index}
                name={s.name} 
                data={s.data} 
                fill={colors[index % colors.length]} 
              />
            ))}
          </ScatterChart>
        </ResponsiveContainer>
      </div>

      {/* Summary */}
      <div className="text-xs text-muted-foreground text-center pt-2 border-t">
        {series[0].data.length} data points • {series.length} series
      </div>
    </div>
  );
};

export default ScatterPlotWidget;