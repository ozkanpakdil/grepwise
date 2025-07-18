import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, MemoryRouter, Routes, Route } from 'react-router-dom';
import DashboardView from '@/pages/dashboard-view';
import { dashboardApi } from '@/api/dashboard';

// Mock the dashboard API
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    getDashboard: vi.fn(),
    shareDashboard: vi.fn(),
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

// Mock clipboard API
const mockClipboardWriteText = vi.fn().mockResolvedValue(undefined);
Object.assign(navigator, {
  clipboard: {
    writeText: mockClipboardWriteText,
  },
});

// Create a spy for the clipboard writeText function
const clipboardSpy = vi.spyOn(navigator.clipboard, 'writeText');

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

describe('Dashboard Sharing', () => {
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
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(mockDashboard);
    vi.mocked(dashboardApi.shareDashboard).mockImplementation((id, isShared) => 
      Promise.resolve({
        ...mockDashboard,
        isShared,
      })
    );
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

  it('renders share button in dashboard view', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    expect(screen.getByText('Share')).toBeInTheDocument();
  });

  it('opens share modal when share button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Share'));

    expect(screen.getByText('Share Dashboard')).toBeInTheDocument();
    expect(screen.getByText('This dashboard is currently private. Enable sharing to generate a link.')).toBeInTheDocument();
  });

  it('enables sharing when enable button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Share'));
    
    await waitFor(() => {
      expect(screen.getByText('Share Dashboard')).toBeInTheDocument();
    });
    
    await user.click(screen.getByText('Enable Sharing'));

    expect(dashboardApi.shareDashboard).toHaveBeenCalledWith('1', true, 'current-user');
    
    // Wait for the API call to complete and UI to update
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Success',
        description: expect.stringContaining('shared'),
      }));
    });
  });

  it('shows share link when dashboard is shared', async () => {
    // Mock dashboard as already shared
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue({
      ...mockDashboard,
      isShared: true,
    });

    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Shared'));

    expect(screen.getByText('This dashboard is currently shared. Anyone with the link can view it.')).toBeInTheDocument();
    expect(screen.getByText('Share Link')).toBeInTheDocument();
    
    // Check that the input contains the correct URL
    const shareInput = screen.getByDisplayValue(new RegExp(`/dashboards/1$`));
    expect(shareInput).toBeInTheDocument();
  });


  it('disables sharing when disable button is clicked', async () => {
    // Mock dashboard as already shared
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue({
      ...mockDashboard,
      isShared: true,
    });

    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Shared'));
    await user.click(screen.getByText('Disable Sharing'));

    expect(dashboardApi.shareDashboard).toHaveBeenCalledWith('1', false, 'current-user');
    
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Success',
        description: expect.stringContaining('no longer shared'),
      }));
    });
  });

  it('closes share modal when close button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Share'));
    await user.click(screen.getByText('Close'));

    expect(screen.queryByText('Share Dashboard')).not.toBeInTheDocument();
  });
});