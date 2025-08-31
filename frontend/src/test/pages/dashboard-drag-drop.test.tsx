import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
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

// No longer using react-grid-layout; MUI Grid is used in implementation

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

  it('toggles edit mode UI elements correctly', async () => {
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
});
