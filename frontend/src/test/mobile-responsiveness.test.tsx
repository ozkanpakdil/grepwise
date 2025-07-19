import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi } from 'vitest';
import Layout from '@/components/layout';
import DashboardView from '@/pages/dashboard-view';
import { useAuthStore } from '@/store/auth-store';
import { ThemeProvider } from '@/components/theme-provider';

// Mock the auth store
vi.mock('@/store/auth-store', () => ({
  useAuthStore: vi.fn(),
}));

// Mock the dashboard API
vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    getDashboard: vi.fn().mockResolvedValue({
      id: 'test-dashboard',
      name: 'Test Dashboard',
      description: 'A test dashboard',
      isShared: false,
      widgets: [
        {
          id: 'widget1',
          title: 'Widget 1',
          type: 'bar',
          query: 'test query',
          positionX: 0,
          positionY: 0,
          width: 4,
          height: 3,
        },
      ],
    }),
    shareDashboard: vi.fn().mockResolvedValue({
      id: 'test-dashboard',
      name: 'Test Dashboard',
      description: 'A test dashboard',
      isShared: true,
      widgets: [],
    }),
    deleteWidget: vi.fn().mockResolvedValue({}),
    updateWidgetPositions: vi.fn().mockResolvedValue({}),
  },
}));

// Mock react-grid-layout
vi.mock('react-grid-layout', () => ({
  Responsive: vi.fn(() => <div data-testid="mock-grid">Grid Layout</div>),
  WidthProvider: vi.fn((Component) => Component),
}));

// Mock WidgetRenderer
vi.mock('@/components/WidgetRenderer', () => ({
  default: vi.fn(() => <div data-testid="mock-widget">Widget</div>),
}));

// Mock html2canvas and jsPDF
vi.mock('html2canvas', () => ({
  default: vi.fn().mockResolvedValue({
    toDataURL: vi.fn().mockReturnValue('mock-image-data'),
  }),
}));

vi.mock('jspdf', () => ({
  default: vi.fn().mockImplementation(() => ({
    text: vi.fn(),
    addImage: vi.fn(),
    save: vi.fn(),
  })),
}));

// Helper function to set viewport size
function setViewportSize(width: number, height: number) {
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: width });
  Object.defineProperty(window, 'innerHeight', { writable: true, configurable: true, value: height });
  window.dispatchEvent(new Event('resize'));
}

