import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import LineChartWidget from '@/components/widgets/LineChartWidget';

// Mock Recharts components
vi.mock('recharts', () => ({
  LineChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="recharts-line-chart">{children}</div>
  ),
  Line: ({ dataKey, name }: { dataKey: string; name: string }) => (
    <div data-testid={`recharts-line-${dataKey}`}>Line: {name}</div>
  ),
  XAxis: ({ dataKey }: { dataKey: string }) => (
    <div data-testid="recharts-xaxis">XAxis: {dataKey}</div>
  ),
  YAxis: () => <div data-testid="recharts-yaxis">YAxis</div>,
  CartesianGrid: () => <div data-testid="recharts-grid">Grid</div>,
  Tooltip: () => <div data-testid="recharts-tooltip">Tooltip</div>,
  Legend: () => <div data-testid="recharts-legend">Legend</div>,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="recharts-container">{children}</div>
  ),
}));

describe('LineChartWidget', () => {
  const mockWidget = {
    id: 'w1',
    title: 'Test Line Chart',
    type: 'line',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'current-user',
  };

  it('renders no data message when data is empty', () => {
    render(<LineChartWidget data={null} widget={mockWidget} />);
    expect(screen.getByText('No data available for line chart visualization')).toBeInTheDocument();
  });

  it('renders line chart with time slots data', () => {
    const mockData = {
      timeSlots: [
        { time: 1640995200000, count: 5 },
        { time: 1640998800000, count: 3 },
      ],
    };

    render(<LineChartWidget data={mockData} widget={mockWidget} />);

    // Check for Recharts components
    expect(screen.getByTestId('recharts-container')).toBeInTheDocument();
    expect(screen.getByTestId('recharts-line-chart')).toBeInTheDocument();
    expect(screen.getByTestId('recharts-xaxis')).toBeInTheDocument();
    expect(screen.getByTestId('recharts-yaxis')).toBeInTheDocument();
    expect(screen.getByTestId('recharts-grid')).toBeInTheDocument();
    expect(screen.getByTestId('recharts-tooltip')).toBeInTheDocument();
    expect(screen.getByTestId('recharts-legend')).toBeInTheDocument();
    
    // Check for line with count data key
    expect(screen.getByTestId('recharts-line-count')).toBeInTheDocument();
    
    // Check for summary
    expect(screen.getByText('2 data points • 1 series')).toBeInTheDocument();
  });

});