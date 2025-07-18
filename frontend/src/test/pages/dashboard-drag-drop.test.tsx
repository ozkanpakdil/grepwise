import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import DashboardView from '@/pages/dashboard-view';
import { dashboardApi } from '@/api/dashboard';

// Mock the dashboard API
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    getDashboard: vi.fn(),
    deleteWidget: vi.fn(),
    updateWidgetPositions: vi.fn(),
  },
}));

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock toast hook
const mockToast = vi.fn();
vi.mock('@/components/ui/use-toast', () => ({
  useToast: () => ({ toast: mockToast }),
}));

// Mock WidgetRenderer component
vi.mock('@/components/WidgetRenderer', () => ({
  default: ({ widget }: { widget: any }) => (
    <div data-testid={`widget-${widget.id}`}>
      <div>{widget.title}</div>
      <div>{widget.type}</div>
    </div>
  ),
}));

// Mock GridLayout component and its onLayoutChange behavior
let mockLayoutChangeHandler: ((layout: any[]) => void) | null = null;
vi.mock('react-grid-layout', () => ({
  default: ({ children, onLayoutChange }: { children: React.ReactNode, onLayoutChange: (layout: any[]) => void }) => {
    // Store the layout change handler so we can call it in tests
    mockLayoutChangeHandler = onLayoutChange;
    return (
      <div data-testid="grid-layout">
        {children}
        <button 
          data-testid="simulate-layout-change"
          onClick={() => {
            if (mockLayoutChangeHandler) {
              mockLayoutChangeHandler([
                { i: 'w1', x: 2, y: 1, w: 6, h: 4 }, // Changed position
                { i: 'w2', x: 0, y: 0, w: 4, h: 3 }  // Original position
              ]);
            }
          }}
        >
          Simulate Layout Change
        </button>
      </div>
    );
  },
}));

describe('Dashboard Drag and Drop', () => {
  const mockDashboard = {
    id: '1',
    name: 'Test Dashboard',
    description: 'Test dashboard description',
    createdBy: 'current-user',
    createdAt: '2024-01-01T00:00:00Z',
    isShared: false,
    widgets: [
      {
        id: 'w1',
        title: 'Chart Widget',
        type: 'chart',
        query: 'search *',
        positionX: 0,
        positionY: 0,
        width: 6,
        height: 4,
        userId: 'current-user',
      },
      {
        id: 'w2',
        title: 'Table Widget',
        type: 'table',
        query: 'search error',
        positionX: 0,
        positionY: 0,
        width: 4,
        height: 3,
        userId: 'current-user',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(mockDashboard);
    vi.mocked(dashboardApi.updateWidgetPositions).mockResolvedValue(undefined);
  });

  const renderWithRouter = (dashboardId = '1') => {
    return render(
      <MemoryRouter initialEntries={[`/dashboards/${dashboardId}`]}>
        <Routes>
          <Route path="/dashboards/:id" element={<DashboardView />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders dashboard with GridLayout component', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    expect(screen.getByTestId('grid-layout')).toBeInTheDocument();
    expect(screen.getByTestId('widget-w1')).toBeInTheDocument();
    expect(screen.getByTestId('widget-w2')).toBeInTheDocument();
  });

  it('enables drag and drop only in edit mode', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode
    await user.click(screen.getByText('Edit'));
    
    // Check that GridLayout was rendered with isDraggable and isResizable set to true
    // (We can't directly test these props, but we can verify edit mode is active)
    expect(screen.getByText('Exit Edit')).toBeInTheDocument();
  });

  it('shows Save Layout button when layout changes', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode
    await user.click(screen.getByText('Edit'));
    
    await waitFor(() => {
      expect(screen.getByText('Exit Edit')).toBeInTheDocument();
    });
    
    // Simulate a layout change
    await user.click(screen.getByTestId('simulate-layout-change'));
    
    // Check that Save Layout button appears
    await waitFor(() => {
      expect(screen.getByText('Save Layout')).toBeInTheDocument();
    });
  });

  it('saves layout changes when Save Layout button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode
    await user.click(screen.getByText('Edit'));
    
    await waitFor(() => {
      expect(screen.getByText('Exit Edit')).toBeInTheDocument();
    });
    
    // Simulate a layout change
    await user.click(screen.getByTestId('simulate-layout-change'));
    
    await waitFor(() => {
      expect(screen.getByText('Save Layout')).toBeInTheDocument();
    });
    
    // Click Save Layout button
    await user.click(screen.getByText('Save Layout'));
    
    // Check that updateWidgetPositions was called with the correct parameters
    expect(dashboardApi.updateWidgetPositions).toHaveBeenCalledWith(
      '1',
      {
        w1: { positionX: 2, positionY: 1, width: 6, height: 4 },
        w2: { positionX: 0, positionY: 0, width: 4, height: 3 }
      },
      'current-user'
    );
    
    // Wait for the save process to complete
    await waitFor(() => {
      // Check that success toast was shown
      expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Success',
        description: 'Dashboard layout saved successfully',
      }));
    });
  });

  it('initializes layout from dashboard widgets', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Simulate a layout change to verify the layout was initialized
    const user = userEvent.setup();
    await user.click(screen.getByText('Edit'));
    
    await waitFor(() => {
      expect(screen.getByText('Exit Edit')).toBeInTheDocument();
    });
    
    await user.click(screen.getByTestId('simulate-layout-change'));
    
    // Check that Save Layout button appears (which means layout was initialized and then changed)
    await waitFor(() => {
      expect(screen.getByText('Save Layout')).toBeInTheDocument();
    });
  });

  it('handles layout save errors gracefully', async () => {
    // Mock API to throw an error
    vi.mocked(dashboardApi.updateWidgetPositions).mockRejectedValue(new Error('Failed to save layout'));
    
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode
    await user.click(screen.getByText('Edit'));
    
    await waitFor(() => {
      expect(screen.getByText('Exit Edit')).toBeInTheDocument();
    });
    
    // Simulate a layout change
    await user.click(screen.getByTestId('simulate-layout-change'));
    
    await waitFor(() => {
      expect(screen.getByText('Save Layout')).toBeInTheDocument();
    });
    
    // Click Save Layout button
    await user.click(screen.getByText('Save Layout'));
    
    // Check that error toast was shown
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Error',
        description: 'Failed to save dashboard layout',
        variant: 'destructive',
      }));
    });
  });
});