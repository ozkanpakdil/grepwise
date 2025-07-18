import React, { useState } from 'react';
import { DashboardWidget } from '@/api/dashboard';

interface PieChartWidgetProps {
  data: any;
  widget: DashboardWidget;
}

interface PieSlice {
  label: string;
  value: number;
  percentage: number;
  color: string;
  startAngle: number;
  endAngle: number;
}

const PieChartWidget: React.FC<PieChartWidgetProps> = ({ data, widget: _widget }) => {
  const [hoveredSlice, setHoveredSlice] = useState<PieSlice | null>(null);

  // Color palette for pie slices
  const colors = [
    '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
    '#06b6d4', '#f97316', '#84cc16', '#ec4899', '#6366f1'
  ];

  // Process the data for pie chart visualization
  const processData = (): PieSlice[] => {
    let chartData: { label: string; value: number }[] = [];

    // For stats queries, extract key-value pairs
    if (data && data.results && data.results.length > 0) {
      const result = data.results[0];
      
      // Look for common field patterns in stats results
      for (const [key, value] of Object.entries(result)) {
        if (key !== 'timestamp' && key !== '_time' && typeof value === 'number' && value > 0) {
          chartData.push({
            label: key,
            value: value,
          });
        }
      }
    }

    // For time-based data, aggregate by some criteria
    if (data && data.timeSlots && data.timeSlots.length > 0) {
      // Group time slots into periods (e.g., morning, afternoon, evening, night)
      const periods = {
        'Morning (6-12)': 0,
        'Afternoon (12-18)': 0,
        'Evening (18-24)': 0,
        'Night (0-6)': 0,
      };

      data.timeSlots.forEach((slot: any) => {
        const hour = new Date(slot.time).getHours();
        if (hour >= 6 && hour < 12) periods['Morning (6-12)'] += slot.count;
        else if (hour >= 12 && hour < 18) periods['Afternoon (12-18)'] += slot.count;
        else if (hour >= 18 && hour < 24) periods['Evening (18-24)'] += slot.count;
        else periods['Night (0-6)'] += slot.count;
      });

      chartData = Object.entries(periods)
        .filter(([_, value]) => value > 0)
        .map(([label, value]) => ({ label, value }));
    }

    if (chartData.length === 0) {
      return [];
    }

    // Sort by value and take top 8 slices
    chartData = chartData
      .sort((a, b) => b.value - a.value)
      .slice(0, 8);

    const total = chartData.reduce((sum, item) => sum + item.value, 0);
    let currentAngle = 0;

    return chartData.map((item, index) => {
      const percentage = (item.value / total) * 100;
      const sliceAngle = (item.value / total) * 360;
      const startAngle = currentAngle;
      const endAngle = currentAngle + sliceAngle;
      
      currentAngle += sliceAngle;

      return {
        label: item.label,
        value: item.value,
        percentage,
        color: colors[index % colors.length],
        startAngle,
        endAngle,
      };
    });
  };

  const pieData = processData();

  // Create SVG path for pie slice
  const createPath = (slice: PieSlice, radius: number, centerX: number, centerY: number): string => {
    const startAngleRad = (slice.startAngle - 90) * (Math.PI / 180);
    const endAngleRad = (slice.endAngle - 90) * (Math.PI / 180);
    
    const x1 = centerX + radius * Math.cos(startAngleRad);
    const y1 = centerY + radius * Math.sin(startAngleRad);
    const x2 = centerX + radius * Math.cos(endAngleRad);
    const y2 = centerY + radius * Math.sin(endAngleRad);
    
    const largeArcFlag = slice.endAngle - slice.startAngle > 180 ? 1 : 0;
    
    return [
      `M ${centerX} ${centerY}`,
      `L ${x1} ${y1}`,
      `A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2}`,
      'Z'
    ].join(' ');
  };

  if (pieData.length === 0) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-sm text-muted-foreground">
          No data available for pie chart visualization
        </div>
      </div>
    );
  }

  const svgSize = 200;
  const radius = 80;
  const centerX = svgSize / 2;
  const centerY = svgSize / 2;

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 flex items-center justify-center">
        <div className="flex items-center space-x-4">
          {/* Pie Chart SVG */}
          <div className="relative">
            <svg width={svgSize} height={svgSize} className="drop-shadow-sm">
              {pieData.map((slice, index) => (
                <path
                  key={index}
                  d={createPath(slice, radius, centerX, centerY)}
                  fill={slice.color}
                  stroke="white"
                  strokeWidth="2"
                  className="cursor-pointer transition-opacity hover:opacity-80"
                  onMouseEnter={() => setHoveredSlice(slice)}
                  onMouseLeave={() => setHoveredSlice(null)}
                />
              ))}
            </svg>
            
            {/* Tooltip */}
            {hoveredSlice && (
              <div className="absolute top-0 left-full ml-2 bg-black text-white text-xs rounded px-2 py-1 z-10 whitespace-nowrap">
                <div className="font-medium">{hoveredSlice.label}</div>
                <div>{hoveredSlice.value} ({hoveredSlice.percentage.toFixed(1)}%)</div>
              </div>
            )}
          </div>

          {/* Legend */}
          <div className="space-y-1 max-h-full overflow-y-auto">
            {pieData.map((slice, index) => (
              <div
                key={index}
                className="flex items-center space-x-2 text-xs cursor-pointer hover:bg-gray-50 p-1 rounded"
                onMouseEnter={() => setHoveredSlice(slice)}
                onMouseLeave={() => setHoveredSlice(null)}
              >
                <div
                  className="w-3 h-3 rounded-sm flex-shrink-0"
                  style={{ backgroundColor: slice.color }}
                />
                <div className="flex-1 min-w-0">
                  <div className="truncate font-medium">{slice.label}</div>
                  <div className="text-muted-foreground">
                    {slice.value} ({slice.percentage.toFixed(1)}%)
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Summary */}
      <div className="text-xs text-muted-foreground text-center pt-2 border-t">
        {pieData.length} categories • Total: {pieData.reduce((sum, slice) => sum + slice.value, 0)}
      </div>
    </div>
  );
};

export default PieChartWidget;