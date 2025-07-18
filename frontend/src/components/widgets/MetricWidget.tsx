import React from 'react';
import { DashboardWidget } from '@/api/dashboard';
import { TrendingUp, TrendingDown, Minus, AlertTriangle } from 'lucide-react';

interface MetricWidgetProps {
  data: any;
  widget: DashboardWidget;
}

interface MetricData {
  value: number;
  label: string;
  unit?: string;
  trend: 'up' | 'down' | 'neutral';
  trendValue?: number;
  status: 'normal' | 'warning' | 'critical';
}

const MetricWidget: React.FC<MetricWidgetProps> = ({ data, widget: _widget }) => {
  
  // Process the data for metric display
  const processData = (): MetricData => {
    let metricValue = 0;
    let label = 'Metric';
    let unit = '';
    let trend: 'up' | 'down' | 'neutral' = 'neutral';
    let trendValue = 0;
    let status: 'normal' | 'warning' | 'critical' = 'normal';

    if (!data) {
      return { value: 0, label: 'No Data', trend: 'neutral', status: 'warning' };
    }

    // For time-based data, show total count
    if (data.timeSlots && data.timeSlots.length > 0) {
      metricValue = data.timeSlots.reduce((sum: number, slot: any) => sum + slot.count, 0);
      label = 'Total Events';
      
      // Calculate trend from first half vs second half
      const midPoint = Math.floor(data.timeSlots.length / 2);
      const firstHalf = data.timeSlots.slice(0, midPoint).reduce((sum: number, slot: any) => sum + slot.count, 0);
      const secondHalf = data.timeSlots.slice(midPoint).reduce((sum: number, slot: any) => sum + slot.count, 0);
      
      if (firstHalf > 0) {
        const change = ((secondHalf - firstHalf) / firstHalf) * 100;
        trendValue = Math.abs(change);
        trend = change > 5 ? 'up' : change < -5 ? 'down' : 'neutral';
      }
    }
    
    // For stats results, try to find a single numeric value
    else if (data.results && data.results.length > 0) {
      const result = data.results[0];
      
      // Look for common metric fields
      const metricFields = ['count', 'total', 'sum', 'avg', 'average', 'value', 'score'];
      let foundField = null;
      
      for (const field of metricFields) {
        if (result[field] !== undefined && typeof result[field] === 'number') {
          foundField = field;
          metricValue = result[field];
          break;
        }
      }
      
      // If no specific metric field found, use the first numeric field
      if (!foundField) {
        for (const [key, value] of Object.entries(result)) {
          if (typeof value === 'number' && key !== 'timestamp' && key !== '_time') {
            metricValue = value;
            foundField = key;
            break;
          }
        }
      }
      
      if (foundField) {
        label = foundField.charAt(0).toUpperCase() + foundField.slice(1).replace(/_/g, ' ');
        
        // Determine unit based on field name
        if (foundField.includes('count') || foundField.includes('total')) {
          unit = '';
        } else if (foundField.includes('percent') || foundField.includes('rate')) {
          unit = '%';
        } else if (foundField.includes('time') || foundField.includes('duration')) {
          unit = 'ms';
        } else if (foundField.includes('size') || foundField.includes('bytes')) {
          unit = 'B';
        }
      }
    }
    
    // Determine status based on value and field type
    if (label.toLowerCase().includes('error') || label.toLowerCase().includes('fail')) {
      status = metricValue > 10 ? 'critical' : metricValue > 0 ? 'warning' : 'normal';
    } else if (label.toLowerCase().includes('warn')) {
      status = metricValue > 5 ? 'warning' : 'normal';
    } else {
      // For general metrics, assume higher is better unless it's an error-related metric
      status = 'normal';
    }

    return {
      value: metricValue,
      label,
      unit,
      trend,
      trendValue,
      status,
    };
  };

  const metric = processData();

  // Format the metric value for display
  const formatValue = (value: number): string => {
    if (value >= 1000000) {
      return (value / 1000000).toFixed(1) + 'M';
    } else if (value >= 1000) {
      return (value / 1000).toFixed(1) + 'K';
    } else if (value % 1 === 0) {
      return value.toString();
    } else {
      return value.toFixed(2);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'critical':
        return 'text-red-600';
      case 'warning':
        return 'text-yellow-600';
      default:
        return 'text-green-600';
    }
  };

  const getStatusBgColor = (status: string) => {
    switch (status) {
      case 'critical':
        return 'bg-red-50 border-red-200';
      case 'warning':
        return 'bg-yellow-50 border-yellow-200';
      default:
        return 'bg-green-50 border-green-200';
    }
  };

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'up':
        return <TrendingUp className="h-4 w-4 text-green-600" />;
      case 'down':
        return <TrendingDown className="h-4 w-4 text-red-600" />;
      default:
        return <Minus className="h-4 w-4 text-gray-400" />;
    }
  };

  return (
    <div className={`h-full flex flex-col justify-center items-center p-4 rounded-lg border-2 ${getStatusBgColor(metric.status)}`}>
      {/* Status indicator */}
      {metric.status !== 'normal' && (
        <div className="mb-2">
          <AlertTriangle className={`h-5 w-5 ${getStatusColor(metric.status)}`} />
        </div>
      )}

      {/* Main metric value */}
      <div className="text-center">
        <div className={`text-3xl font-bold ${getStatusColor(metric.status)}`}>
          {formatValue(metric.value)}
          {metric.unit && <span className="text-lg ml-1">{metric.unit}</span>}
        </div>
        
        <div className="text-sm text-muted-foreground mt-1">
          {metric.label}
        </div>
      </div>

      {/* Trend indicator */}
      {metric.trend !== 'neutral' && metric.trendValue && metric.trendValue > 0 && (
        <div className="flex items-center space-x-1 mt-3 text-xs">
          {getTrendIcon(metric.trend)}
          <span className={metric.trend === 'up' ? 'text-green-600' : 'text-red-600'}>
            {metric.trendValue.toFixed(1)}%
          </span>
          <span className="text-muted-foreground">vs previous period</span>
        </div>
      )}

      {/* Simple gauge visualization for percentage values */}
      {metric.unit === '%' && (
        <div className="w-full mt-4">
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className={`h-2 rounded-full transition-all duration-300 ${
                metric.value > 80 ? 'bg-red-500' : 
                metric.value > 60 ? 'bg-yellow-500' : 'bg-green-500'
              }`}
              style={{ width: `${Math.min(metric.value, 100)}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default MetricWidget;