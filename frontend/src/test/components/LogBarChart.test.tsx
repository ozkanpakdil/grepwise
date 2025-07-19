import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LogBarChart from '@/components/LogBarChart';
import { TimeSlot } from '@/api/logSearch';
import React from 'react';

// Mock the useState hook for screen width
vi.mock('react', async () => {
  const actual = await vi.importActual('react');
  return {
    ...actual,
    useState: vi.fn(),
  };
});

describe('LogBarChart', () => {
  // Mock data
  const mockTimeSlots: TimeSlot[] = [
    { time: 1640995200000, count: 5 }, // 2022-01-01 00:00:00
    { time: 1640998800000, count: 10 }, // 2022-01-01 01:00:00
    { time: 1641002400000, count: 3 }, // 2022-01-01 02:00:00
    { time: 1641006000000, count: 8 }, // 2022-01-01 03:00:00
    { time: 1641009600000, count: 15 }, // 2022-01-01 04:00:00
  ];

  const mockOnTimeSlotClick = vi.fn();

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

  // Mock implementation for useState to return tablet width (600px)
  const mockUseStateForTablet = () => {
    vi.mocked(React.useState).mockImplementation((init) => {
      // Return tablet width (600px) for screenWidth state
      if (init === 1024) {
        return [600, vi.fn()];
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

  it('renders the chart title correctly', () => {
    render(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    expect(screen.getByText('Log Distribution')).toBeInTheDocument();
  });

  it('displays a message when no data is available', () => {
    render(
      <LogBarChart
        timeSlots={[]}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    expect(screen.getByText('No data available for the selected time range')).toBeInTheDocument();
  });

  it('renders bars for each time slot', () => {
    render(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Check for the container that holds the bars
    const barContainer = screen.getByText('Log Distribution').closest('div')?.nextElementSibling;
    expect(barContainer).toBeInTheDocument();

    // Check for the y-axis labels
    expect(screen.getByText('Max: 15')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('calls onTimeSlotClick when a bar is double-clicked', async () => {
    const user = userEvent.setup();
    render(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Find a bar and double-click it
    const bars = document.querySelectorAll('.bg-primary\\/80');
    expect(bars.length).toBeGreaterThan(0);
    
    await user.dblClick(bars[0]);
    
    expect(mockOnTimeSlotClick).toHaveBeenCalledWith(mockTimeSlots[0]);
  });

  it('shows tooltip on hover', async () => {
    const user = userEvent.setup();
    render(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Find a bar and hover over it
    const bars = document.querySelectorAll('.bg-primary\\/80');
    expect(bars.length).toBeGreaterThan(0);
    
    await user.hover(bars[0]);
    
    // Check if the tooltip appears with the correct content
    const tooltipContent = document.querySelector('.absolute.bg-background.border.rounded-md');
    expect(tooltipContent).toBeInTheDocument();
  });

  it('formats time based on timeRange prop', () => {
    // Test with 1h timeRange
    const { rerender } = render(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="1h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Check for time formatting (exact format depends on locale)
    const timeLabels = document.querySelectorAll('.text-xs.text-muted-foreground.mt-1');
    expect(timeLabels.length).toBeGreaterThan(0);

    // Test with custom timeRange
    rerender(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="custom"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Check that time labels are still rendered
    const customTimeLabels = document.querySelectorAll('.text-xs.text-muted-foreground.mt-1');
    expect(customTimeLabels.length).toBeGreaterThan(0);
  });

  it('adapts to small screen sizes (mobile)', () => {
    // Mock useState to return mobile width
    mockUseStateForMobile();
    
    render(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Check for mobile-specific elements
    expect(screen.getByText('Double-tap to zoom')).toBeInTheDocument();
  });

  it('adapts to medium screen sizes (tablet)', () => {
    // Mock useState to return tablet width
    mockUseStateForTablet();
    
    render(
      <LogBarChart
        timeSlots={mockTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Check for desktop text (used for tablet and desktop)
    expect(screen.getByText('Double-click on a bar to zoom into that time period')).toBeInTheDocument();
  });

  it('handles large numbers with compact formatting', () => {
    const largeCountTimeSlots: TimeSlot[] = [
      { time: 1640995200000, count: 1500 },
      { time: 1640998800000, count: 2500 },
    ];

    // Test with desktop width first
    mockUseStateForDesktop();
    
    const { unmount } = render(
      <LogBarChart
        timeSlots={largeCountTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Check for number formatting on desktop
    expect(screen.getByText('Max: 2500')).toBeInTheDocument();
    
    unmount();
    
    // Test with mobile width
    mockUseStateForMobile();
    
    render(
      <LogBarChart
        timeSlots={largeCountTimeSlots}
        timeRange="24h"
        onTimeSlotClick={mockOnTimeSlotClick}
      />
    );

    // Check for compact number formatting on mobile
    expect(screen.getByText('Max: 2.5K')).toBeInTheDocument();
  });
});