import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import {BrowserRouter} from 'react-router-dom';
import DashboardView from '@/pages/dashboard-view';
// auth state util mocked below
import {ThemeProvider} from '@/components/theme-provider';

// Mock the auth util
vi.mock('@/api/http', () => ({
  getAuthState: () => ({ state: { user: { username: 'testuser', email: 'test@example.com' } } }),
  clearAuthState: vi.fn(),
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

// No react-grid-layout in use; MUI Grid is rendered within a wrapper having data-testid="grid-layout"

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
    Object.defineProperty(window, 'innerWidth', {writable: true, configurable: true, value: width});
    Object.defineProperty(window, 'innerHeight', {writable: true, configurable: true, value: height});
    window.dispatchEvent(new Event('resize'));
}

describe('Mobile Responsiveness Tests', () => {
    beforeEach(() => {
        // No-op: auth is mocked via getAuthState in '@/api/http'
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('Responsive Grid Layout', () => {
        it('renders grid layout wrapper', async () => {
            // Mock useParams to return a dashboard ID
            vi.mock('react-router-dom', async () => {
                const actual = await vi.importActual('react-router-dom');
                return {
                    ...actual,
                    useParams: () => ({id: 'test-dashboard'}),
                    useNavigate: () => vi.fn(),
                };
            });

            render(
                <BrowserRouter>
                    <ThemeProvider defaultTheme="light" storageKey="theme">
                        <DashboardView/>
                    </ThemeProvider>
                </BrowserRouter>
            );

            // Wait for dashboard to load
            await screen.findByText('Test Dashboard');

            // Check that the grid layout wrapper is rendered
            expect(screen.getByTestId('grid-layout')).toBeInTheDocument();

            // Check that the widget is rendered
            expect(screen.getByTestId('mock-widget')).toBeInTheDocument();
        });
    });
});