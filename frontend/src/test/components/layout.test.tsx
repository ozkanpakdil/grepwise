import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import Layout from '@/components/layout';
import { BrowserRouter } from 'react-router-dom';

// Mock the theme provider
vi.mock('@/components/theme-provider', () => ({
  useTheme: vi.fn().mockReturnValue({
    theme: 'dark',
    setTheme: vi.fn(),
  }),
}));

// Mock the auth state util
vi.mock('@/api/http', () => ({
  getAuthState: () => ({ state: { user: { username: 'testuser', email: 'test@example.com' } } }),
  clearAuthState: vi.fn(),
}));

// Mock the react-router-dom hooks
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useLocation: vi.fn().mockReturnValue({ pathname: '/search' }),
  };
});

// Helper component to wrap Layout with router
const LayoutWithRouter = () => (
  <BrowserRouter>
    <Layout />
  </BrowserRouter>
);

describe('Layout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the header with logo', () => {
    render(<LayoutWithRouter />);

    // Check for the logo/title
    expect(screen.getByText('GrepWise')).toBeInTheDocument();
  });

  it('renders the theme toggle button', () => {
    render(<LayoutWithRouter />);

    // Check for the theme toggle button
    const themeToggleButton = screen.getByTitle('Switch to light theme');
    expect(themeToggleButton).toBeInTheDocument();
  });

  it('renders the mobile menu button on small screens', () => {
    // Mock window.matchMedia to simulate small screen
    window.matchMedia = vi.fn().mockImplementation((query) => ({
      matches: query === '(max-width: 768px)',
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }));

    render(<LayoutWithRouter />);

    // Check for mobile menu button
    const menuButton = screen.getByLabelText('Toggle menu');
    expect(menuButton).toBeInTheDocument();
  });

  it('renders the footer with copyright information', () => {
    render(<LayoutWithRouter />);

    // Check for footer content
    const currentYear = new Date().getFullYear();
    expect(screen.getByText(`© ${currentYear} GrepWise. All rights reserved.`)).toBeInTheDocument();
    expect(screen.getByText('An open-source alternative to Splunk')).toBeInTheDocument();
  });
});