describe('Mobile Responsiveness Tests', () => {
  beforeEach(() => {
    // Mock the auth store to return a logged-in user
    (useAuthStore as any).mockReturnValue({
      user: { username: 'testuser', email: 'test@example.com' },
      logout: vi.fn(),
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Layout Component', () => {
    test('shows hamburger menu on mobile screens', () => {
      setViewportSize(375, 667); // iPhone SE size
      
      render(
        <BrowserRouter>
          <ThemeProvider defaultTheme="light" storageKey="theme">
            <Layout />
          </ThemeProvider>
        </BrowserRouter>
      );
      
      // Check that the hamburger menu button is visible
      const menuButton = screen.getByLabelText('Toggle menu');
      expect(menuButton).toBeInTheDocument();
      
      // Click the menu button to open the mobile menu
      fireEvent.click(menuButton);
      
      // Check that the mobile menu is now visible
      const mobileMenu = screen.getByText('Menu');
      expect(mobileMenu).toBeInTheDocument();
      
      // Check that navigation items are in the mobile menu
      expect(screen.getByText('Search')).toBeInTheDocument();
      expect(screen.getByText('Dashboards')).toBeInTheDocument();
      expect(screen.getByText('Alarms')).toBeInTheDocument();
      
      // Close the menu
      const closeButton = screen.getByLabelText('Close menu');
      fireEvent.click(closeButton);
    });
    
    test('shows full navigation on desktop screens', () => {
      setViewportSize(1024, 768); // Desktop size
      
      render(
        <BrowserRouter>
          <ThemeProvider defaultTheme="light" storageKey="theme">
            <Layout />
          </ThemeProvider>
        </BrowserRouter>
      );
      
      // Check that the navigation items are directly visible (not in a menu)
      const navItems = screen.getAllByText(/Search|Dashboards|Alarms|Settings|Users|Roles/);
      expect(navItems.length).toBeGreaterThanOrEqual(6);
      
      // Hamburger menu should not be visible
      expect(screen.queryByLabelText('Toggle menu')).not.toBeInTheDocument();
    });
  });
  
  describe('Dashboard View', () => {
    test('shows mobile actions menu on small screens', async () => {
      setViewportSize(375, 667); // iPhone SE size
      
      // Mock useParams to return a dashboard ID
      vi.mock('react-router-dom', async () => {
        const actual = await vi.importActual('react-router-dom');
        return {
          ...actual,
          useParams: () => ({ id: 'test-dashboard' }),
          useNavigate: () => vi.fn(),
        };
      });
      
      render(
        <BrowserRouter>
          <ThemeProvider defaultTheme="light" storageKey="theme">
            <DashboardView />
          </ThemeProvider>
        </BrowserRouter>
      );
      
      // Wait for dashboard to load
      await screen.findByText('Test Dashboard');
      
      // Check that the mobile menu button is visible
      const mobileMenuButton = screen.getByLabelText('More actions');
      expect(mobileMenuButton).toBeInTheDocument();
      
      // Click to open the mobile actions menu
      fireEvent.click(mobileMenuButton);
      
      // Check that the actions are in the dropdown
      expect(screen.getByText('Share')).toBeInTheDocument();
      expect(screen.getByText('Export')).toBeInTheDocument();
      expect(screen.getByText('Add Widget')).toBeInTheDocument();
    });
    
    test('shows share modal with responsive layout', async () => {
      setViewportSize(375, 667); // iPhone SE size
      
      // Mock useParams to return a dashboard ID
      vi.mock('react-router-dom', async () => {
        const actual = await vi.importActual('react-router-dom');
        return {
          ...actual,
          useParams: () => ({ id: 'test-dashboard' }),
          useNavigate: () => vi.fn(),
        };
      });
      
      render(
        <BrowserRouter>
          <ThemeProvider defaultTheme="light" storageKey="theme">
            <DashboardView />
          </ThemeProvider>
        </BrowserRouter>
      );
      
      // Wait for dashboard to load
      await screen.findByText('Test Dashboard');
      
      // Open the mobile actions menu
      const mobileMenuButton = screen.getByLabelText('More actions');
      fireEvent.click(mobileMenuButton);
      
      // Click on Share in the dropdown
      fireEvent.click(screen.getByText('Share'));
      
      // Check that the share modal is visible
      expect(screen.getByText('Share Dashboard')).toBeInTheDocument();
      
      // Check that the close button is visible
      const closeButton = screen.getByLabelText('Close');
      expect(closeButton).toBeInTheDocument();
      
      // Close the modal
      fireEvent.click(closeButton);
    });
  });
  
  describe('Responsive Grid Layout', () => {
    test('renders responsive grid layout', async () => {
      // Mock useParams to return a dashboard ID
      vi.mock('react-router-dom', async () => {
        const actual = await vi.importActual('react-router-dom');
        return {
          ...actual,
          useParams: () => ({ id: 'test-dashboard' }),
          useNavigate: () => vi.fn(),
        };
      });
      
      render(
        <BrowserRouter>
          <ThemeProvider defaultTheme="light" storageKey="theme">
            <DashboardView />
          </ThemeProvider>
        </BrowserRouter>
      );
      
      // Wait for dashboard to load
      await screen.findByText('Test Dashboard');
      
      // Check that the grid layout is rendered
      expect(screen.getByTestId('mock-grid')).toBeInTheDocument();
      
      // Check that the widget is rendered
      expect(screen.getByTestId('mock-widget')).toBeInTheDocument();
    });
  });
});