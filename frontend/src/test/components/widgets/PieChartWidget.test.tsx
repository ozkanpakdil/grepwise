import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import PieChartWidget from '@/components/widgets/PieChartWidget';

describe('PieChartWidget', () => {
  const mockWidget = {
    id: 'w1',
    title: 'Test Pie Chart',
    type: 'pie',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'current-user',
  };

  const mockStatsData = {
    results: [
      {
        error_count: 25,
        warning_count: 15,
        info_count: 60,
        debug_count: 5,
      },
    ],
    timeSlots: [],
    total: 1,
  };

  const mockTimeSlotData = {
    results: [],
    timeSlots: [
      { time: new Date('2024-01-01T08:00:00Z').getTime(), count: 10 }, // Morning
      { time: new Date('2024-01-01T14:00:00Z').getTime(), count: 15 }, // Afternoon
      { time: new Date('2024-01-01T20:00:00Z').getTime(), count: 8 },  // Evening
      { time: new Date('2024-01-01T02:00:00Z').getTime(), count: 5 },  // Night
    ],
    total: 4,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders pie chart with stats data', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    // Check for legend items
    expect(screen.getByText('error_count')).toBeInTheDocument();
    expect(screen.getByText('warning_count')).toBeInTheDocument();
    expect(screen.getByText('info_count')).toBeInTheDocument();
    expect(screen.getByText('debug_count')).toBeInTheDocument();

    // Check for summary information
    expect(screen.getByText(/4 categories • Total: 105/)).toBeInTheDocument();
  });

  it('renders pie chart with time slot data grouped by periods', () => {
    render(<PieChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    // Should group time slots into periods
    expect(screen.getByText('Morning (6-12)')).toBeInTheDocument();
    expect(screen.getByText('Afternoon (12-18)')).toBeInTheDocument();
    expect(screen.getByText('Evening (18-24)')).toBeInTheDocument();
    expect(screen.getByText('Night (0-6)')).toBeInTheDocument();

    // Check for summary
    expect(screen.getByText(/4 categories • Total: 38/)).toBeInTheDocument();
  });

  it('renders SVG pie chart with correct number of slices', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    // Check for SVG element
    const svg = document.querySelector('svg');
    expect(svg).toBeInTheDocument();

    // Check for pie slices (path elements)
    const paths = document.querySelectorAll('path');
    expect(paths.length).toBe(4); // One for each data point
  });

  it('shows correct percentages in legend', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    // info_count should be the largest (60/105 = 57.1%)
    expect(screen.getByText(/57\.1%/)).toBeInTheDocument();
    
    // error_count should be 25/105 = 23.8%
    expect(screen.getByText(/23\.8%/)).toBeInTheDocument();
    
    // warning_count should be 15/105 = 14.3%
    expect(screen.getByText(/14\.3%/)).toBeInTheDocument();
    
    // debug_count should be 5/105 = 4.8%
    expect(screen.getByText(/4\.8%/)).toBeInTheDocument();
  });

  it('shows tooltip on slice hover', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    const firstPath = document.querySelector('path');
    expect(firstPath).toBeInTheDocument();

    // Simulate hover
    fireEvent.mouseEnter(firstPath!);

    // Check for tooltip
    const tooltip = document.querySelector('.absolute.bg-black');
    expect(tooltip).toBeInTheDocument();
  });

  it('hides tooltip on mouse leave', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    const firstPath = document.querySelector('path');
    expect(firstPath).toBeInTheDocument();

    // Simulate hover and then leave
    fireEvent.mouseEnter(firstPath!);
    fireEvent.mouseLeave(firstPath!);

    // Tooltip should be hidden
    const tooltip = document.querySelector('.absolute.bg-black');
    expect(tooltip).not.toBeInTheDocument();
  });

  it('shows tooltip on legend item hover', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    const legendItem = screen.getByText('error_count').closest('.cursor-pointer');
    expect(legendItem).toBeInTheDocument();

    // Simulate hover on legend item
    fireEvent.mouseEnter(legendItem!);

    // Should show tooltip (legend items also trigger tooltip)
    const tooltip = document.querySelector('.absolute.bg-black');
    expect(tooltip).toBeInTheDocument();
  });

  it('limits data to top 8 slices', () => {
    const largeStatsData = {
      results: [
        Object.fromEntries(
          Array.from({ length: 15 }, (_, i) => [`field_${i}`, i + 1])
        ),
      ],
      timeSlots: [],
      total: 1,
    };

    render(<PieChartWidget data={largeStatsData} widget={mockWidget} />);

    // Should only show 8 slices
    const paths = document.querySelectorAll('path');
    expect(paths.length).toBe(8);

    // Should show 8 categories in summary
    expect(screen.getByText(/8 categories/)).toBeInTheDocument();
  });

  it('sorts data by value in descending order', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    // Legend items should be sorted by value (highest first)
    const legendItems = document.querySelectorAll('.flex.items-center.space-x-2.text-xs');
    const firstItem = legendItems[0];
    const lastItem = legendItems[legendItems.length - 1];

    // First item should be info_count (highest value: 60)
    expect(firstItem).toHaveTextContent('info_count');
    expect(firstItem).toHaveTextContent('60');

    // Last item should be debug_count (lowest value: 5)
    expect(lastItem).toHaveTextContent('debug_count');
    expect(lastItem).toHaveTextContent('5');
  });

  it('filters out zero values from stats data', () => {
    const dataWithZeros = {
      results: [
        {
          error_count: 25,
          warning_count: 0, // Should be filtered out
          info_count: 60,
          debug_count: 5,
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<PieChartWidget data={dataWithZeros} widget={mockWidget} />);

    // Should only show 3 slices (warning_count filtered out)
    const paths = document.querySelectorAll('path');
    expect(paths.length).toBe(3);

    expect(screen.getByText('error_count')).toBeInTheDocument();
    expect(screen.queryByText('warning_count')).not.toBeInTheDocument();
    expect(screen.getByText('info_count')).toBeInTheDocument();
    expect(screen.getByText('debug_count')).toBeInTheDocument();
  });

  it('filters out non-numeric and timestamp fields from stats data', () => {
    const mixedStatsData = {
      results: [
        {
          count: 10,
          timestamp: '2024-01-01T00:00:00Z', // Should be filtered out
          _time: 1640995200000, // Should be filtered out
          message: 'test message', // Should be filtered out
          level: 'INFO', // Should be filtered out
          numeric_field: 25,
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<PieChartWidget data={mixedStatsData} widget={mockWidget} />);

    // Should only show numeric fields
    const paths = document.querySelectorAll('path');
    expect(paths.length).toBe(2);

    expect(screen.getByText('count')).toBeInTheDocument();
    expect(screen.getByText('numeric_field')).toBeInTheDocument();
    expect(screen.queryByText('timestamp')).not.toBeInTheDocument();
    expect(screen.queryByText('message')).not.toBeInTheDocument();
  });

  it('shows no data message when data is empty', () => {
    const emptyData = {
      results: [],
      timeSlots: [],
      total: 0,
    };

    render(<PieChartWidget data={emptyData} widget={mockWidget} />);

    expect(screen.getByText('No data available for pie chart visualization')).toBeInTheDocument();
  });

  it('shows no data message when data is null', () => {
    render(<PieChartWidget data={null} widget={mockWidget} />);

    expect(screen.getByText('No data available for pie chart visualization')).toBeInTheDocument();
  });

  it('shows no data message when all values are zero', () => {
    const allZeroData = {
      results: [
        {
          error_count: 0,
          warning_count: 0,
          info_count: 0,
          debug_count: 0,
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<PieChartWidget data={allZeroData} widget={mockWidget} />);

    expect(screen.getByText('No data available for pie chart visualization')).toBeInTheDocument();
  });

  it('applies correct colors to pie slices', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    const paths = document.querySelectorAll('path');
    const colors = ['#3b82f6', '#ef4444', '#10b981', '#f59e0b']; // First 4 colors from palette

    paths.forEach((path, index) => {
      expect(path).toHaveAttribute('fill', colors[index]);
    });
  });

  it('sets correct stroke properties for pie slices', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    const paths = document.querySelectorAll('path');
    
    paths.forEach(path => {
      expect(path).toHaveAttribute('stroke', 'white');
      expect(path).toHaveAttribute('stroke-width', '2');
    });
  });

  it('applies hover effects to pie slices', () => {
    render(<PieChartWidget data={mockStatsData} widget={mockWidget} />);

    const paths = document.querySelectorAll('path');
    
    paths.forEach(path => {
      expect(path).toHaveClass('cursor-pointer');
      expect(path).toHaveClass('transition-opacity');
      expect(path).toHaveClass('hover:opacity-80');
    });
  });

  it('handles time slot data with missing periods', () => {
    const partialTimeData = {
      results: [],
      timeSlots: [
        { time: new Date('2024-01-01T08:00:00Z').getTime(), count: 10 }, // Morning only
      ],
      total: 1,
    };

    render(<PieChartWidget data={partialTimeData} widget={mockWidget} />);

    // Should only show the period with data
    expect(screen.getByText('Morning (6-12)')).toBeInTheDocument();
    expect(screen.queryByText('Afternoon (12-18)')).not.toBeInTheDocument();
    expect(screen.queryByText('Evening (18-24)')).not.toBeInTheDocument();
    expect(screen.queryByText('Night (0-6)')).not.toBeInTheDocument();

    expect(screen.getByText(/1 categories • Total: 10/)).toBeInTheDocument();
  });
});