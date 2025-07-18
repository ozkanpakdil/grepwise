import React, { useState, useEffect } from 'react';
import { DashboardWidget } from '@/api/dashboard';
import { logSearchApi } from '@/api/logSearch';
import { BarChart3, PieChart, Table, Activity, AlertCircle } from 'lucide-react';

// Widget-specific components
import BarChartWidget from './widgets/BarChartWidget';
import PieChartWidget from './widgets/PieChartWidget';
import TableWidget from './widgets/TableWidget';
import MetricWidget from './widgets/MetricWidget';

interface WidgetRendererProps {
  widget: DashboardWidget;
}

interface WidgetData {
  loading: boolean;
  error: string | null;
  data: any;
}

const WidgetRenderer: React.FC<WidgetRendererProps> = ({ widget }) => {
  const [widgetData, setWidgetData] = useState<WidgetData>({
    loading: true,
    error: null,
    data: null,
  });

  useEffect(() => {
    loadWidgetData();
    
    // Set up auto-refresh for real-time updates (every 30 seconds)
    const interval = setInterval(loadWidgetData, 30000);
    
    return () => clearInterval(interval);
  }, [widget.query]);

  const loadWidgetData = async () => {
    if (!widget.query.trim()) {
      setWidgetData({
        loading: false,
        error: 'No query specified',
        data: null,
      });
      return;
    }

    try {
      setWidgetData(prev => ({ ...prev, loading: true, error: null }));
      
      // Execute the widget query
      const response = await logSearchApi.search({
        query: widget.query,
        timeRange: '24h', // Default time range, could be made configurable
        maxResults: 1000,
      });

      setWidgetData({
        loading: false,
        error: null,
        data: response,
      });
    } catch (error) {
      setWidgetData({
        loading: false,
        error: error instanceof Error ? error.message : 'Failed to load widget data',
        data: null,
      });
    }
  };

  const getWidgetIcon = (type: string) => {
    switch (type) {
      case 'chart':
        return <BarChart3 className="h-4 w-4" />;
      case 'pie':
        return <PieChart className="h-4 w-4" />;
      case 'table':
        return <Table className="h-4 w-4" />;
      case 'metric':
        return <Activity className="h-4 w-4" />;
      default:
        return <BarChart3 className="h-4 w-4" />;
    }
  };

  const renderWidget = () => {
    if (widgetData.loading) {
      return (
        <div className="flex items-center justify-center h-full">
          <div className="text-sm text-muted-foreground">Loading...</div>
        </div>
      );
    }

    if (widgetData.error) {
      return (
        <div className="flex items-center justify-center h-full">
          <div className="text-center">
            <AlertCircle className="h-8 w-8 text-red-500 mx-auto mb-2" />
            <div className="text-sm text-red-600">{widgetData.error}</div>
          </div>
        </div>
      );
    }

    if (!widgetData.data) {
      return (
        <div className="flex items-center justify-center h-full">
          <div className="text-sm text-muted-foreground">No data available</div>
        </div>
      );
    }

    // Render the appropriate widget type
    switch (widget.type) {
      case 'chart':
        return <BarChartWidget data={widgetData.data} widget={widget} />;
      case 'pie':
        return <PieChartWidget data={widgetData.data} widget={widget} />;
      case 'table':
        return <TableWidget data={widgetData.data} widget={widget} />;
      case 'metric':
        return <MetricWidget data={widgetData.data} widget={widget} />;
      default:
        return (
          <div className="flex items-center justify-center h-full">
            <div className="text-sm text-muted-foreground">
              Unsupported widget type: {widget.type}
            </div>
          </div>
        );
    }
  };

  return (
    <div className="widget-renderer h-full flex flex-col">
      {/* Widget Header */}
      <div className="widget-header flex items-center justify-between p-3 border-b bg-gray-50">
        <div className="flex items-center space-x-2">
          {getWidgetIcon(widget.type)}
          <h3 className="font-medium text-sm">{widget.title}</h3>
        </div>
        <div className="flex items-center space-x-1">
          {widgetData.loading && (
            <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
          )}
          <button
            onClick={loadWidgetData}
            className="text-xs text-muted-foreground hover:text-foreground"
            title="Refresh widget"
          >
            ↻
          </button>
        </div>
      </div>

      {/* Widget Content */}
      <div className="widget-content flex-1 p-3">
        {renderWidget()}
      </div>
    </div>
  );
};

export default WidgetRenderer;