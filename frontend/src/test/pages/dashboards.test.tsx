import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import DashboardsPage from '@/pages/dashboards';
import { dashboardApi } from '@/api/dashboard';

// Mock the dashboard API
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    getDashboards: vi.fn(),
    createDashboard: vi.fn(),
    deleteDashboard: vi.fn(),
    shareDashboard: vi.fn(),
    addWidget: vi.fn(),
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

// Test wrapper component
const TestWrapper = ({ children }: { children: React.ReactNode }) => <BrowserRouter>{children}</BrowserRouter>;

describe('DashboardsPage', () => {
  const mockDashboards = [
    {
      id: '1',
      name: 'Test Dashboard 1',
      description: 'Test description 1',
      createdBy: 'current-user',
      createdAt: '2024-01-01T00:00:00Z',
      isShared: false,
      widgets: [
        {
          id: 'w1',
          title: 'Test Widget',
          type: 'chart',
          query: 'search *',
          positionX: 0,
          positionY: 0,
          width: 4,
          height: 3,
          userId: 'current-user',
        },
      ],
    },
    {
      id: '2',
      name: 'Test Dashboard 2',
      description: 'Test description 2',
      createdBy: 'current-user',
      createdAt: '2024-01-02T00:00:00Z',
      isShared: true,
      widgets: [],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(dashboardApi.getDashboards).mockResolvedValue(mockDashboards);
  });

  it('renders dashboard page with title and description', async () => {
    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // Wait for component to fully render and any state updates to complete
    await waitFor(() => {
      expect(screen.getByText('Dashboards')).toBeInTheDocument();
      expect(screen.getByText('Create and manage your log analysis dashboards')).toBeInTheDocument();
      expect(screen.getByText('New Dashboard')).toBeInTheDocument();
    });
  });

  it('loads and displays dashboards on mount', async () => {
    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    await waitFor(() => {
      expect(dashboardApi.getDashboards).toHaveBeenCalledWith('current-user');
    });

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard 1')).toBeInTheDocument();
      expect(screen.getByText('Test Dashboard 2')).toBeInTheDocument();
      expect(screen.getByText('Test description 1')).toBeInTheDocument();
      expect(screen.getByText('Test description 2')).toBeInTheDocument();
    });
  });

  it('shows loading state initially', async () => {
    // Mock API to never resolve during this test
    vi.mocked(dashboardApi.getDashboards).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolving promise
    );

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // Check for loading state
    expect(screen.getByText('Loading dashboards...')).toBeInTheDocument();
  });

  it('displays widget count for each dashboard', async () => {
    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // First wait for dashboards to load
    await waitFor(() => {
      expect(screen.getByText('Test Dashboard 1')).toBeInTheDocument();
    });

    // Then check for widget counts
    await waitFor(() => {
      expect(screen.getByText('1 widgets')).toBeInTheDocument();
      expect(screen.getByText('0 widgets')).toBeInTheDocument();
    });
  });

  it('opens create dashboard modal when New Dashboard button is clicked', async () => {
    const user = userEvent.setup();

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('New Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('New Dashboard'));

    // Wait for the modal to appear
    await waitFor(() => {
      expect(screen.getByText('Create New Dashboard')).toBeInTheDocument();
    });

    // Check modal contents
    expect(screen.getByPlaceholderText('Enter dashboard name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter dashboard description')).toBeInTheDocument();
  });

  it('creates a new dashboard when form is submitted', async () => {
    const user = userEvent.setup();
    vi.mocked(dashboardApi.createDashboard).mockResolvedValue(undefined);

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('New Dashboard')).toBeInTheDocument();
    });

    // Open modal
    await user.click(screen.getByText('New Dashboard'));

    // Wait for the modal to appear
    await waitFor(() => {
      expect(screen.getByText('Create New Dashboard')).toBeInTheDocument();
    });

    // Fill form
    await user.type(screen.getByPlaceholderText('Enter dashboard name'), 'New Test Dashboard');
    await user.type(screen.getByPlaceholderText('Enter dashboard description'), 'New test description');

    // Submit form
    await user.click(screen.getByText('Create Dashboard'));

    // Check that the API was called with the correct parameters
    await waitFor(() => {
      expect(dashboardApi.createDashboard).toHaveBeenCalledWith({
        name: 'New Test Dashboard',
        description: 'New test description',
        createdBy: 'current-user',
      });
    });

    // Wait for the success toast
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'Success',
        description: 'Dashboard created successfully',
      });
    });
  });

  it('shows error when creating dashboard without name', async () => {
    const user = userEvent.setup();

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('New Dashboard')).toBeInTheDocument();
    });

    // Open modal
    await user.click(screen.getByText('New Dashboard'));

    // Wait for the modal to appear
    await waitFor(() => {
      expect(screen.getByText('Create New Dashboard')).toBeInTheDocument();
    });

    // Submit form without name
    await user.click(screen.getByText('Create Dashboard'));

    // Wait for the error toast
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'Error',
        description: 'Dashboard name is required',
        variant: 'destructive',
      });
    });
  });

  it('navigates to dashboard view when View button is clicked', async () => {
    const user = userEvent.setup();

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // Wait for dashboards to load
    await waitFor(() => {
      expect(screen.getByText('Test Dashboard 1')).toBeInTheDocument();
    });

    // Find the dashboard card
    const dashboardCard = screen.getByText('Test Dashboard 1').closest('.border');
    expect(dashboardCard).not.toBeNull();

    // Find the View button within the card
    const viewButton = dashboardCard ? within(dashboardCard).getByText('View') : null;
    expect(viewButton).not.toBeNull();

    if (viewButton) {
      await user.click(viewButton);
    }

    // Check that navigation was called
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/dashboards/1');
    });
  });

  it('opens add widget modal when Add Widget button is clicked', async () => {
    const user = userEvent.setup();

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // Wait for dashboards to load
    await waitFor(() => {
      expect(screen.getByText('Test Dashboard 1')).toBeInTheDocument();
    });

    // Find the dashboard card
    const dashboardCard = screen.getByText('Test Dashboard 1').closest('.border');
    expect(dashboardCard).not.toBeNull();

    // Find the Add Widget button within the card
    const addWidgetButton = dashboardCard ? within(dashboardCard).getByText('Add Widget') : null;
    expect(addWidgetButton).not.toBeNull();

    if (addWidgetButton) {
      await user.click(addWidgetButton);
    }

    // Wait for the modal to appear
    await waitFor(() => {
      expect(screen.getByText('Add Widget to Test Dashboard 1')).toBeInTheDocument();
    });

    // Check modal contents
    expect(screen.getByPlaceholderText('Enter widget title')).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText('Enter SPL query (e.g., search error | stats count by level)')
    ).toBeInTheDocument();
  });

  it('shows empty state when no dashboards exist', async () => {
    // Mock API to return empty array
    vi.mocked(dashboardApi.getDashboards).mockResolvedValue([]);

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    // First wait for the API call to complete
    await waitFor(() => {
      expect(dashboardApi.getDashboards).toHaveBeenCalled();
    });

    // Then check for empty state message
    await waitFor(() => {
      expect(screen.getByText('No dashboards found')).toBeInTheDocument();
      expect(screen.getByText('Create your first dashboard')).toBeInTheDocument();
    });
  });

  it('handles API errors gracefully', async () => {
    vi.mocked(dashboardApi.getDashboards).mockRejectedValue(new Error('API Error'));

    render(
      <TestWrapper>
        <DashboardsPage />
      </TestWrapper>
    );

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'Error',
        description: 'Failed to load dashboards',
        variant: 'destructive',
      });
    });
  });
});
