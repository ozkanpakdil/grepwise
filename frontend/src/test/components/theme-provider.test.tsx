import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, useTheme } from '@/components/theme-provider';

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    clear: vi.fn(() => {
      store = {};
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
  };
})();

Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock document.documentElement for theme class testing
const documentElementClassListMock = {
  add: vi.fn(),
  remove: vi.fn(),
};

Object.defineProperty(document.documentElement, 'classList', {
  value: documentElementClassListMock,
  writable: false,
});

// Mock window.matchMedia for system theme detection
const matchMediaMock = vi.fn().mockReturnValue({
  matches: false,
  media: '(prefers-color-scheme: no-preference)',
  onchange: null,
  addListener: vi.fn(), // deprecated
  removeListener: vi.fn(), // deprecated
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  dispatchEvent: vi.fn(),
});
Object.defineProperty(window, 'matchMedia', { value: matchMediaMock });

// Test component that uses the theme hook
const TestComponent = () => {
  const { theme, setTheme } = useTheme();
  return (
    <div>
      <div data-testid="current-theme">{theme}</div>
      <button onClick={() => setTheme('light')}>Set Light</button>
      <button onClick={() => setTheme('dark')}>Set Dark</button>
      <button onClick={() => setTheme('system')}>Set System</button>
    </div>
  );
};

describe('ThemeProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.clear();
    documentElementClassListMock.add.mockClear();
    documentElementClassListMock.remove.mockClear();
    // Ensure matchMedia returns a valid object by default after clearing mocks
    matchMediaMock.mockReturnValue({
      matches: false,
      media: '(prefers-color-scheme: no-preference)',
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('provides default theme as system when no localStorage value exists', () => {
    localStorageMock.getItem.mockReturnValueOnce(null);

    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    expect(screen.getByTestId('current-theme').textContent).toBe('system');
  });

  it('uses theme from localStorage when available', () => {
    localStorageMock.getItem.mockReturnValueOnce('dark');

    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    expect(screen.getByTestId('current-theme').textContent).toBe('dark');
  });

  it('uses custom defaultTheme when provided', () => {
    localStorageMock.getItem.mockReturnValueOnce(null);

    render(
      <ThemeProvider defaultTheme="light">
        <TestComponent />
      </ThemeProvider>
    );

    expect(screen.getByTestId('current-theme').textContent).toBe('light');
  });

  it('uses custom storageKey when provided', async () => {
    const user = userEvent.setup();
    localStorageMock.getItem.mockReturnValueOnce(null);

    render(
      <ThemeProvider storageKey="custom-theme-key">
        <TestComponent />
      </ThemeProvider>
    );

    const lightButton = screen.getByText('Set Light');
    await user.click(lightButton);

    expect(localStorageMock.setItem).toHaveBeenCalledWith('custom-theme-key', 'light');
  });

  it('updates theme when setTheme is called', async () => {
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    const darkButton = screen.getByText('Set Dark');
    await user.click(darkButton);

    expect(screen.getByTestId('current-theme').textContent).toBe('dark');
    expect(localStorageMock.setItem).toHaveBeenCalledWith('ui-theme', 'dark');
  });

  it('applies theme class to document root', async () => {
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    // Initial render should remove both classes and add the default
    expect(documentElementClassListMock.remove).toHaveBeenCalledWith('light', 'dark');

    // Click to set dark theme
    const darkButton = screen.getByText('Set Dark');
    await user.click(darkButton);

    // Should remove both classes and add dark
    expect(documentElementClassListMock.remove).toHaveBeenCalledWith('light', 'dark');
    expect(documentElementClassListMock.add).toHaveBeenCalledWith('dark');

    // Click to set light theme
    const lightButton = screen.getByText('Set Light');
    await user.click(lightButton);

    // Should remove both classes and add light
    expect(documentElementClassListMock.remove).toHaveBeenCalledWith('light', 'dark');
    expect(documentElementClassListMock.add).toHaveBeenCalledWith('light');
  });

  it('detects system theme when theme is set to system', async () => {
    const user = userEvent.setup();

    // Mock system preference as dark
    matchMediaMock.mockReturnValue({ matches: true });

    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    // Set to system theme
    const systemButton = screen.getByText('Set System');
    await user.click(systemButton);

    // Should check system preference
    expect(matchMediaMock).toHaveBeenCalledWith('(prefers-color-scheme: dark)');

    // Should apply dark theme based on system preference
    expect(documentElementClassListMock.add).toHaveBeenCalledWith('dark');

    // Change system preference to light
    matchMediaMock.mockReturnValue({ matches: false });

    // Trigger re-render
    await act(async () => {
      await user.click(systemButton);
    });

    // Should apply light theme based on new system preference
    expect(documentElementClassListMock.add).toHaveBeenCalledWith('light');
  });

  it('throws error when useTheme is used outside ThemeProvider', () => {
    // Suppress console.error for this test
    const originalConsoleError = console.error;
    console.error = vi.fn();

    // Expect error when rendering component that uses useTheme without provider
    expect(() => {
      render(<TestComponent />);
    }).toThrow('useTheme must be used within a ThemeProvider');

    // Restore console.error
    console.error = originalConsoleError;
  });
});
