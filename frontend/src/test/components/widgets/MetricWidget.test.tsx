import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import MetricWidget from '@/components/widgets/MetricWidget';

describe('MetricWidget', () => {
  const mockWidget = {
    id: 'w1',
    title: 'Test Metric',
    type: 'metric',
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
        count: 150,
        avg_response_time: 250.5,
        error_rate: 2.3,
      },
    ],
    timeSlots: [],
    total: 1,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders metric with time slot data showing total events', () => {
    render(<MetricWidget data={mockTimeSlotData} widget={mockWidget} />);

    // Should show total count from time slots
    expect(screen.getByText('53')).toBeInTheDocument(); // 10+15+8+20
    expect(screen.getByText('Total Events')).toBeInTheDocument();
  });

  it('renders metric with stats data showing first numeric field', () => {
    render(<MetricWidget data={mockStatsData} widget={mockWidget} />);

    // Should show the first numeric field (count)
    expect(screen.getByText('150')).toBeInTheDocument();
    expect(screen.getByText('Count')).toBeInTheDocument();
  });

  it('prioritizes common metric fields', () => {
    const dataWithMultipleFields = {
      results: [
        {
          some_field: 100,
          total: 200, // Should be prioritized
          other_field: 300,
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={dataWithMultipleFields} widget={mockWidget} />);

    // Should show 'total' field as it's in the priority list
    expect(screen.getByText('200')).toBeInTheDocument();
    expect(screen.getByText('Total')).toBeInTheDocument();
  });

  it('formats large numbers with K/M suffixes', () => {
    const largeNumberData = {
      results: [{ count: 1500000 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={largeNumberData} widget={mockWidget} />);

    // Should format as 1.5M
    expect(screen.getByText('1.5M')).toBeInTheDocument();
  });

  it('formats thousands with K suffix', () => {
    const thousandData = {
      results: [{ count: 2500 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={thousandData} widget={mockWidget} />);

    // Should format as 2.5K
    expect(screen.getByText('2.5K')).toBeInTheDocument();
  });

  it('shows decimal values for small numbers', () => {
    const decimalData = {
      results: [{ rate: 2.35 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={decimalData} widget={mockWidget} />);

    // Should show decimal value
    expect(screen.getByText('2.35')).toBeInTheDocument();
  });

  it('shows integer values without decimals', () => {
    const integerData = {
      results: [{ count: 42 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={integerData} widget={mockWidget} />);

    // Should show integer value
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('determines units based on field names', () => {
    const percentData = {
      results: [{ error_rate: 5.2 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={percentData} widget={mockWidget} />);

    // Should show percentage unit
    expect(screen.getByText(/5\.2/)).toBeInTheDocument();
    expect(screen.getByText(/%/)).toBeInTheDocument();
  });

  it('shows time units for time-related fields', () => {
    const timeData = {
      results: [{ response_time: 150 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={timeData} widget={mockWidget} />);

    // Should show milliseconds unit
    expect(screen.getByText('150')).toBeInTheDocument();
    expect(screen.getByText(/ms/)).toBeInTheDocument();
  });

  it('shows bytes unit for size-related fields', () => {
    const sizeData = {
      results: [{ file_size: 1024 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={sizeData} widget={mockWidget} />);

    // Should show bytes unit
    expect(screen.getByText('1.0K')).toBeInTheDocument();
    expect(screen.getByText(/B/)).toBeInTheDocument();
  });

  it('calculates trend from time slot data', () => {
    const trendData = {
      results: [],
      timeSlots: [
        { time: 1640995200000, count: 10 }, // First half
        { time: 1640998800000, count: 10 },
        { time: 1641002400000, count: 20 }, // Second half (doubled)
        { time: 1641006000000, count: 20 },
      ],
      total: 4,
    };

    render(<MetricWidget data={trendData} widget={mockWidget} />);

    // Should show upward trend (100% increase)
    expect(screen.getByText('100.0%')).toBeInTheDocument();
    expect(screen.getByText('vs previous period')).toBeInTheDocument();
    
    // Should show trending up icon (check for class instead of data attribute)
    const trendIcon = document.querySelector('.text-green-600');
    expect(trendIcon).toBeInTheDocument();
  });

  it('shows downward trend when values decrease', () => {
    const downTrendData = {
      results: [],
      timeSlots: [
        { time: 1640995200000, count: 20 }, // First half (higher)
        { time: 1640998800000, count: 20 },
        { time: 1641002400000, count: 10 }, // Second half (lower)
        { time: 1641006000000, count: 10 },
      ],
      total: 4,
    };

    render(<MetricWidget data={downTrendData} widget={mockWidget} />);

    // Should show downward trend (50% decrease)
    expect(screen.getByText('50.0%')).toBeInTheDocument();
    
    // Should show trending down icon (check for red color class)
    const trendIcon = document.querySelector('.text-red-600');
    expect(trendIcon).toBeInTheDocument();
  });

  it('shows neutral trend for small changes', () => {
    const neutralTrendData = {
      results: [],
      timeSlots: [
        { time: 1640995200000, count: 100 }, // First half
        { time: 1640998800000, count: 100 },
        { time: 1641002400000, count: 102 }, // Second half (2% increase - below threshold)
        { time: 1641006000000, count: 102 },
      ],
      total: 4,
    };

    render(<MetricWidget data={neutralTrendData} widget={mockWidget} />);

    // Should not show trend indicator for small changes
    expect(screen.queryByText('vs previous period')).not.toBeInTheDocument();
  });

  it('determines status based on field type - error fields', () => {
    const errorData = {
      results: [{ error_count: 15 }], // > 10, should be critical
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={errorData} widget={mockWidget} />);

    // Should show critical status (red) - check the main container
    const metricContainer = document.querySelector('.h-full.flex.flex-col.justify-center.items-center');
    expect(metricContainer).toHaveClass('bg-red-50', 'border-red-200');
    
    // Should show alert icon
    const alertIcon = document.querySelector('.h-5.w-5');
    expect(alertIcon).toBeInTheDocument();
  });

  it('determines status based on field type - warning fields', () => {
    const warningData = {
      results: [{ warning_count: 8 }], // > 5, should be warning
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={warningData} widget={mockWidget} />);

    // Should show warning status (yellow) - check the main container
    const metricContainer = document.querySelector('.h-full.flex.flex-col.justify-center.items-center');
    expect(metricContainer).toHaveClass('bg-yellow-50', 'border-yellow-200');
  });

  it('shows normal status for regular metrics', () => {
    const normalData = {
      results: [{ count: 100 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={normalData} widget={mockWidget} />);

    // Should show normal status (green) - check the main container
    const metricContainer = document.querySelector('.h-full.flex.flex-col.justify-center.items-center');
    expect(metricContainer).toHaveClass('bg-green-50', 'border-green-200');
    
    // Should not show alert icon
    const alertIcon = document.querySelector('.h-5.w-5');
    expect(alertIcon).not.toBeInTheDocument();
  });

  it('shows gauge visualization for percentage values', () => {
    const percentageData = {
      results: [{ cpu_usage_percent: 75 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={percentageData} widget={mockWidget} />);

    // Should show gauge bar
    const gaugeBar = document.querySelector('.bg-yellow-500'); // 75% should be yellow
    expect(gaugeBar).toBeInTheDocument();
    expect(gaugeBar).toHaveStyle('width: 75%');
  });

  it('shows red gauge for high percentage values', () => {
    const highPercentData = {
      results: [{ usage_percent: 90 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={highPercentData} widget={mockWidget} />);

    // Should show red gauge for high values
    const gaugeBar = document.querySelector('.bg-red-500');
    expect(gaugeBar).toBeInTheDocument();
  });

  it('shows green gauge for low percentage values', () => {
    const lowPercentData = {
      results: [{ success_rate: 45 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={lowPercentData} widget={mockWidget} />);

    // Should show green gauge for low values
    const gaugeBar = document.querySelector('.bg-green-500');
    expect(gaugeBar).toBeInTheDocument();
  });

  it('caps gauge at 100%', () => {
    const overHundredData = {
      results: [{ rate: 150 }], // Over 100%
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={overHundredData} widget={mockWidget} />);

    // Should cap gauge width at 100%
    const gaugeBar = document.querySelector('.bg-red-500');
    expect(gaugeBar).toHaveStyle('width: 100%');
  });

  it('shows no data message when data is null', () => {
    render(<MetricWidget data={null} widget={mockWidget} />);

    expect(screen.getByText('No Data')).toBeInTheDocument();
    
    // Should show warning status
    const container = document.querySelector('.h-full.flex.flex-col.justify-center.items-center');
    expect(container).toHaveClass('bg-yellow-50', 'border-yellow-200');
  });

  it('shows default metric when data is empty', () => {
    const emptyData = {
      results: [],
      timeSlots: [],
      total: 0,
    };

    render(<MetricWidget data={emptyData} widget={mockWidget} />);

    // Component shows default "Metric" with value 0 for empty data
    expect(screen.getByText('0')).toBeInTheDocument();
    expect(screen.getByText('Metric')).toBeInTheDocument();
  });

  it('handles missing results gracefully', () => {
    const dataWithoutResults = {
      timeSlots: [],
      total: 0,
    };

    render(<MetricWidget data={dataWithoutResults} widget={mockWidget} />);

    // Component shows default "Metric" with value 0 for missing results
    expect(screen.getByText('0')).toBeInTheDocument();
    expect(screen.getByText('Metric')).toBeInTheDocument();
  });

  it('ignores non-numeric fields in stats data', () => {
    const mixedData = {
      results: [
        {
          message: 'test message', // Should be ignored
          timestamp: '2024-01-01', // Should be ignored
          _time: 1640995200000, // Should be ignored
          count: 42, // Should be used
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={mixedData} widget={mockWidget} />);

    // Should show the numeric field
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Count')).toBeInTheDocument();
  });

  it('formats field names properly', () => {
    const fieldNameData = {
      results: [{ avg_response_time: 150 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={fieldNameData} widget={mockWidget} />);

    // Should format field name (capitalize and replace underscores)
    expect(screen.getByText('Avg response time')).toBeInTheDocument();
  });

  it('applies correct text colors based on status', () => {
    const errorData = {
      results: [{ error_count: 15 }],
      timeSlots: [],
      total: 1,
    };

    render(<MetricWidget data={errorData} widget={mockWidget} />);

    // Should have red text for critical status
    const valueElement = screen.getByText('15');
    expect(valueElement).toHaveClass('text-red-600');
  });

  it('shows trend colors correctly', () => {
    const trendData = {
      results: [],
      timeSlots: [
        { time: 1640995200000, count: 10 },
        { time: 1640998800000, count: 10 },
        { time: 1641002400000, count: 20 },
        { time: 1641006000000, count: 20 },
      ],
      total: 4,
    };

    render(<MetricWidget data={trendData} widget={mockWidget} />);

    // Trend percentage should be green for upward trend
    const trendPercentage = screen.getByText('100.0%');
    expect(trendPercentage).toHaveClass('text-green-600');
  });
});