import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, MemoryRouter, Routes, Route } from 'react-router-dom';
import DashboardView from '@/pages/dashboard-view';
import { dashboardApi } from '@/api/dashboard';

// Mock the dashboard API
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    getDashboard: vi.fn(),
    deleteWidget: vi.fn(),
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

// Mock GridLayout component
vi.mock('react-grid-layout', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="grid-layout">{children}</div>
  ),
}));

describe('DashboardView', () => {
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
        positionX: 6,
        positionY: 0,
        width: 6,
        height: 4,
        userId: 'current-user',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(mockDashboard);
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

  it('renders dashboard view with title and description', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
      expect(screen.getByText('Test dashboard description')).toBeInTheDocument();
    });

    expect(screen.getByText('Back to Dashboards')).toBeInTheDocument();
    expect(screen.getByText('Edit')).toBeInTheDocument();
    expect(screen.getByText('Add Widget')).toBeInTheDocument();
  });

  it('loads dashboard data on mount', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(dashboardApi.getDashboard).toHaveBeenCalledWith('1', 'current-user');
    });
  });

  it('shows loading state initially', () => {
    // Mock API to never resolve during this test
    vi.mocked(dashboardApi.getDashboard).mockImplementationOnce(() => 
      new Promise(() => {}) // Never resolving promise
    );
    
    renderWithRouter();
    
    // Check for loading state
    expect(screen.getByText('Loading dashboard...')).toBeInTheDocument();
  });

  it('displays widgets in grid layout', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByTestId('grid-layout')).toBeInTheDocument();
      expect(screen.getByTestId('widget-w1')).toBeInTheDocument();
      expect(screen.getByTestId('widget-w2')).toBeInTheDocument();
      expect(screen.getByText('Chart Widget')).toBeInTheDocument();
      expect(screen.getByText('Table Widget')).toBeInTheDocument();
    });
  });

  it('navigates back to dashboards when back button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Back to Dashboards'));

    expect(mockNavigate).toHaveBeenCalledWith('/dashboards');
  });

  it('toggles edit mode when edit button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    // Wait for dashboard to load
    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Initially not in edit mode
    expect(screen.getByText('Edit')).toBeInTheDocument();

    // Click edit button
    await user.click(screen.getByText('Edit'));

    // Wait for state update
    await waitFor(() => {
      expect(screen.getByText('Exit Edit')).toBeInTheDocument();
    });

    // Click exit edit button
    await user.click(screen.getByText('Exit Edit'));

    // Wait for state update
    await waitFor(() => {
      expect(screen.getByText('Edit')).toBeInTheDocument();
    });
  });

  it('shows delete buttons in edit mode', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode
    await user.click(screen.getByText('Edit'));

    // Wait for state update
    await waitFor(() => {
      expect(screen.getByText('Exit Edit')).toBeInTheDocument();
    });

    // Check for delete buttons
    const deleteButtons = screen.getAllByRole('button');
    
    // Since we can't easily query for the Trash2 icon component,
    // we'll check for buttons with the expected class
    const trashButtons = deleteButtons.filter(button => 
      button.className.includes('text-red-600') || 
      button.className.includes('text-red-700')
    );

    expect(trashButtons.length).toBeGreaterThan(0); // At least one delete button
  });

  it('deletes widget when delete button is clicked and confirmed', async () => {
    const user = userEvent.setup();
    vi.mocked(dashboardApi.deleteWidget).mockResolvedValue(undefined);
    
    // Mock window.confirm
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode
    await user.click(screen.getByText('Edit'));

    // Find and click delete button for first widget
    const deleteButtons = screen.getAllByRole('button');
    const deleteButton = deleteButtons.find(button => 
      button.querySelector('.lucide-trash2')
    );
    
    if (deleteButton) {
      await user.click(deleteButton);
    }

    expect(confirmSpy).toHaveBeenCalledWith('Are you sure you want to delete this widget?');
    
    await waitFor(() => {
      expect(dashboardApi.deleteWidget).toHaveBeenCalledWith('1', 'w1', 'current-user');
    });

    expect(mockToast).toHaveBeenCalledWith({
      title: 'Success',
      description: 'Widget deleted successfully',
    });

    confirmSpy.mockRestore();
  });

  it('does not delete widget when confirmation is cancelled', async () => {
    const user = userEvent.setup();
    
    // Mock window.confirm to return false
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode
    await user.click(screen.getByText('Edit'));

    // Find and click delete button
    const deleteButtons = screen.getAllByRole('button');
    const deleteButton = deleteButtons.find(button => 
      button.querySelector('.lucide-trash2')
    );
    
    if (deleteButton) {
      await user.click(deleteButton);
    }

    expect(confirmSpy).toHaveBeenCalled();
    expect(dashboardApi.deleteWidget).not.toHaveBeenCalled();

    confirmSpy.mockRestore();
  });

  it('navigates to add widget page when Add Widget button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Add Widget'));

    expect(mockNavigate).toHaveBeenCalledWith('/dashboards/1/add-widget');
  });

  it('shows empty state when dashboard has no widgets', async () => {
    const emptyDashboard = {
      ...mockDashboard,
      widgets: [],
    };
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(emptyDashboard);

    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('No widgets in this dashboard')).toBeInTheDocument();
      expect(screen.getByText('Add your first widget')).toBeInTheDocument();
    });
  });

  it('navigates to add widget from empty state', async () => {
    const user = userEvent.setup();
    const emptyDashboard = {
      ...mockDashboard,
      widgets: [],
    };
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(emptyDashboard);

    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Add your first widget')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Add your first widget'));

    expect(mockNavigate).toHaveBeenCalledWith('/dashboards/1/add-widget');
  });

  it('handles dashboard not found error', async () => {
    vi.mocked(dashboardApi.getDashboard).mockRejectedValue(new Error('Dashboard not found'));

    renderWithRouter();

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'Error',
        description: 'Failed to load dashboard',
        variant: 'destructive',
      });
    });

    expect(mockNavigate).toHaveBeenCalledWith('/dashboards');
  });

  it('shows dashboard not found message when dashboard is null', async () => {
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(null);

    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Dashboard not found')).toBeInTheDocument();
    });
  });

  it('handles widget deletion error gracefully', async () => {
    const user = userEvent.setup();
    vi.mocked(dashboardApi.deleteWidget).mockRejectedValue(new Error('Delete failed'));
    
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    // Enter edit mode and delete widget
    await user.click(screen.getByText('Edit'));

    const deleteButtons = screen.getAllByRole('button');
    const deleteButton = deleteButtons.find(button => 
      button.querySelector('.lucide-trash2')
    );
    
    if (deleteButton) {
      await user.click(deleteButton);
    }

    // Wait for the API call to be made first
    await waitFor(() => {
      expect(dashboardApi.deleteWidget).toHaveBeenCalled();
    });
    
    // Then wait for the error toast to be shown
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Error',
        description: 'Failed to delete widget',
        variant: 'destructive',
      }));
    });

    confirmSpy.mockRestore();
  });

  it('renders dashboard without description when description is empty', async () => {
    const dashboardWithoutDescription = {
      ...mockDashboard,
      description: '',
    };
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(dashboardWithoutDescription);

    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    expect(screen.queryByText('Test dashboard description')).not.toBeInTheDocument();
  });

});