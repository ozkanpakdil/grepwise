import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import BarChartWidget from '@/components/widgets/BarChartWidget';

describe('BarChartWidget', () => {
  const mockWidget = {
    id: 'w1',
    title: 'Test Bar Chart',
    type: 'chart',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'current-user',
  };

  const mockTimeSlotData = {
    results: [],
    timeSlots: [
      { time: 1640995200000, count: 10 },
      { time: 1640998800000, count: 15 },
      { time: 1641002400000, count: 8 },
      { time: 1641006000000, count: 20 },
    ],
    total: 4,
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

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders bar chart with time slot data', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    // Check that bars are rendered
    const bars = document.querySelectorAll('.bg-blue-500');
    expect(bars.length).toBe(4);

    // Check for summary information
    expect(screen.getByText('4 data points • Max: 20')).toBeInTheDocument();
  });

  it('renders bar chart with stats data', () => {
    render(<BarChartWidget data={mockStatsData} widget={mockWidget} />);

    // Should render bars for each stat field
    const bars = document.querySelectorAll('.bg-blue-500');
    expect(bars.length).toBe(4); // error_count, warning_count, info_count, debug_count

    // Check for summary information
    expect(screen.getByText('4 data points • Max: 60')).toBeInTheDocument();
  });

  it('shows correct bar heights based on values', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    const bars = document.querySelectorAll('.bg-blue-500');
    
    // The highest value (20) should have 100% height
    // Other bars should have proportional heights
    const heights = Array.from(bars).map(bar => 
      parseFloat((bar as HTMLElement).style.height.replace('%', ''))
    );

    expect(Math.max(...heights)).toBe(100); // Max height should be 100%
    expect(Math.min(...heights)).toBeGreaterThan(0); // All bars should have some height
  });

  it('displays Y-axis labels with correct values', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    // Check for Y-axis labels
    expect(screen.getByText('20')).toBeInTheDocument(); // Max value
    expect(screen.getByText('10')).toBeInTheDocument(); // Mid value (max/2)
    expect(screen.getByText('0')).toBeInTheDocument(); // Min value
  });

  it('shows tooltip on bar hover', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    const firstBar = document.querySelector('.bg-blue-500');
    expect(firstBar).toBeInTheDocument();

    // Simulate hover
    fireEvent.mouseEnter(firstBar!);

    // Check for tooltip content
    const tooltip = document.querySelector('.absolute.bg-black');
    expect(tooltip).toBeInTheDocument();
  });

  it('hides tooltip on mouse leave', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    const firstBar = document.querySelector('.bg-blue-500');
    expect(firstBar).toBeInTheDocument();

    // Simulate hover and then leave
    fireEvent.mouseEnter(firstBar!);
    fireEvent.mouseLeave(firstBar!);

    // Tooltip should be hidden
    const tooltip = document.querySelector('.absolute.bg-black');
    expect(tooltip).not.toBeInTheDocument();
  });

  it('formats time labels correctly for time slot data', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    // Should show time labels (only some due to spacing logic)
    const timeLabels = document.querySelectorAll('.text-xs.text-muted-foreground');
    expect(timeLabels.length).toBeGreaterThan(0);
  });

  it('shows field names as labels for stats data', () => {
    render(<BarChartWidget data={mockStatsData} widget={mockWidget} />);

    // Should show field names as labels
    expect(screen.getByText('error_count')).toBeInTheDocument();
    expect(screen.getByText('warning_count')).toBeInTheDocument();
    expect(screen.getByText('info_count')).toBeInTheDocument();
    expect(screen.getByText('debug_count')).toBeInTheDocument();
  });

  it('limits stats data to 20 bars for readability', () => {
    const largeStatsData = {
      results: [
        Object.fromEntries(
          Array.from({ length: 30 }, (_, i) => [`field_${i}`, i + 1])
        ),
      ],
      timeSlots: [],
      total: 1,
    };

    render(<BarChartWidget data={largeStatsData} widget={mockWidget} />);

    const bars = document.querySelectorAll('.bg-blue-500');
    expect(bars.length).toBe(20); // Should be limited to 20

    expect(screen.getByText('20 data points • Max: 20')).toBeInTheDocument();
  });

  it('shows no data message when data is empty', () => {
    const emptyData = {
      results: [],
      timeSlots: [],
      total: 0,
    };

    render(<BarChartWidget data={emptyData} widget={mockWidget} />);

    expect(screen.getByText('No data available for chart visualization')).toBeInTheDocument();
  });

  it('shows no data message when data is null', () => {
    render(<BarChartWidget data={null} widget={mockWidget} />);

    expect(screen.getByText('No data available for chart visualization')).toBeInTheDocument();
  });

  it('shows no data message when timeSlots is missing', () => {
    const dataWithoutTimeSlots = {
      results: [],
      total: 0,
    };

    render(<BarChartWidget data={dataWithoutTimeSlots} widget={mockWidget} />);

    expect(screen.getByText('No data available for chart visualization')).toBeInTheDocument();
  });

  it('filters out non-numeric fields from stats data', () => {
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

    render(<BarChartWidget data={mixedStatsData} widget={mockWidget} />);

    const bars = document.querySelectorAll('.bg-blue-500');
    expect(bars.length).toBe(2); // Only count and numeric_field

    expect(screen.getByText('count')).toBeInTheDocument();
    expect(screen.getByText('numeric_field')).toBeInTheDocument();
    expect(screen.queryByText('timestamp')).not.toBeInTheDocument();
    expect(screen.queryByText('message')).not.toBeInTheDocument();
  });

  it('handles zero values correctly', () => {
    const dataWithZeros = {
      results: [],
      timeSlots: [
        { time: 1640995200000, count: 0 },
        { time: 1640998800000, count: 5 },
        { time: 1641002400000, count: 0 },
      ],
      total: 3,
    };

    render(<BarChartWidget data={dataWithZeros} widget={mockWidget} />);

    const bars = document.querySelectorAll('.bg-blue-500');
    expect(bars.length).toBe(3);

    // Zero values should still render bars with minimum height
    const heights = Array.from(bars).map(bar => 
      parseFloat((bar as HTMLElement).style.height.replace('%', ''))
    );
    
    // All bars should have at least 2% height (minimum height)
    heights.forEach(height => {
      expect(height).toBeGreaterThanOrEqual(2);
    });
  });

  it('shows appropriate X-axis labels based on data size', () => {
    // Test with small dataset (should show all labels)
    const smallData = {
      results: [],
      timeSlots: [
        { time: 1640995200000, count: 10 },
        { time: 1640998800000, count: 15 },
      ],
      total: 2,
    };

    const { rerender } = render(<BarChartWidget data={smallData} widget={mockWidget} />);

    // With small dataset, should show labels for all bars
    let labels = document.querySelectorAll('.text-xs.text-muted-foreground.mt-1');
    expect(labels.length).toBe(2);

    // Test with large dataset (should show fewer labels)
    const largeData = {
      results: [],
      timeSlots: Array.from({ length: 20 }, (_, i) => ({
        time: 1640995200000 + i * 3600000,
        count: i + 1,
      })),
      total: 20,
    };

    rerender(<BarChartWidget data={largeData} widget={mockWidget} />);

    // With large dataset, should show fewer labels (every nth item)
    labels = document.querySelectorAll('.text-xs.text-muted-foreground.mt-1');
    expect(labels.length).toBeLessThan(20);
  });

  it('applies hover effects to bars', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    const firstBar = document.querySelector('.bg-blue-500');
    expect(firstBar).toHaveClass('hover:bg-blue-600');
    expect(firstBar).toHaveClass('cursor-pointer');
    expect(firstBar).toHaveClass('transition-colors');
  });

  it('sets correct title attributes for accessibility', () => {
    render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);

    const bars = document.querySelectorAll('.bg-blue-500');
    bars.forEach(bar => {
      expect(bar).toHaveAttribute('title');
      const title = bar.getAttribute('title');
      expect(title).toMatch(/: \d+$/); // Should end with ": number"
    });
  });
});