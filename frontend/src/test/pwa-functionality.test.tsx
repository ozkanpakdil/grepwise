import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from '@/App';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from '@/components/theme-provider';

// Create a client for React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

// Mock service worker registration
const mockServiceWorkerRegistration = {
  update: vi.fn(),
  unregister: vi.fn(),
  addEventListener: vi.fn(),
};

// Mock navigator.serviceWorker
Object.defineProperty(window, 'navigator', {
  value: {
    ...window.navigator,
    serviceWorker: {
      register: vi.fn().mockResolvedValue(mockServiceWorkerRegistration),
      getRegistration: vi.fn().mockResolvedValue(mockServiceWorkerRegistration),
      ready: Promise.resolve(mockServiceWorkerRegistration),
    },
  },
  writable: true,
});

// Mock window.matchMedia for offline detection tests
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false, // Default to online
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Test wrapper component
const TestWrapper = ({ children }: { children: React.ReactNode }) => (
  <BrowserRouter>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider defaultTheme="system" storageKey="grepwise-theme">
        {children}
      </ThemeProvider>
    </QueryClientProvider>
  </BrowserRouter>
);

describe('PWA Functionality', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('should handle offline mode gracefully', async () => {
    // Mock navigator.onLine to simulate offline mode
    Object.defineProperty(window.navigator, 'onLine', {
      writable: true,
      value: false,
    });

    // Mock matchMedia to simulate offline mode
    window.matchMedia = vi.fn().mockImplementation(query => ({
      matches: query === '(display-mode: standalone)',
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }));

    // Mock fetch to simulate network error
    global.fetch = vi.fn().mockRejectedValue(new Error('Network error'));

    // Render the app
    render(
      <TestWrapper>
        <App />
      </TestWrapper>
    );

    // The app should render without crashing in offline mode
    // This is a basic test to ensure the app doesn't crash when offline
    expect(document.body).toBeDefined();
  });
});