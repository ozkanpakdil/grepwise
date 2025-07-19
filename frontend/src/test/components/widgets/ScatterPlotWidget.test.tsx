import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import ScatterPlotWidget from '@/components/widgets/ScatterPlotWidget';
import React from 'react';
import { DashboardWidget } from '@/api/dashboard';

// Mock the recharts components
vi.mock('recharts', () => ({
  ScatterChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="scatter-chart">{children}</div>
  ),
  Scatter: ({ name, data }: { name: string; data: any[] }) => (
    <div data-testid={`scatter-${name}`} data-count={data.length}>
      Scatter: {name}
    </div>
  ),
  XAxis: ({ dataKey, name }: { dataKey: string; name: string }) => (
    <div data-testid="x-axis" data-key={dataKey} data-name={name}>
      XAxis: {dataKey}
    </div>
  ),
  YAxis: ({ dataKey, name }: { dataKey: string; name: string }) => (
    <div data-testid="y-axis" data-key={dataKey} data-name={name}>
      YAxis: {dataKey}
    </div>
  ),
  ZAxis: ({ dataKey, name }: { dataKey: string; name: string }) => (
    <div data-testid="z-axis" data-key={dataKey} data-name={name}>
      ZAxis: {dataKey}
    </div>
  ),
  CartesianGrid: () => <div data-testid="cartesian-grid">CartesianGrid</div>,
  Tooltip: () => <div data-testid="tooltip">Tooltip</div>,
  Legend: () => <div data-testid="legend">Legend</div>,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="responsive-container">{children}</div>
  ),
}));

describe('ScatterPlotWidget', () => {
  // Mock data
  const mockTimeSlotData = {
    timeSlots: [
      { time: 1640995200000, count: 5 }, // 2022-01-01 00:00:00
      { time: 1640998800000, count: 10 }, // 2022-01-01 01:00:00
      { time: 1641002400000, count: 3 }, // 2022-01-01 02:00:00
    ],
    results: [],
    total: 3,
  };

  const mockNumericFieldsData = {
    timeSlots: [],
    results: [
      {
        id: '1',
        cpu: 45,
        memory: 75,
        disk: 30,
        timestamp: 1640995200000,
      },
      {
        id: '2',
        cpu: 60,
        memory: 85,
        disk: 40,
        timestamp: 1640998800000,
      },
    ],
    total: 2,
  };

  const mockFrequencyData = {
    timeSlots: [],
    results: [
      {
        level: 'ERROR',
        component: 'API',
        count: 5,
      },
      {
        level: 'ERROR',
        component: 'Database',
        count: 3,
      },
      {
        level: 'WARN',
        component: 'API',
        count: 8,
      },
    ],
    total: 3,
  };

  const mockWidget: DashboardWidget = {
    id: 'widget1',
    title: 'Test Scatter Plot',
    type: 'scatter',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'user1',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders a message when no data is available', () => {
    render(<ScatterPlotWidget data={null} widget={mockWidget} />);
    
    expect(screen.getByText('No suitable data available for scatter plot visualization')).toBeInTheDocument();
  });

  it('renders a message when data is empty', () => {
    render(<ScatterPlotWidget data={{ timeSlots: [], results: [] }} widget={mockWidget} />);
    
    expect(screen.getByText('No suitable data available for scatter plot visualization')).toBeInTheDocument();
  });

  it('renders scatter plot with time slots data', () => {
    render(<ScatterPlotWidget data={mockTimeSlotData} widget={mockWidget} />);
    
    // Check for chart components
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('scatter-chart')).toBeInTheDocument();
    expect(screen.getByTestId('x-axis')).toBeInTheDocument();
    expect(screen.getByTestId('y-axis')).toBeInTheDocument();
    expect(screen.getByTestId('z-axis')).toBeInTheDocument();
    expect(screen.getByTestId('cartesian-grid')).toBeInTheDocument();
    expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    expect(screen.getByTestId('legend')).toBeInTheDocument();
    
    // Check for scatter component
    expect(screen.getByTestId('scatter-Time Slots')).toBeInTheDocument();
    
    // Check for summary text
    expect(screen.getByText('3 data points • 1 series')).toBeInTheDocument();
  });

  it('renders scatter plot with numeric fields data', () => {
    render(<ScatterPlotWidget data={mockNumericFieldsData} widget={mockWidget} />);
    
    // Check for chart components
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('scatter-chart')).toBeInTheDocument();
    
    // Check for scatter component
    expect(screen.getByTestId('scatter-Data Points')).toBeInTheDocument();
    
    // Check for axis labels
    const xAxis = screen.getByTestId('x-axis');
    const yAxis = screen.getByTestId('y-axis');
    
    // First two numeric fields should be used for x and y axes
    expect(xAxis.getAttribute('data-name')).toBe('cpu');
    expect(yAxis.getAttribute('data-name')).toBe('memory');
  });

  it('renders scatter plot with frequency data', () => {
    render(<ScatterPlotWidget data={mockFrequencyData} widget={mockWidget} />);
    
    // Check for chart components
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('scatter-chart')).toBeInTheDocument();
    
    // Check for scatter component
    expect(screen.getByTestId('scatter-Frequency')).toBeInTheDocument();
  });

  it('formats x-axis for time values', () => {
    render(<ScatterPlotWidget data={mockTimeSlotData} widget={mockWidget} />);
    
    // Check for x-axis with Time label
    const xAxis = screen.getByTestId('x-axis');
    expect(xAxis.getAttribute('data-name')).toBe('Time');
  });

  it('uses z-axis for point size', () => {
    render(<ScatterPlotWidget data={mockNumericFieldsData} widget={mockWidget} />);
    
    // Check for z-axis
    const zAxis = screen.getByTestId('z-axis');
    expect(zAxis.getAttribute('data-key')).toBe('z');
    
    // If there's a third numeric field, it should be used for z-axis
    expect(zAxis.getAttribute('data-name')).toBe('disk');
  });

  it('handles data with insufficient numeric fields', () => {
    // Data with only one numeric field
    const insufficientData = {
      results: [
        {
          id: '1',
          name: 'Test',
          value: 10,
        },
      ],
    };
    
    render(<ScatterPlotWidget data={insufficientData} widget={mockWidget} />);
    
    // Should still render the chart by creating a frequency scatter plot
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('scatter-chart')).toBeInTheDocument();
  });

  it('displays correct number of data points in summary', () => {
    render(<ScatterPlotWidget data={mockNumericFieldsData} widget={mockWidget} />);
    
    // Check for summary text with correct count
    expect(screen.getByText('2 data points • 1 series')).toBeInTheDocument();
  });

  it('handles hover state for data points', () => {
    // Create a mock implementation of useState for hoveredPoint
    vi.mock('react', async () => {
      const actual = await vi.importActual('react');
      return {
        ...actual,
        useState: vi.fn().mockImplementation((init) => {
          if (init === null) {
            // For hoveredPoint state
            return [{ x: 45, y: 75, z: 30, name: 'Point 1' }, vi.fn()];
          }
          // For other useState calls, return the initial value
          return [init, vi.fn()];
        }),
      };
    });
    
    render(<ScatterPlotWidget data={mockNumericFieldsData} widget={mockWidget} />);
    
    // With hoveredPoint set, tooltip should be visible
    expect(screen.getByTestId('tooltip')).toBeInTheDocument();
  });
});