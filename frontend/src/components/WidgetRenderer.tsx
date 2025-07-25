import React, { useState, useEffect, useRef } from 'react';
import { DashboardWidget } from '@/api/dashboard';
import { logSearchApi } from '@/api/logSearch';
import { BarChart3, PieChart, Table, Activity, AlertCircle, LineChart, TrendingUp, ScatterChart, RefreshCw, Grid } from 'lucide-react';
import { getWidgetUpdateClient } from '@/utils/sseClient';
import { useSwipeable } from 'react-swipeable';

// Widget-specific components
import BarChartWidget from './widgets/BarChartWidget';
import PieChartWidget from './widgets/PieChartWidget';
import TableWidget from './widgets/TableWidget';
import MetricWidget from './widgets/MetricWidget';
import LineChartWidget from './widgets/LineChartWidget';
import AreaChartWidget from './widgets/AreaChartWidget';
import ScatterPlotWidget from './widgets/ScatterPlotWidget';
import HeatmapWidget from './widgets/HeatmapWidget';

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
  
  // State to track swipe refresh gesture
  const [refreshing, setRefreshing] = useState(false);

  // Reference to the SSE client
  const sseClientRef = useRef<any>(null);
  
  // Swipe handlers for widget interactions
  const widgetSwipeHandlers = useSwipeable({
    onSwipedDown: () => {
      // Pull-to-refresh functionality
      if (!widgetData.loading) {
        setRefreshing(true);
        loadWidgetData().finally(() => {
          setTimeout(() => setRefreshing(false), 1000);
        });
      }
    },
    trackMouse: false,
    swipeDuration: 500,
    preventScrollOnSwipe: false, // Allow scrolling for widgets with scrollable content
    delta: 50, // Minimum swipe distance to trigger the action
  });

  useEffect(() => {
    // Initial data load
    loadWidgetData();
    
    // Set up SSE for real-time updates
    if (widget.dashboardId && widget.id) {
      const sseClient = getWidgetUpdateClient(widget.dashboardId, widget.id);
      sseClientRef.current = sseClient;
      
      // Listen for widget updates
      sseClient.on('widgetUpdate', (data) => {
        console.log(`Received real-time update for widget ${widget.id}`, data);
        setWidgetData({
          loading: false,
          error: null,
          data: data
        });
      });
      
      // Listen for initial data
      sseClient.on('initialData', (data) => {
        console.log(`Received initial data for widget ${widget.id}`, data);
        setWidgetData({
          loading: false,
          error: null,
          data: data
        });
      });
      
      // Connect to the SSE endpoint
      sseClient.connect();
      
      console.log(`Established SSE connection for widget ${widget.id}`);
    }
    
    // Clean up SSE connection when component unmounts
    return () => {
      if (sseClientRef.current) {
        sseClientRef.current.close();
        sseClientRef.current = null;
        console.log(`Closed SSE connection for widget ${widget.id}`);
      }
    };
  }, [widget.query, widget.id, widget.dashboardId]);

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
      case 'line':
        return <LineChart className="h-4 w-4" />;
      case 'area':
        return <TrendingUp className="h-4 w-4" />;
      case 'scatter':
        return <ScatterChart className="h-4 w-4" />;
      case 'heatmap':
        return <Grid className="h-4 w-4" />;
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
      case 'line':
        return <LineChartWidget data={widgetData.data} widget={widget} />;
      case 'area':
        return <AreaChartWidget data={widgetData.data} widget={widget} />;
      case 'scatter':
        return <ScatterPlotWidget data={widgetData.data} widget={widget} />;
      case 'heatmap':
        return <HeatmapWidget data={widgetData.data} widget={widget} />;
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
    <div className="widget-renderer h-full flex flex-col" {...widgetSwipeHandlers}>
      {/* Pull-to-refresh indicator */}
      {refreshing && (
        <div className="absolute inset-0 bg-black bg-opacity-10 flex items-center justify-center z-10">
          <div className="bg-white dark:bg-gray-800 rounded-full p-2 shadow-lg">
            <RefreshCw className="h-6 w-6 text-blue-500 animate-spin" />
          </div>
        </div>
      )}
      
      {/* Widget Header */}
      <div className="widget-header flex items-center justify-between p-3 border-b bg-gray-50">
        <div className="flex items-center space-x-2">
          {getWidgetIcon(widget.type)}
          <h3 className="font-medium text-sm">{widget.title}</h3>
        </div>
        <div className="flex items-center space-x-2">
          {widgetData.loading && !refreshing && (
            <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
          )}
          <button
            onClick={loadWidgetData}
            className="p-1 rounded-full hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
            title="Refresh widget (or swipe down to refresh)"
            aria-label="Refresh widget"
          >
            <RefreshCw className="h-4 w-4 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200" />
          </button>
        </div>
      </div>

      {/* Widget Content */}
      <div className="widget-content flex-1 p-3 relative">
        {/* Swipe hint - shown briefly on first render */}
        <div className="absolute top-0 left-0 right-0 text-center text-xs text-muted-foreground py-1 opacity-70">
          Swipe down to refresh
        </div>
        {renderWidget()}
      </div>
    </div>
  );
};

export default WidgetRenderer;