import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import MonitoringPage from '@/pages/monitoring';
import * as metricsApi from '@/api/metrics';

// Mock the metrics API
vi.mock('@/api/metrics', () => ({
  getHealthStatus: vi.fn(),
  getSystemInfo: vi.fn(),
  getGrepWiseMetrics: vi.fn(),
  getJvmMetrics: vi.fn(),
  getSystemMetrics: vi.fn(),
  getHttpMetrics: vi.fn(),
}));

// Mock toast hook
const mockToast = vi.fn();
vi.mock('@/components/ui/use-toast', () => ({
  useToast: () => ({ toast: mockToast }),
}));

describe('MonitoringPage', () => {
  // Mock data for tests
  const mockHealthStatus = {
    status: 'UP',
    components: {
      db: { status: 'UP' },
      diskSpace: { status: 'UP' },
      ping: { status: 'UP' },
    },
  };

  const mockSystemInfo = {
    app: {
      name: 'GrepWise',
      description: 'An open-source alternative to Splunk for log analysis and monitoring',
      version: '0.0.1-SNAPSHOT',
    },
    java: {
      version: '17.0.2',
      vendor: { name: 'Oracle Corporation' },
      runtime: { name: 'OpenJDK Runtime Environment', version: '17.0.2+8' },
    },
    os: {
      name: 'Windows 10',
      version: '10.0',
      arch: 'amd64',
    },
  };

  const mockGrepwiseMetrics = [
    {
      name: 'grepwise.logbuffer.size',
      description: 'Current size of the log buffer',
      baseUnit: null,
      measurements: [{ statistic: 'VALUE', value: 42 }],
      availableTags: [],
    },
    {
      name: 'grepwise.logbuffer.utilization',
      description: 'Current utilization of the log buffer (0-1)',
      baseUnit: null,
      measurements: [{ statistic: 'VALUE', value: 0.42 }],
      availableTags: [],
    },
  ];

  const mockJvmMetrics = [
    {
      name: 'jvm.memory.used',
      description: 'The amount of used memory',
      baseUnit: 'bytes',
      measurements: [{ statistic: 'VALUE', value: 100000000 }],
      availableTags: [],
    },
    {
      name: 'jvm.memory.max',
      description: 'The maximum amount of memory that can be used',
      baseUnit: 'bytes',
      measurements: [{ statistic: 'VALUE', value: 1000000000 }],
      availableTags: [],
    },
  ];

  const mockSystemMetrics = [
    {
      name: 'system.cpu.usage',
      description: 'The system CPU usage',
      baseUnit: null,
      measurements: [{ statistic: 'VALUE', value: 0.65 }],
      availableTags: [],
    },
    {
      name: 'disk.free',
      description: 'Free disk space',
      baseUnit: 'bytes',
      measurements: [{ statistic: 'VALUE', value: 50000000000 }],
      availableTags: [],
    },
    {
      name: 'disk.total',
      description: 'Total disk space',
      baseUnit: 'bytes',
      measurements: [{ statistic: 'VALUE', value: 500000000000 }],
      availableTags: [],
    },
  ];

  const mockHttpMetrics = [
    {
      name: 'http.server.requests',
      description: 'HTTP request metrics',
      baseUnit: null,
      measurements: [{ statistic: 'COUNT', value: 1234 }],
      availableTags: [],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Mock API responses
    vi.mocked(metricsApi.getHealthStatus).mockResolvedValue(mockHealthStatus);
    vi.mocked(metricsApi.getSystemInfo).mockResolvedValue(mockSystemInfo);
    vi.mocked(metricsApi.getGrepWiseMetrics).mockResolvedValue(mockGrepwiseMetrics);
    vi.mocked(metricsApi.getJvmMetrics).mockResolvedValue(mockJvmMetrics);
    vi.mocked(metricsApi.getSystemMetrics).mockResolvedValue(mockSystemMetrics);
    vi.mocked(metricsApi.getHttpMetrics).mockResolvedValue(mockHttpMetrics);
    
    // Mock Date.now() to return a consistent value for testing
    vi.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('12:00:00 PM');
  });

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );
  };

  it('renders the monitoring page title', () => {
    renderComponent();
    expect(screen.getByText('System Monitoring')).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    renderComponent();
    
    // Check for loading skeletons
    const skeletons = screen.getAllByTestId(/skeleton/i);
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('fetches metrics on mount', async () => {
    renderComponent();
    
    await waitFor(() => {
      expect(metricsApi.getHealthStatus).toHaveBeenCalled();
      expect(metricsApi.getSystemInfo).toHaveBeenCalled();
      expect(metricsApi.getGrepWiseMetrics).toHaveBeenCalled();
      expect(metricsApi.getJvmMetrics).toHaveBeenCalled();
      expect(metricsApi.getSystemMetrics).toHaveBeenCalled();
      expect(metricsApi.getHttpMetrics).toHaveBeenCalled();
    });
  });

  it('displays health status correctly', async () => {
    renderComponent();
    
    await waitFor(() => {
      expect(screen.getByText('UP')).toBeInTheDocument();
    });
  });

  it('displays system info correctly', async () => {
    renderComponent();
    
    await waitFor(() => {
      expect(screen.getByText('GrepWise')).toBeInTheDocument();
      expect(screen.getByText(/Version: 0.0.1-SNAPSHOT/)).toBeInTheDocument();
      expect(screen.getByText('Windows 10')).toBeInTheDocument();
    });
  });

  it('displays CPU usage with correct formatting', async () => {
    renderComponent();
    
    await waitFor(() => {
      expect(screen.getByText('65.0%')).toBeInTheDocument();
    });
  });

  it('displays memory usage with correct formatting', async () => {
    renderComponent();
    
    await waitFor(() => {
      expect(screen.getByText('95.37 MB')).toBeInTheDocument();
    });
  });

  it('displays disk space with correct formatting', async () => {
    renderComponent();
    
    await waitFor(() => {
      expect(screen.getByText('46.57 GB')).toBeInTheDocument();
    });
  });

  it('switches between tabs', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('UP')).toBeInTheDocument();
    });
    
    // Click on JVM tab
    await user.click(screen.getByRole('tab', { name: /JVM/i }));
    
    // Check that JVM tab content is displayed
    expect(screen.getByText('JVM Metrics')).toBeInTheDocument();
    
    // Click on GrepWise tab
    await user.click(screen.getByRole('tab', { name: /GrepWise/i }));
    
    // Check that GrepWise tab content is displayed
    expect(screen.getByText('GrepWise Metrics')).toBeInTheDocument();
  });

  it('refreshes metrics when refresh button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Wait for initial data to load
    await waitFor(() => {
      expect(screen.getByText('UP')).toBeInTheDocument();
    });
    
    // Clear mocks to check if they're called again
    vi.clearAllMocks();
    
    // Click refresh button
    await user.click(screen.getByRole('button', { name: /Refresh/i }));
    
    // Check that metrics APIs were called again
    await waitFor(() => {
      expect(metricsApi.getHealthStatus).toHaveBeenCalled();
      expect(metricsApi.getSystemInfo).toHaveBeenCalled();
      expect(metricsApi.getGrepWiseMetrics).toHaveBeenCalled();
      expect(metricsApi.getJvmMetrics).toHaveBeenCalled();
      expect(metricsApi.getSystemMetrics).toHaveBeenCalled();
      expect(metricsApi.getHttpMetrics).toHaveBeenCalled();
    });
  });

  it('toggles auto-refresh when button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();
    
    // Wait for initial data to load
    await waitFor(() => {
      expect(screen.getByText('UP')).toBeInTheDocument();
    });
    
    // Auto-refresh should be enabled by default
    expect(screen.getByText('Disable Auto-refresh')).toBeInTheDocument();
    
    // Click to disable auto-refresh
    await user.click(screen.getByRole('button', { name: /Disable Auto-refresh/i }));
    
    // Check that button text changed
    expect(screen.getByText('Enable Auto-refresh')).toBeInTheDocument();
    
    // Click to enable auto-refresh again
    await user.click(screen.getByRole('button', { name: /Enable Auto-refresh/i }));
    
    // Check that button text changed back
    expect(screen.getByText('Disable Auto-refresh')).toBeInTheDocument();
  });

  it('handles API errors gracefully', async () => {
    // Mock API to throw an error
    vi.mocked(metricsApi.getHealthStatus).mockRejectedValue(new Error('API error'));
    
    renderComponent();
    
    // Wait for error toast to be called
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'Error',
        description: 'Failed to fetch metrics. Please try again.',
        variant: 'destructive'
      });
    });
  });

  it('shows empty state when no metrics are found', async () => {
    // Mock empty metrics
    vi.mocked(metricsApi.getGrepWiseMetrics).mockResolvedValue([]);
    
    renderComponent();
    
    // Go to GrepWise tab
    const user = userEvent.setup();
    await waitFor(() => {
      expect(screen.getByText('UP')).toBeInTheDocument();
    });
    
    await user.click(screen.getByRole('tab', { name: /GrepWise/i }));
    
    // Check for empty state message
    await waitFor(() => {
      expect(screen.getByText('No GrepWise metrics found')).toBeInTheDocument();
    });
  });
});