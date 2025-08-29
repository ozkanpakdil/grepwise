import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import BarChartWidget from '@/components/widgets/BarChartWidget';
import PieChartWidget from '@/components/widgets/PieChartWidget';
import LineChartWidget from '@/components/widgets/LineChartWidget';
import AreaChartWidget from '@/components/widgets/AreaChartWidget';
import LogBarChart from '@/components/LogBarChart';

// Mock data for testing
const mockWidget = {
  id: 'w1',
  title: 'Test Chart',
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

const mockPieData = {
  results: [
    {
      category_a: 30,
      category_b: 20,
      category_c: 15,
      category_d: 10,
      category_e: 5,
    },
  ],
  timeSlots: [],
  total: 1,
};

const mockLogBarChartTimeSlots = [
  { time: 1640995200000, count: 10 },
  { time: 1640998800000, count: 15 },
  { time: 1641002400000, count: 8 },
  { time: 1641006000000, count: 20 },
];

describe('Responsive Visualization Components', () => {
  // Store original window.innerWidth
  let originalInnerWidth: number;

  beforeEach(() => {
    // Save original innerWidth
    originalInnerWidth = window.innerWidth;
    vi.clearAllMocks();
  });

  afterEach(() => {
    // Restore original innerWidth
    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      writable: true,
      value: originalInnerWidth,
    });
  });

  // Helper function to mock different screen widths
  const mockScreenWidth = (width: number) => {
    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      writable: true,
      value: width,
    });
    
    // Trigger resize event
    window.dispatchEvent(new Event('resize'));
  };

  describe('BarChartWidget Responsive Behavior', () => {
    it('uses smaller gap between bars on small screens', () => {
      // Mock small screen width
      mockScreenWidth(480);
      
      const { container } = render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);
      
      // Check for gap-0.5 class on small screens
      const chartContainer = container.querySelector('.flex.items-end.h-full');
      expect(chartContainer).toHaveClass('gap-0.5');
      expect(chartContainer).not.toHaveClass('gap-1');
    });

    it('uses normal gap between bars on larger screens', () => {
      // Mock larger screen width
      mockScreenWidth(800);
      
      const { container } = render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);
      
      // Check for gap-1 class on larger screens
      const chartContainer = container.querySelector('.flex.items-end.h-full');
      expect(chartContainer).toHaveClass('gap-1');
      expect(chartContainer).not.toHaveClass('gap-0.5');
    });

    it('uses smaller font size for labels on small screens', () => {
      // Mock small screen width
      mockScreenWidth(480);
      
      const { container } = render(<BarChartWidget data={mockTimeSlotData} widget={mockWidget} />);
      
      // Check for text-[10px] class on small screens
      const labels = container.querySelectorAll('.text-muted-foreground.mt-1');
      labels.forEach(label => {
        expect(label).toHaveClass('text-[10px]');
        expect(label).not.toHaveClass('text-xs');
      });
    });

  });

  describe('PieChartWidget Responsive Behavior', () => {
    it('uses normal SVG size on larger screens', () => {
      // Mock larger screen width
      mockScreenWidth(800);
      
      const { container } = render(<PieChartWidget data={mockPieData} widget={mockWidget} />);
      
      // Check for normal SVG size on larger screens
      const svg = container.querySelector('svg');
      expect(svg).toHaveAttribute('width', '200');
      expect(svg).toHaveAttribute('height', '200');
    });

    it('uses vertical layout on small screens', () => {
      // Mock small screen width
      mockScreenWidth(480);
      
      const { container } = render(<PieChartWidget data={mockPieData} widget={mockWidget} />);
      
      // Check for flex-col class on small screens
      const layout = container.querySelector('.flex-1.flex.items-center.justify-center > div');
      expect(layout).toHaveClass('flex-col');
      expect(layout).not.toHaveClass('space-x-4');
      expect(layout).toHaveClass('space-y-4');
    });

    it('uses horizontal layout on larger screens', () => {
      // Mock larger screen width
      mockScreenWidth(800);
      
      const { container } = render(<PieChartWidget data={mockPieData} widget={mockWidget} />);
      
      // Check for horizontal layout on larger screens
      const layout = container.querySelector('.flex-1.flex.items-center.justify-center > div');
      expect(layout).not.toHaveClass('flex-col');
      expect(layout).toHaveClass('space-x-4');
      expect(layout).not.toHaveClass('space-y-4');
    });

    it('uses grid layout for legend on small screens', () => {
      // Mock small screen width
      mockScreenWidth(480);
      
      const { container } = render(<PieChartWidget data={mockPieData} widget={mockWidget} />);
      
      // Check for grid layout on small screens
      const legend = container.querySelector('.flex-1.flex.items-center.justify-center > div > div:nth-child(2)');
      expect(legend).toHaveClass('grid');
      expect(legend).toHaveClass('grid-cols-2');
    });
  });

  describe('LineChartWidget Responsive Behavior', () => {
    it('uses smaller margins on small screens', async () => {
      // Mock small screen width
      mockScreenWidth(480);
      
      render(<LineChartWidget data={mockTimeSlotData} widget={mockWidget} />);
      
      // Check for smaller font size in summary
      const summary = screen.getByText(/data points/);
      expect(summary).toHaveClass('text-[10px]');
      expect(summary).not.toHaveClass('text-xs');
    });

    it('uses normal font size on larger screens', () => {
      // Mock larger screen width
      mockScreenWidth(800);
      
      render(<LineChartWidget data={mockTimeSlotData} widget={mockWidget} />);
      
      // Check for normal font size in summary
      const summary = screen.getByText(/data points/);
      expect(summary).toHaveClass('text-xs');
      expect(summary).not.toHaveClass('text-[10px]');
    });
  });

  describe('AreaChartWidget Responsive Behavior', () => {
    it('uses smaller font size on small screens', () => {
      // Mock small screen width
      mockScreenWidth(480);
      
      render(<AreaChartWidget data={mockTimeSlotData} widget={mockWidget} />);
      
      // Check for smaller font size in summary
      const summary = screen.getByText(/data points/);
      expect(summary).toHaveClass('text-[10px]');
      expect(summary).not.toHaveClass('text-xs');
    });

    it('uses normal font size on larger screens', () => {
      // Mock larger screen width
      mockScreenWidth(800);
      
      render(<AreaChartWidget data={mockTimeSlotData} widget={mockWidget} />);
      
      // Check for normal font size in summary
      const summary = screen.getByText(/data points/);
      expect(summary).toHaveClass('text-xs');
      expect(summary).not.toHaveClass('text-[10px]');
    });
  });

});