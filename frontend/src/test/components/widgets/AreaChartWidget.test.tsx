import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import AreaChartWidget from '@/components/widgets/AreaChartWidget';
import React from 'react';
import { DashboardWidget } from '@/api/dashboard';

// Mock the recharts components
vi.mock('recharts', () => ({
  AreaChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="area-chart">{children}</div>
  ),
  Area: ({ dataKey, name }: { dataKey: string; name: string }) => (
    <div data-testid={`area-${dataKey}`} data-name={name}>
      Area: {dataKey}
    </div>
  ),
  XAxis: ({ dataKey }: { dataKey: string }) => (
    <div data-testid="x-axis" data-key={dataKey}>
      XAxis: {dataKey}
    </div>
  ),
  YAxis: () => <div data-testid="y-axis">YAxis</div>,
  CartesianGrid: () => <div data-testid="cartesian-grid">CartesianGrid</div>,
  Tooltip: () => <div data-testid="tooltip">Tooltip</div>,
  Legend: () => <div data-testid="legend">Legend</div>,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="responsive-container">{children}</div>
  ),
}));

// Mock useState for screen width
vi.mock('react', async () => {
  const actual = await vi.importActual('react');
  return {
    ...actual,
    useState: vi.fn(),
  };
});

describe('AreaChartWidget', () => {
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

  const mockResultsData = {
    timeSlots: [],
    results: [
      {
        _time: 1640995200000,
        errors: 5,
        warnings: 10,
        info: 15,
      },
    ],
    total: 1,
  };

  const mockWidget: DashboardWidget = {
    id: 'widget1',
    title: 'Test Area Chart',
    type: 'area',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'user1',
  };

  // Mock implementation for useState to return desktop width (1024px)
  const mockUseStateForDesktop = () => {
    vi.mocked(React.useState).mockImplementation((init) => {
      // Return desktop width (1024px) for screenWidth state
      if (init === 1024) {
        return [1024, vi.fn()];
      }
      // For other useState calls, return the initial value
      return [init, vi.fn()];
    });
  };

  // Mock implementation for useState to return mobile width (400px)
  const mockUseStateForMobile = () => {
    vi.mocked(React.useState).mockImplementation((init) => {
      // Return mobile width (400px) for screenWidth state
      if (init === 1024) {
        return [400, vi.fn()];
      }
      // For other useState calls, return the initial value
      return [init, vi.fn()];
    });
  };

  beforeEach(() => {
    vi.clearAllMocks();
    // Default to desktop width
    mockUseStateForDesktop();
  });

  it('renders a message when no data is available', () => {
    render(<AreaChartWidget data={null} widget={mockWidget} />);
    
    expect(screen.getByText('No data available for area chart visualization')).toBeInTheDocument();
  });

  it('renders a message when data is empty', () => {
    render(<AreaChartWidget data={{ timeSlots: [], results: [] }} widget={mockWidget} />);
    
    expect(screen.getByText('No data available for area chart visualization')).toBeInTheDocument();
  });

  it('renders area chart with time slots data', () => {
    render(<AreaChartWidget data={mockTimeSlotData} widget={mockWidget} />);
    
    // Check for chart components
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('area-chart')).toBeInTheDocument();
    expect(screen.getByTestId('x-axis')).toBeInTheDocument();
    expect(screen.getByTestId('y-axis')).toBeInTheDocument();
    expect(screen.getByTestId('cartesian-grid')).toBeInTheDocument();
    expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    expect(screen.getByTestId('legend')).toBeInTheDocument();
    
    // Check for area component with count data key
    expect(screen.getByTestId('area-count')).toBeInTheDocument();
    
    // Check for summary text
    expect(screen.getByText('3 data points • 1 series')).toBeInTheDocument();
  });

  it('renders area chart with results data', () => {
    render(<AreaChartWidget data={mockResultsData} widget={mockWidget} />);
    
    // Check for chart components
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('area-chart')).toBeInTheDocument();
    
    // Should extract numeric fields from results
    expect(screen.getByTestId('area-errors')).toBeInTheDocument();
    expect(screen.getByTestId('area-warnings')).toBeInTheDocument();
    expect(screen.getByTestId('area-info')).toBeInTheDocument();
  });

  it('adapts to small screen sizes', () => {
    // Mock useState to return mobile width
    mockUseStateForMobile();
    
    render(<AreaChartWidget data={mockTimeSlotData} widget={mockWidget} />);
    
    // Check for responsive container
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    
    // Check for summary text with smaller font
    const summaryElement = screen.getByText('3 data points • 1 series');
    expect(summaryElement.className).toContain('text-[10px]');
  });

  it('handles different data formats correctly', () => {
    // Test with data that has no time-based fields
    const nonTimeData = {
      results: [
        {
          name: 'Category A',
          value: 10,
        },
        {
          name: 'Category B',
          value: 20,
        },
      ],
    };
    
    render(<AreaChartWidget data={nonTimeData} widget={mockWidget} />);
    
    // Should still render the chart
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('area-chart')).toBeInTheDocument();
  });

  it('uses different colors for multiple data series', () => {
    render(<AreaChartWidget data={mockResultsData} widget={mockWidget} />);
    
    // Check that each area has a different data-name attribute
    const errorArea = screen.getByTestId('area-errors');
    const warningArea = screen.getByTestId('area-warnings');
    const infoArea = screen.getByTestId('area-info');
    
    expect(errorArea.getAttribute('data-name')).toBe('errors');
    expect(warningArea.getAttribute('data-name')).toBe('warnings');
    expect(infoArea.getAttribute('data-name')).toBe('info');
  });

  it('handles empty results array', () => {
    const emptyResultsData = {
      timeSlots: [],
      results: [],
      total: 0,
    };
    
    render(<AreaChartWidget data={emptyResultsData} widget={mockWidget} />);
    
    expect(screen.getByText('No data available for area chart visualization')).toBeInTheDocument();
  });
});