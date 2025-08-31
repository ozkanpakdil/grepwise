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

// Mock html2canvas
vi.mock('html2canvas', () => ({
  default: vi.fn().mockResolvedValue({
    toDataURL: vi.fn().mockReturnValue('mock-image-data-url'),
    height: 500,
    width: 800,
  }),
}));

// Mock jsPDF
vi.mock('jspdf', () => {
  const mockSave = vi.fn();
  const mockJsPdf = vi.fn().mockImplementation(() => ({
    setFontSize: vi.fn(),
    text: vi.fn(),
    addImage: vi.fn(),
    save: mockSave,
  }));

  // Expose the mock save function on the constructor for testing
  mockJsPdf.mockSave = mockSave;

  return {
    default: mockJsPdf,
  };
});

// Create a global mock link that can be accessed in tests
const mockLink = {
  click: vi.fn(),
  download: '',
  href: '',
};

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
  default: ({ children }: { children: React.ReactNode }) => <div data-testid="grid-layout">{children}</div>,
}));

describe('Dashboard Export', () => {
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

    // Mock URL.createObjectURL and URL.revokeObjectURL
    global.URL.createObjectURL = vi.fn().mockReturnValue('mock-blob-url');
    global.URL.revokeObjectURL = vi.fn();

    // Reset the mock link for each test
    mockLink.click.mockClear();
    mockLink.download = '';
    mockLink.href = '';

    // Store the original createElement function
    const originalCreateElement = document.createElement.bind(document);

    // Mock document.createElement to return our global mock link for 'a' tags
    global.document.createElement = vi.fn().mockImplementation((tag) => {
      if (tag === 'a') return mockLink;
      return originalCreateElement(tag);
    });
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

  it('renders export button in dashboard view', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    expect(screen.getByText('Export')).toBeInTheDocument();
  });

  it('opens export modal when export button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Export'));

    expect(screen.getByText('Export Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Choose a format to export your dashboard')).toBeInTheDocument();
  });

  it('shows export options in the modal', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Export'));

    expect(screen.getByText('PDF Document')).toBeInTheDocument();
    expect(screen.getByText('PNG Image')).toBeInTheDocument();
    expect(screen.getByText('JSON Data')).toBeInTheDocument();
  });

  it('exports dashboard as JSON when JSON option is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Export'));

    await waitFor(() => {
      expect(screen.getByText('Export Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('JSON Data'));

    // Check that a Blob was created with the dashboard data
    expect(global.URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob));

    // Check that a download link was created and clicked
    const mockLink = document.createElement('a');
    expect(mockLink.download).toContain('dashboard-test-dashboard.json');
    expect(mockLink.click).toHaveBeenCalled();

    // Check that URL.revokeObjectURL was called to clean up
    expect(global.URL.revokeObjectURL).toHaveBeenCalledWith('mock-blob-url');

    // Wait for the export process to complete
    await waitFor(() => {
      // Check that success toast was shown
      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Success',
          description: 'Dashboard data exported as JSON',
        })
      );
    });
  });

  it('closes export modal when cancel button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Export'));

    await waitFor(() => {
      expect(screen.getByText('Export Dashboard')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel'));

    await waitFor(() => {
      expect(screen.queryByText('Export Dashboard')).not.toBeInTheDocument();
    });
  });
});
