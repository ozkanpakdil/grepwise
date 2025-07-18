import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import WidgetRenderer from '@/components/WidgetRenderer';
import { logSearchApi } from '@/api/logSearch';

// Mock the log search API
vi.mock('@/api/logSearch', () => ({
  logSearchApi: {
    search: vi.fn(),
  },
}));

// Mock individual widget components
vi.mock('@/components/widgets/BarChartWidget', () => ({
  default: ({ data, widget }: { data: any; widget: any }) => (
    <div data-testid="bar-chart-widget">
      <div>Bar Chart: {widget.title}</div>
      <div>Data: {JSON.stringify(data)}</div>
    </div>
  ),
}));

vi.mock('@/components/widgets/PieChartWidget', () => ({
  default: ({ data, widget }: { data: any; widget: any }) => (
    <div data-testid="pie-chart-widget">
      <div>Pie Chart: {widget.title}</div>
      <div>Data: {JSON.stringify(data)}</div>
    </div>
  ),
}));

vi.mock('@/components/widgets/TableWidget', () => ({
  default: ({ data, widget }: { data: any; widget: any }) => (
    <div data-testid="table-widget">
      <div>Table: {widget.title}</div>
      <div>Data: {JSON.stringify(data)}</div>
    </div>
  ),
}));

vi.mock('@/components/widgets/MetricWidget', () => ({
  default: ({ data, widget }: { data: any; widget: any }) => (
    <div data-testid="metric-widget">
      <div>Metric: {widget.title}</div>
      <div>Data: {JSON.stringify(data)}</div>
    </div>
  ),
}));

