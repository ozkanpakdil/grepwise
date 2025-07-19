import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Layout from '@/components/layout';
import { BrowserRouter } from 'react-router-dom';
import React from 'react';

// Mock the theme provider
vi.mock('@/components/theme-provider', () => ({
  useTheme: vi.fn().mockReturnValue({
    theme: 'dark',
    setTheme: vi.fn(),
  }),
}));

// Mock the auth store
vi.mock('@/store/auth-store', () => ({
  useAuthStore: vi.fn().mockReturnValue({
    user: { username: 'testuser', email: 'test@example.com' },
    logout: vi.fn(),
  }),
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

  it('renders navigation links', () => {
    render(<LayoutWithRouter />);
    
    // Check for navigation links
    expect(screen.getByText('Search')).toBeInTheDocument();
    expect(screen.getByText('Dashboards')).toBeInTheDocument();
    expect(screen.getByText('Alarms')).toBeInTheDocument();
    expect(screen.getByText('Settings')).toBeInTheDocument();
    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(screen.getByText('Roles')).toBeInTheDocument();
  });

  it('highlights the active navigation link', () => {
    // Mock the useLocation to return /search
    vi.mocked(vi.importMock('react-router-dom').useLocation).mockReturnValue({ pathname: '/search' });
    
    render(<LayoutWithRouter />);
    
    // The Search link should have the active class
    const searchLink = screen.getByText('Search').closest('a');
    expect(searchLink).toHaveClass('text-foreground');
    expect(searchLink).not.toHaveClass('text-muted-foreground');
    
    // Other links should not have the active class
    const dashboardsLink = screen.getByText('Dashboards').closest('a');
    expect(dashboardsLink).toHaveClass('text-muted-foreground');
    expect(dashboardsLink).not.toHaveClass('text-foreground');
  });

  it('renders the theme toggle button', () => {
    render(<LayoutWithRouter />);
    
    // Check for the theme toggle button
    const themeToggleButton = screen.getByTitle('Switch to light theme');
    expect(themeToggleButton).toBeInTheDocument();
  });

  it('renders user information when user is logged in', () => {
    render(<LayoutWithRouter />);
    
    // Check for user information
    expect(screen.getByText('testuser')).toBeInTheDocument();
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
  });

  it('renders logout button when user is logged in', async () => {
    const user = userEvent.setup();
    const { useAuthStore } = vi.importMock('@/store/auth-store');
    const mockLogout = vi.fn();
    
    // Update the mock to include the logout function
    useAuthStore.mockReturnValue({
      user: { username: 'testuser', email: 'test@example.com' },
      logout: mockLogout,
    });
    
    render(<LayoutWithRouter />);
    
    // Check for logout button
    const logoutButton = screen.getByTitle('Logout');
    expect(logoutButton).toBeInTheDocument();
    
    // Click the logout button
    await user.click(logoutButton);
    
    // Check that logout was called
    expect(mockLogout).toHaveBeenCalled();
  });

  it('toggles the theme when theme button is clicked', async () => {
    const user = userEvent.setup();
    const { useTheme } = vi.importMock('@/components/theme-provider');
    const mockSetTheme = vi.fn();
    
    // Update the mock to include the setTheme function
    useTheme.mockReturnValue({
      theme: 'dark',
      setTheme: mockSetTheme,
    });
    
    render(<LayoutWithRouter />);
    
    // Check for theme toggle button
    const themeToggleButton = screen.getByTitle('Switch to light theme');
    
    // Click the theme toggle button
    await user.click(themeToggleButton);
    
    // Check that setTheme was called with 'light'
    expect(mockSetTheme).toHaveBeenCalledWith('light');
  });

  it('renders the mobile menu button on small screens', () => {
    // Mock window.matchMedia to simulate small screen
    window.matchMedia = vi.fn().mockImplementation(query => ({
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

  it('toggles the mobile menu when menu button is clicked', async () => {
    const user = userEvent.setup();
    
    // Mock window.matchMedia to simulate small screen
    window.matchMedia = vi.fn().mockImplementation(query => ({
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
    
    // Initially, mobile menu should be closed
    expect(screen.queryByText('Menu')).not.toBeInTheDocument();
    
    // Click the menu button to open the menu
    await user.click(menuButton);
    
    // Now the mobile menu should be visible
    expect(screen.getByText('Menu')).toBeInTheDocument();
    
    // Click the close button to close the menu
    const closeButton = screen.getByLabelText('Close menu');
    await user.click(closeButton);
    
    // Mobile menu should be closed again
    expect(screen.queryByText('Menu')).not.toBeInTheDocument();
  });

  it('renders the footer with copyright information', () => {
    render(<LayoutWithRouter />);
    
    // Check for footer content
    const currentYear = new Date().getFullYear();
    expect(screen.getByText(`© ${currentYear} GrepWise. All rights reserved.`)).toBeInTheDocument();
    expect(screen.getByText('An open-source alternative to Splunk')).toBeInTheDocument();
  });

  it('does not render user information when user is not logged in', () => {
    const { useAuthStore } = vi.importMock('@/store/auth-store');
    
    // Update the mock to simulate no user logged in
    useAuthStore.mockReturnValue({
      user: null,
      logout: vi.fn(),
    });
    
    render(<LayoutWithRouter />);
    
    // User information should not be present
    expect(screen.queryByText('testuser')).not.toBeInTheDocument();
    expect(screen.queryByText('test@example.com')).not.toBeInTheDocument();
  });
});