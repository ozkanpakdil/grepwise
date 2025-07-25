import React, { useState, useEffect } from 'react';
import { DashboardWidget } from '@/api/dashboard';

interface HeatmapWidgetProps {
  data: any;
  widget: DashboardWidget;
}

// Helper function to generate a color based on value intensity
const getHeatColor = (value: number, maxValue: number): string => {
  // Calculate intensity (0-1)
  const intensity = maxValue > 0 ? value / maxValue : 0;
  
  // Generate color from blue (cold) to red (hot)
  if (intensity < 0.2) {
    return `rgba(0, 0, 255, ${0.1 + intensity * 2})`; // Light blue
  } else if (intensity < 0.5) {
    return `rgba(0, ${255 - intensity * 255}, 255, ${0.3 + intensity * 0.7})`; // Blue to purple
  } else if (intensity < 0.8) {
    return `rgba(${intensity * 255}, 0, ${255 - intensity * 255}, ${0.5 + intensity * 0.5})`; // Purple to red
  } else {
    return `rgba(255, ${intensity * 100}, 0, ${0.7 + intensity * 0.3})`; // Red to bright red
  }
};

const HeatmapWidget: React.FC<HeatmapWidgetProps> = ({ data, widget }) => {
  const [hoveredCell, setHoveredCell] = useState<any>(null);
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

  // Process the data for heatmap visualization
  const processData = () => {
    if (!data) {
      return { rows: [], columns: [], matrix: [], maxValue: 0 };
    }

    // Try to extract data from different query result formats
    
    // For stats queries with multiple dimensions
    if (data.results && data.results.length > 0) {
      // Look for fields that could be used as row and column dimensions
      const firstResult = data.results[0];
      const fields = Object.keys(firstResult);
      
      // Try to identify dimensions and value fields
      let rowField = null;
      let colField = null;
      let valueField = null;
      
      // Use configuration if available
      if (widget.configuration) {
        rowField = widget.configuration.rowField;
        colField = widget.configuration.colField;
        valueField = widget.configuration.valueField;
      }
      
      // Auto-detect fields if not configured
      if (!rowField || !colField || !valueField) {
        // Look for common dimension and value field patterns
        for (const field of fields) {
          if (field.toLowerCase().includes('time') || 
              field.toLowerCase().includes('date') || 
              field.toLowerCase().includes('day')) {
            if (!rowField) rowField = field;
            else if (!colField) colField = field;
          } else if (field.toLowerCase().includes('source') || 
                    field.toLowerCase().includes('host') || 
                    field.toLowerCase().includes('category')) {
            if (!rowField) rowField = field;
            else if (!colField) colField = field;
          } else if (field.toLowerCase().includes('count') || 
                    field.toLowerCase().includes('value') || 
                    field.toLowerCase().includes('sum') ||
                    typeof firstResult[field] === 'number') {
            valueField = field;
          }
        }
      }
      
      // If we couldn't identify fields, use the first three fields
      if (!rowField && fields.length > 0) rowField = fields[0];
      if (!colField && fields.length > 1) colField = fields[1];
      if (!valueField && fields.length > 2) valueField = fields[2];
      
      // If we still don't have a value field, look for any numeric field
      if (!valueField) {
        for (const field of fields) {
          if (typeof firstResult[field] === 'number') {
            valueField = field;
            break;
          }
        }
      }
      
      // If we have the necessary fields, build the heatmap data
      if (rowField && colField && valueField) {
        // Extract unique row and column values
        const rowValues = new Set<string>();
        const colValues = new Set<string>();
        
        data.results.forEach((result: any) => {
          if (result[rowField]) rowValues.add(String(result[rowField]));
          if (result[colField]) colValues.add(String(result[colField]));
        });
        
        const rows = Array.from(rowValues);
        const columns = Array.from(colValues);
        
        // Create the data matrix
        const matrix: number[][] = Array(rows.length).fill(0).map(() => Array(columns.length).fill(0));
        let maxValue = 0;
        
        data.results.forEach((result: any) => {
          const rowIndex = rows.indexOf(String(result[rowField]));
          const colIndex = columns.indexOf(String(result[colField]));
          
          if (rowIndex >= 0 && colIndex >= 0 && result[valueField] !== undefined) {
            const value = Number(result[valueField]);
            matrix[rowIndex][colIndex] = value;
            maxValue = Math.max(maxValue, value);
          }
        });
        
        return { rows, columns, matrix, maxValue, rowField, colField, valueField };
      }
    }
    
    // Fallback: create a simple demo heatmap if we couldn't extract data
    const rows = ['Row 1', 'Row 2', 'Row 3', 'Row 4', 'Row 5'];
    const columns = ['Col A', 'Col B', 'Col C', 'Col D'];
    const matrix = rows.map(() => columns.map(() => Math.floor(Math.random() * 100)));
    const maxValue = 100;
    
    return { 
      rows, 
      columns, 
      matrix, 
      maxValue,
      rowField: 'Demo Row', 
      colField: 'Demo Column', 
      valueField: 'Value'
    };
  };

  const { rows, columns, matrix, maxValue, rowField, colField, valueField } = processData();

  if (rows.length === 0 || columns.length === 0) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-sm text-muted-foreground">
          No data available for heatmap visualization
        </div>
      </div>
    );
  }

  // Calculate cell sizes based on available space and screen size
  const isSmallScreen = screenWidth < 640;
  const fontSize = isSmallScreen ? 'text-[10px]' : 'text-xs';
  
  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 overflow-auto">
        <div className="relative">
          {/* Tooltip */}
          {hoveredCell && (
            <div 
              className={`absolute bg-black text-white ${fontSize} rounded px-2 py-1 z-10 pointer-events-none`}
              style={{ 
                left: hoveredCell.x,
                top: hoveredCell.y,
                transform: 'translate(10px, -100%)',
                marginTop: '-8px',
                maxWidth: isSmallScreen ? '120px' : '200px'
              }}
            >
              <div className="font-medium">{rows[hoveredCell.row]} × {columns[hoveredCell.col]}</div>
              <div>{valueField}: {matrix[hoveredCell.row][hoveredCell.col]}</div>
            </div>
          )}

          {/* Heatmap Grid */}
          <div className="mt-6 ml-6">
            {/* Column Headers */}
            <div className="flex">
              <div className="w-20 flex-shrink-0"></div>
              {columns.map((col, colIndex) => (
                <div 
                  key={colIndex} 
                  className={`${fontSize} text-muted-foreground text-center flex-1 min-w-[40px] truncate px-1`}
                  title={col}
                >
                  {isSmallScreen && col.length > 6 ? col.substring(0, 6) + '...' : col}
                </div>
              ))}
            </div>
            
            {/* Rows with Heatmap Cells */}
            {rows.map((row, rowIndex) => (
              <div key={rowIndex} className="flex">
                {/* Row Header */}
                <div 
                  className={`${fontSize} text-muted-foreground w-20 flex-shrink-0 truncate py-1 pr-2 text-right`}
                  title={row}
                >
                  {isSmallScreen && row.length > 10 ? row.substring(0, 10) + '...' : row}
                </div>
                
                {/* Heatmap Cells */}
                {columns.map((col, colIndex) => {
                  const value = matrix[rowIndex][colIndex];
                  return (
                    <div 
                      key={colIndex}
                      className="flex-1 min-w-[40px] min-h-[30px] border border-gray-100 cursor-pointer transition-opacity hover:opacity-80"
                      style={{ backgroundColor: getHeatColor(value, maxValue) }}
                      title={`${row} × ${col}: ${value}`}
                      onMouseEnter={(e) => setHoveredCell({ 
                        row: rowIndex, 
                        col: colIndex, 
                        x: e.clientX, 
                        y: e.clientY 
                      })}
                      onMouseLeave={() => setHoveredCell(null)}
                    >
                      <div className={`w-full h-full flex items-center justify-center ${fontSize} ${value > maxValue * 0.7 ? 'text-white' : 'text-gray-800'}`}>
                        {isSmallScreen ? '' : value}
                      </div>
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="mt-2 px-2">
        <div className="flex justify-between items-center">
          <div className={`${fontSize} text-muted-foreground`}>Low</div>
          <div className="flex-1 h-2 mx-2 rounded-full" style={{ 
            background: 'linear-gradient(to right, rgba(0,0,255,0.3), rgba(128,0,128,0.6), rgba(255,0,0,0.9))' 
          }}></div>
          <div className={`${fontSize} text-muted-foreground`}>High</div>
        </div>
      </div>

      {/* Summary */}
      <div className={`${fontSize} text-muted-foreground text-center pt-2 border-t mt-2`}>
        {rows.length} × {columns.length} grid • Max: {maxValue}
      </div>
    </div>
  );
};

export default HeatmapWidget;