import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import HeatmapWidget from '@/components/widgets/HeatmapWidget';
import React from 'react';
import { DashboardWidget } from '@/api/dashboard';

// Mock useState for screen width
vi.mock('react', async () => {
  const actual = await vi.importActual('react');
  return {
    ...actual,
    useState: vi.fn(),
  };
});

describe('HeatmapWidget', () => {
  // Mock data
  const mockMultiDimensionalData = {
    results: [
      { dayOfWeek: 'Monday', hour: '00-06', count: 5 },
      { dayOfWeek: 'Monday', hour: '06-12', count: 10 },
      { dayOfWeek: 'Monday', hour: '12-18', count: 15 },
      { dayOfWeek: 'Monday', hour: '18-24', count: 8 },
      { dayOfWeek: 'Tuesday', hour: '00-06', count: 3 },
      { dayOfWeek: 'Tuesday', hour: '06-12', count: 12 },
      { dayOfWeek: 'Tuesday', hour: '12-18', count: 18 },
      { dayOfWeek: 'Tuesday', hour: '18-24', count: 7 },
      { dayOfWeek: 'Wednesday', hour: '00-06', count: 4 },
      { dayOfWeek: 'Wednesday', hour: '06-12', count: 9 },
      { dayOfWeek: 'Wednesday', hour: '12-18', count: 14 },
      { dayOfWeek: 'Wednesday', hour: '18-24', count: 6 },
    ],
    total: 12,
  };

  const mockConfiguredData = {
    results: [
      { source: 'App1', level: 'ERROR', value: 25 },
      { source: 'App1', level: 'WARN', value: 15 },
      { source: 'App1', level: 'INFO', value: 50 },
      { source: 'App2', level: 'ERROR', value: 10 },
      { source: 'App2', level: 'WARN', value: 20 },
      { source: 'App2', level: 'INFO', value: 60 },
      { source: 'App3', level: 'ERROR', value: 5 },
      { source: 'App3', level: 'WARN', value: 12 },
      { source: 'App3', level: 'INFO', value: 45 },
    ],
    total: 9,
  };

  const mockWidget: DashboardWidget = {
    id: 'widget1',
    dashboardId: 'dashboard1',
    title: 'Test Heatmap',
    type: 'heatmap',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    createdAt: Date.now(),
    updatedAt: Date.now(),
  };

  const mockWidgetWithConfig: DashboardWidget = {
    ...mockWidget,
    configuration: {
      rowField: 'source',
      colField: 'level',
      valueField: 'value',
    },
  };

  // Mock implementation for useState to return desktop width (1024px)
  const mockUseStateForDesktop = () => {
    (React.useState as jest.Mock).mockImplementation((init) => {
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
    (React.useState as jest.Mock).mockImplementation((init) => {
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
    render(<HeatmapWidget data={null} widget={mockWidget} />);
    
    expect(screen.getByText('No data available for heatmap visualization')).toBeInTheDocument();
  });

  it('renders a message when data is empty', () => {
    render(<HeatmapWidget data={{ results: [] }} widget={mockWidget} />);
    
    expect(screen.getByText('No data available for heatmap visualization')).toBeInTheDocument();
  });

  it('renders heatmap with auto-detected fields', () => {
    render(<HeatmapWidget data={mockMultiDimensionalData} widget={mockWidget} />);
    
    // Check for heatmap grid
    expect(screen.getByText('Monday')).toBeInTheDocument();
    expect(screen.getByText('Tuesday')).toBeInTheDocument();
    expect(screen.getByText('Wednesday')).toBeInTheDocument();
    expect(screen.getByText('00-06')).toBeInTheDocument();
    expect(screen.getByText('06-12')).toBeInTheDocument();
    
    // Check for legend
    expect(screen.getByText('Low')).toBeInTheDocument();
    expect(screen.getByText('High')).toBeInTheDocument();
    
    // Check for summary text
    expect(screen.getByText('3 × 4 grid • Max: 18')).toBeInTheDocument();
  });

  it('renders heatmap with configured fields', () => {
    render(<HeatmapWidget data={mockConfiguredData} widget={mockWidgetWithConfig} />);
    
    // Check for heatmap grid with configured fields
    expect(screen.getByText('App1')).toBeInTheDocument();
    expect(screen.getByText('App2')).toBeInTheDocument();
    expect(screen.getByText('App3')).toBeInTheDocument();
    expect(screen.getByText('ERROR')).toBeInTheDocument();
    expect(screen.getByText('WARN')).toBeInTheDocument();
    expect(screen.getByText('INFO')).toBeInTheDocument();
    
    // Check for summary text
    expect(screen.getByText('3 × 3 grid • Max: 60')).toBeInTheDocument();
  });

  it('adapts to small screen sizes', () => {
    // Mock useState to return mobile width
    mockUseStateForMobile();
    
    render(<HeatmapWidget data={mockMultiDimensionalData} widget={mockWidget} />);
    
    // Check for summary text with smaller font
    const summaryElement = screen.getByText('3 × 4 grid • Max: 18');
    expect(summaryElement.className).toContain('text-[10px]');
  });

  it('falls back to demo data when no valid data is provided', () => {
    // Provide data that doesn't have the expected structure
    const invalidData = {
      results: [
        { singleDimension: 'value1' },
      ],
    };
    
    render(<HeatmapWidget data={invalidData} widget={mockWidget} />);
    
    // Should show the demo data
    expect(screen.getByText('Row 1')).toBeInTheDocument();
    expect(screen.getByText('Col A')).toBeInTheDocument();
  });
});