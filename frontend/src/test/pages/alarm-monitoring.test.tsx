import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AlarmMonitoringPage from '@/pages/alarm-monitoring';
import { alarmApi } from '@/api/alarm';

// Mock the alarm API
vi.mock('@/api/alarm', () => ({
  alarmApi: {
    getAllAlarms: vi.fn(),
    getStatistics: vi.fn(),
    getAlarmEvents: vi.fn(),
    acknowledgeAlarm: vi.fn(),
    resolveAlarm: vi.fn(),
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

describe('AlarmMonitoringPage', () => {
  const mockAlarms = [
    {
      id: 'alarm1',
      name: 'Database Error Alert',
      description: 'Alert for database errors',
      query: 'level:ERROR AND message:database',
      condition: 'count >',
      threshold: 5,
      timeWindowMinutes: 15,
      enabled: true,
      createdAt: Date.now() - 86400000,
      updatedAt: Date.now() - 86400000,
      notificationChannels: [{ type: 'EMAIL', destination: 'admin@example.com' }],
    },
    {
      id: 'alarm2',
      name: 'API Timeout Alert',
      description: 'Alert for API timeouts',
      query: 'level:ERROR AND message:timeout',
      condition: 'count >',
      threshold: 3,
      timeWindowMinutes: 10,
      enabled: true,
      createdAt: Date.now() - 172800000,
      updatedAt: Date.now() - 172800000,
      notificationChannels: [{ type: 'EMAIL', destination: 'admin@example.com' }],
    },
  ];

  const mockStatistics = {
    totalAlarms: 5,
    enabledAlarms: 3,
    disabledAlarms: 2,
    recentlyTriggered: 2,
  };

  const mockAlarmEvents = [
    {
      id: 'evt1',
      alarmId: 'alarm1',
      alarmName: 'Database Error Alert',
      timestamp: Date.now() - 3600000, // 1 hour ago
      status: 'TRIGGERED',
      matchCount: 12,
      details: 'Found 12 matching log entries with database errors',
    },
    {
      id: 'evt2',
      alarmId: 'alarm2',
      alarmName: 'API Timeout Alert',
      timestamp: Date.now() - 7200000, // 2 hours ago
      status: 'ACKNOWLEDGED',
      acknowledgedBy: 'admin',
      acknowledgedAt: Date.now() - 7000000,
      matchCount: 8,
      details: 'Found 8 matching log entries with API timeouts',
    },
    {
      id: 'evt3',
      alarmId: 'alarm3',
      alarmName: 'Security Warning',
      timestamp: Date.now() - 86400000, // 1 day ago
      status: 'RESOLVED',
      acknowledgedBy: 'admin',
      acknowledgedAt: Date.now() - 85000000,
      resolvedBy: 'admin',
      resolvedAt: Date.now() - 84000000,
      matchCount: 3,
      details: 'Found 3 matching log entries with security warnings',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(alarmApi.getAllAlarms).mockResolvedValue(mockAlarms);
    vi.mocked(alarmApi.getStatistics).mockResolvedValue(mockStatistics);
    vi.mocked(alarmApi.getAlarmEvents).mockResolvedValue(mockAlarmEvents);
    vi.mocked(alarmApi.acknowledgeAlarm).mockImplementation((id) => {
      const event = mockAlarmEvents.find((e) => e.id === id);
      if (!event) throw new Error('Event not found');

      return Promise.resolve({
        ...event,
        status: 'ACKNOWLEDGED',
        acknowledgedBy: 'current-user',
        acknowledgedAt: Date.now(),
      });
    });
    vi.mocked(alarmApi.resolveAlarm).mockImplementation((id) => {
      const event = mockAlarmEvents.find((e) => e.id === id);
      if (!event) throw new Error('Event not found');

      return Promise.resolve({
        ...event,
        status: 'RESOLVED',
        resolvedBy: 'current-user',
        resolvedAt: Date.now(),
      });
    });
  });

  const renderWithRouter = () => {
    return render(
      <MemoryRouter initialEntries={['/alarm-monitoring']}>
        <Routes>
          <Route path="/alarm-monitoring" element={<AlarmMonitoringPage />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders alarm monitoring dashboard with title', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Alarm Monitoring Dashboard')).toBeInTheDocument();
      expect(screen.getByText('Monitor and manage active alarms and alerts')).toBeInTheDocument();
    });

    expect(screen.getByText('Manage Alarms')).toBeInTheDocument();
  });

  it('loads alarm data on mount', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(alarmApi.getAllAlarms).toHaveBeenCalled();
      expect(alarmApi.getStatistics).toHaveBeenCalled();
      expect(alarmApi.getAlarmEvents).toHaveBeenCalled();
    });
  });

  it('shows loading state initially', () => {
    // Mock APIs to never resolve during this test
    vi.mocked(alarmApi.getAllAlarms).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolving promise
    );
    vi.mocked(alarmApi.getStatistics).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolving promise
    );
    vi.mocked(alarmApi.getAlarmEvents).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolving promise
    );

    renderWithRouter();

    // Check for loading state
    expect(screen.getByText('Loading alarm data...')).toBeInTheDocument();
  });

  it('displays statistics cards', async () => {
    renderWithRouter();

    await waitFor(() => {
      expect(screen.getByText('Total Alarms')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument(); // totalAlarms value
      expect(screen.getByText('Enabled Alarms')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument(); // enabledAlarms value
      expect(screen.getByText('Disabled Alarms')).toBeInTheDocument();
      expect(screen.getByText('Recently Triggered')).toBeInTheDocument();
    });

    // Find all elements with text "2" and verify both are present
    const elementsWithTwo = screen.getAllByText('2');
    expect(elementsWithTwo).toHaveLength(2);

    // One should be regular styling (disabled alarms), one should be red (recently triggered)
    const disabledAlarmsValue = elementsWithTwo.find(
      (el) =>
        el.className.includes('text-2xl') &&
        el.className.includes('font-bold') &&
        !el.className.includes('text-red-600')
    );
    const recentlyTriggeredValue = elementsWithTwo.find(
      (el) =>
        el.className.includes('text-2xl') && el.className.includes('font-bold') && el.className.includes('text-red-600')
    );

    expect(disabledAlarmsValue).toBeInTheDocument();
    expect(recentlyTriggeredValue).toBeInTheDocument();
  });

  it('displays alarm events in a table', async () => {
    renderWithRouter();

    // Wait for data to load and check that active events are shown by default
    await waitFor(() => {
      expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
      expect(screen.getByText('Found 12 matching log entries with database errors')).toBeInTheDocument();
    });

    // Check for table headers
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('Alarm Name')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Triggered' })).toBeInTheDocument();
    expect(screen.getByText('Match Count')).toBeInTheDocument();
    expect(screen.getByText('Actions')).toBeInTheDocument();

    // Switch to "all" tab to see all events including "API Timeout Alert"
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: /all/i }));

    await waitFor(() => {
      expect(screen.getByText('API Timeout Alert')).toBeInTheDocument();
    });
  });

  it('filters events based on tab selection', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
    });

    // Initially on "active" tab, should show only TRIGGERED events
    expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
    expect(screen.queryByText('Security Warning')).not.toBeInTheDocument();

    // Click on "acknowledged" tab
    await user.click(screen.getByRole('tab', { name: /acknowledged/i }));

    // Should show only ACKNOWLEDGED events
    expect(screen.queryByText('Database Error Alert')).not.toBeInTheDocument();
    expect(screen.getByText('API Timeout Alert')).toBeInTheDocument();

    // Click on "resolved" tab
    await user.click(screen.getByRole('tab', { name: /resolved/i }));

    // Should show only RESOLVED events
    expect(screen.queryByText('Database Error Alert')).not.toBeInTheDocument();
    expect(screen.queryByText('API Timeout Alert')).not.toBeInTheDocument();
    expect(screen.getByText('Security Warning')).toBeInTheDocument();

    // Click on "all" tab
    await user.click(screen.getByRole('tab', { name: /all/i }));

    // Should show all events
    expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
    expect(screen.getByText('API Timeout Alert')).toBeInTheDocument();
    expect(screen.getByText('Security Warning')).toBeInTheDocument();
  });

  it('filters events based on search query', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
    });

    // Click on "all" tab to show all events
    await user.click(screen.getByRole('tab', { name: /all/i }));

    // Enter search query
    await user.type(screen.getByPlaceholderText('Search alarms...'), 'database');

    // Should show only events matching the search query
    expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
    expect(screen.queryByText('API Timeout Alert')).not.toBeInTheDocument();
    expect(screen.queryByText('Security Warning')).not.toBeInTheDocument();
  });

  it('acknowledges an alarm when acknowledge button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
    });

    // Find and click acknowledge button for the triggered alarm
    const acknowledgeButton = screen.getByRole('button', { name: /acknowledge/i });
    await user.click(acknowledgeButton);

    // Check that the API was called
    expect(alarmApi.acknowledgeAlarm).toHaveBeenCalledWith('evt1');

    // Check that a success toast was shown
    expect(mockToast).toHaveBeenCalledWith({
      title: 'Alarm acknowledged',
      description: 'The alarm has been acknowledged successfully',
    });
  });

  it('resolves an alarm when resolve button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Database Error Alert')).toBeInTheDocument();
    });

    // Find and click resolve button for the triggered alarm
    const resolveButton = screen.getByRole('button', { name: /resolve/i });
    await user.click(resolveButton);

    // Check that the API was called
    expect(alarmApi.resolveAlarm).toHaveBeenCalledWith('evt1');

    // Check that a success toast was shown
    expect(mockToast).toHaveBeenCalledWith({
      title: 'Alarm resolved',
      description: 'The alarm has been resolved successfully',
    });
  });

  it('navigates to alarms page when Manage Alarms button is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter();

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Alarm Monitoring Dashboard')).toBeInTheDocument();
    });

    // Click on Manage Alarms button
    await user.click(screen.getByText('Manage Alarms'));

    // Check that navigation was called
    expect(mockNavigate).toHaveBeenCalledWith('/alarms');
  });

  it('handles API errors gracefully', async () => {
    // Mock API to reject
    vi.mocked(alarmApi.getAllAlarms).mockRejectedValueOnce(new Error('Failed to fetch alarms'));

    renderWithRouter();

    // Wait for error toast to be shown
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'Error',
        description: 'Failed to load alarm data',
        variant: 'destructive',
      });
    });
  });
});