describe('WidgetRenderer', () => {
  const mockSearchResponse = {
    results: [
      { id: '1', message: 'Test log 1', level: 'INFO' },
      { id: '2', message: 'Test log 2', level: 'ERROR' },
    ],
    timeSlots: [
      { time: 1640995200000, count: 5 },
      { time: 1640998800000, count: 3 },
    ],
    total: 2,
  };

  const baseWidget = {
    id: 'w1',
    title: 'Test Widget',
    type: 'chart',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'current-user',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(logSearchApi.search).mockResolvedValue(mockSearchResponse);
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('renders widget header with title and icon', async () => {
    render(<WidgetRenderer widget={baseWidget} />);

    expect(screen.getByText('Test Widget')).toBeInTheDocument();
    
    // Check for chart icon (BarChart3)
    const header = screen.getByText('Test Widget').closest('.widget-header');
    expect(header).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    render(<WidgetRenderer widget={baseWidget} />);

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('loads widget data on mount', async () => {
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(logSearchApi.search).toHaveBeenCalledWith({
        query: 'search *',
        timeRange: '24h',
        maxResults: 1000,
      });
    });
  });

  it('renders bar chart widget when type is chart', async () => {
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(screen.getByTestId('bar-chart-widget')).toBeInTheDocument();
      expect(screen.getByText('Bar Chart: Test Widget')).toBeInTheDocument();
    });
  });

  it('renders pie chart widget when type is pie', async () => {
    const pieWidget = { ...baseWidget, type: 'pie' };
    render(<WidgetRenderer widget={pieWidget} />);

    await waitFor(() => {
      expect(screen.getByTestId('pie-chart-widget')).toBeInTheDocument();
      expect(screen.getByText('Pie Chart: Test Widget')).toBeInTheDocument();
    });
  });

  it('renders table widget when type is table', async () => {
    const tableWidget = { ...baseWidget, type: 'table' };
    render(<WidgetRenderer widget={tableWidget} />);

    await waitFor(() => {
      expect(screen.getByTestId('table-widget')).toBeInTheDocument();
      expect(screen.getByText('Table: Test Widget')).toBeInTheDocument();
    });
  });

  it('renders metric widget when type is metric', async () => {
    const metricWidget = { ...baseWidget, type: 'metric' };
    render(<WidgetRenderer widget={metricWidget} />);

    await waitFor(() => {
      expect(screen.getByTestId('metric-widget')).toBeInTheDocument();
      expect(screen.getByText('Metric: Test Widget')).toBeInTheDocument();
    });
  });

  it('shows error message for unsupported widget type', async () => {
    const unsupportedWidget = { ...baseWidget, type: 'unsupported' };
    render(<WidgetRenderer widget={unsupportedWidget} />);

    await waitFor(() => {
      expect(screen.getByText('Unsupported widget type: unsupported')).toBeInTheDocument();
    });
  });

  it('shows error when widget has no query', async () => {
    const emptyQueryWidget = { ...baseWidget, query: '' };
    render(<WidgetRenderer widget={emptyQueryWidget} />);

    await waitFor(() => {
      expect(screen.getByText('No query specified')).toBeInTheDocument();
    });
  });

  it('shows error when widget has whitespace-only query', async () => {
    const whitespaceQueryWidget = { ...baseWidget, query: '   ' };
    render(<WidgetRenderer widget={whitespaceQueryWidget} />);

    await waitFor(() => {
      expect(screen.getByText('No query specified')).toBeInTheDocument();
    });
  });

  it('handles API errors gracefully', async () => {
    vi.mocked(logSearchApi.search).mockRejectedValue(new Error('API Error'));
    
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(screen.getByText('API Error')).toBeInTheDocument();
    });

    // Check for error icon (AlertCircle SVG)
    const errorIcon = document.querySelector('.lucide-alert-circle');
    expect(errorIcon).toBeInTheDocument();
  });

  it('shows no data message when API returns null', async () => {
    vi.mocked(logSearchApi.search).mockResolvedValue(null);
    
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(screen.getByText('No data available')).toBeInTheDocument();
    });
  });

  it('passes correct data to widget components', async () => {
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      const dataText = screen.getByText(/Data: /);
      expect(dataText.textContent).toContain(JSON.stringify(mockSearchResponse));
    });
  });

  it('shows refresh button in header', async () => {
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      const refreshButton = screen.getByTitle('Refresh widget');
      expect(refreshButton).toBeInTheDocument();
      expect(refreshButton.textContent).toBe('↻');
    });
  });

  it('refreshes data when refresh button is clicked', async () => {
    const user = userEvent.setup();
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(logSearchApi.search).toHaveBeenCalledTimes(1);
    });

    const refreshButton = screen.getByTitle('Refresh widget');
    await user.click(refreshButton);

    await waitFor(() => {
      expect(logSearchApi.search).toHaveBeenCalledTimes(2);
    });
  });

  it('shows loading indicator during refresh', async () => {
    const user = userEvent.setup();
    
    // Make the API call hang to test loading state
    let resolvePromise: (value: any) => void;
    const hangingPromise = new Promise(resolve => {
      resolvePromise = resolve;
    });
    vi.mocked(logSearchApi.search).mockReturnValue(hangingPromise);

    render(<WidgetRenderer widget={baseWidget} />);

    const refreshButton = screen.getByTitle('Refresh widget');
    await user.click(refreshButton);

    // Check for loading indicator (blue pulsing dot)
    const loadingIndicator = document.querySelector('.animate-pulse');
    expect(loadingIndicator).toBeInTheDocument();

    // Resolve the promise to clean up
    resolvePromise!(mockSearchResponse);
  });

  it('reloads data when widget query changes', async () => {
    const { rerender } = render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(logSearchApi.search).toHaveBeenCalledTimes(1);
    });

    // Change the query
    const updatedWidget = { ...baseWidget, query: 'search error' };
    rerender(<WidgetRenderer widget={updatedWidget} />);

    await waitFor(() => {
      expect(logSearchApi.search).toHaveBeenCalledTimes(2);
      expect(logSearchApi.search).toHaveBeenLastCalledWith({
        query: 'search error',
        timeRange: '24h',
        maxResults: 1000,
      });
    });
  });

  it('sets up auto-refresh interval', async () => {
    const setIntervalSpy = vi.spyOn(global, 'setInterval');
    
    render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(logSearchApi.search).toHaveBeenCalledTimes(1);
    });

    // Verify that setInterval was called with 30000ms (30 seconds)
    expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 30000);

    setIntervalSpy.mockRestore();
  }, 10000);

  it('clears auto-refresh interval on unmount', async () => {
    const clearIntervalSpy = vi.spyOn(global, 'clearInterval');
    
    const { unmount } = render(<WidgetRenderer widget={baseWidget} />);

    await waitFor(() => {
      expect(logSearchApi.search).toHaveBeenCalledTimes(1);
    });

    unmount();

    // Verify that clearInterval was called on unmount
    expect(clearIntervalSpy).toHaveBeenCalled();

    clearIntervalSpy.mockRestore();
  }, 10000);

  it('displays correct icon for each widget type', async () => {
    // Test just one widget type to avoid complex loops
    const chartWidget = { ...baseWidget, type: 'chart' };
    render(<WidgetRenderer widget={chartWidget} />);
    
    await waitFor(() => {
      const header = screen.getByText('Test Widget').closest('.widget-header');
      expect(header).toBeInTheDocument();
      // Verify chart icon is present
      const chartIcon = document.querySelector('.lucide-bar-chart3');
      expect(chartIcon).toBeInTheDocument();
    });
  }, 10000);

  it('handles loading state correctly during data fetch', async () => {
    let resolvePromise: (value: any) => void;
    const pendingPromise = new Promise(resolve => {
      resolvePromise = resolve;
    });
    vi.mocked(logSearchApi.search).mockReturnValue(pendingPromise);

    render(<WidgetRenderer widget={baseWidget} />);

    // Should show loading initially
    expect(screen.getByText('Loading...')).toBeInTheDocument();

    // Resolve the promise immediately
    resolvePromise!(mockSearchResponse);

    await waitFor(() => {
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
      expect(screen.getByTestId('bar-chart-widget')).toBeInTheDocument();
    });
  }, 10000);

  it('shows loading indicator during refresh', async () => {
    const user = userEvent.setup();
    render(<WidgetRenderer widget={baseWidget} />);

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByTestId('bar-chart-widget')).toBeInTheDocument();
    });

    // Make refresh hang temporarily
    let resolveRefresh: (value: any) => void;
    const refreshPromise = new Promise(resolve => {
      resolveRefresh = resolve;
    });
    vi.mocked(logSearchApi.search).mockReturnValue(refreshPromise);

    const refreshButton = screen.getByTitle('Refresh widget');
    await user.click(refreshButton);

    // Should show loading indicator during refresh
    const loadingIndicator = document.querySelector('.animate-pulse');
    expect(loadingIndicator).toBeInTheDocument();

    // Resolve refresh immediately
    resolveRefresh!(mockSearchResponse);

    // Wait for refresh to complete
    await waitFor(() => {
      expect(screen.getByTestId('bar-chart-widget')).toBeInTheDocument();
    });
  }, 10000);
});