import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import AuditLogsPage from '@/pages/audit-logs';
import { auditLogApi } from '@/api/audit-log';

// Mock the audit log API
vi.mock('@/api/audit-log', () => ({
  auditLogApi: {
    getAuditLogs: vi.fn(),
    getCategories: vi.fn(),
    getActions: vi.fn(),
    getTargetTypes: vi.fn(),
    getCount: vi.fn(),
  },
}));

// Mock toast hook
const mockToast = vi.fn();
vi.mock('@/components/ui/use-toast', () => ({
  useToast: () => ({ toast: mockToast }),
}));

describe('AuditLogsPage', () => {
  const mockAuditLogs = [
    {
      id: '1',
      timestamp: 1689504000000, // 2023-07-16 12:00:00
      userId: 'user1',
      username: 'admin',
      ipAddress: '127.0.0.1',
      category: 'AUTH',
      action: 'LOGIN',
      status: 'SUCCESS',
      description: 'User logged in successfully',
      targetId: 'user1',
      targetType: 'USER',
      details: {},
    },
    {
      id: '2',
      timestamp: 1689507600000, // 2023-07-16 13:00:00
      userId: 'user1',
      username: 'admin',
      ipAddress: '127.0.0.1',
      category: 'USER_MGMT',
      action: 'CREATE',
      status: 'SUCCESS',
      description: 'Created new user: testuser',
      targetId: 'user2',
      targetType: 'USER',
      details: {},
    },
    {
      id: '3',
      timestamp: 1689511200000, // 2023-07-16 14:00:00
      userId: 'user1',
      username: 'admin',
      ipAddress: '127.0.0.1',
      category: 'DASHBOARD',
      action: 'UPDATE',
      status: 'FAILURE',
      description: 'Failed to update dashboard',
      targetId: 'dash1',
      targetType: 'DASHBOARD',
      details: { error: 'Permission denied' },
    },
  ];

  const mockCategories = ['AUTH', 'USER_MGMT', 'DASHBOARD', 'ALARM', 'SETTINGS'];
  const mockActions = ['LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'DELETE'];
  const mockTargetTypes = ['USER', 'DASHBOARD', 'ALARM', 'SETTING'];
  const mockCount = 3;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(auditLogApi.getAuditLogs).mockResolvedValue(mockAuditLogs);
    vi.mocked(auditLogApi.getCategories).mockResolvedValue(mockCategories);
    vi.mocked(auditLogApi.getActions).mockResolvedValue(mockActions);
    vi.mocked(auditLogApi.getTargetTypes).mockResolvedValue(mockTargetTypes);
    vi.mocked(auditLogApi.getCount).mockResolvedValue(mockCount);
  });

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <AuditLogsPage />
      </BrowserRouter>
    );
  };

  it('renders audit log viewer with title', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Audit Log Viewer')).toBeInTheDocument();
    });
  });

  it('loads audit logs and metadata on mount', async () => {
    renderComponent();

    await waitFor(() => {
      expect(auditLogApi.getAuditLogs).toHaveBeenCalled();
      expect(auditLogApi.getCategories).toHaveBeenCalled();
      expect(auditLogApi.getActions).toHaveBeenCalled();
      expect(auditLogApi.getTargetTypes).toHaveBeenCalled();
      expect(auditLogApi.getCount).toHaveBeenCalled();
    });
  });

  it('shows loading state initially', () => {
    // Mock APIs to never resolve during this test
    vi.mocked(auditLogApi.getAuditLogs).mockImplementationOnce(() => 
      new Promise(() => {}) // Never resolving promise
    );
    
    renderComponent();
    
    // Check for loading state
    expect(screen.getByText('Loading audit logs...')).toBeInTheDocument();
  });

  it('populates filter dropdowns with metadata', async () => {
    renderComponent();

    await waitFor(() => {
      // Check for filter options
      const categorySelect = screen.getByLabelText('Category');
      expect(categorySelect).toBeInTheDocument();
      
      const actionSelect = screen.getByLabelText('Action');
      expect(actionSelect).toBeInTheDocument();
      
      const targetTypeSelect = screen.getByLabelText('Target Type');
      expect(targetTypeSelect).toBeInTheDocument();
    });
  });

  it('applies filters when changed', async () => {
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText('Category')).toBeInTheDocument();
    });

    // Select a category filter
    const categorySelect = screen.getByLabelText('Category');
    await user.selectOptions(categorySelect, 'AUTH');

    // Click apply filters button
    await user.click(screen.getByText('Apply Filters'));

    await waitFor(() => {
      expect(auditLogApi.getAuditLogs).toHaveBeenCalledWith(
        expect.objectContaining({ category: 'AUTH' })
      );
    });
  });

  it('clears filters when clear button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText('Category')).toBeInTheDocument();
    });

    // Select a category filter
    const categorySelect = screen.getByLabelText('Category');
    await user.selectOptions(categorySelect, 'AUTH');

    // Click clear filters button
    await user.click(screen.getByText('Clear Filters'));

    // Click apply filters button
    await user.click(screen.getByText('Apply Filters'));

    await waitFor(() => {
      expect(auditLogApi.getAuditLogs).toHaveBeenCalledWith(
        expect.objectContaining({ 
          page: 0,
          size: 20,
        })
      );
    });
  });

  it('changes page size when selected', async () => {
    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Showing 1 to 3 of 3 entries')).toBeInTheDocument();
    });

    // Change page size
    const pageSizeSelect = screen.getByRole('combobox', { name: '' });
    await user.selectOptions(pageSizeSelect, '50');

    await waitFor(() => {
      expect(auditLogApi.getAuditLogs).toHaveBeenCalledWith(
        expect.objectContaining({ size: 50 })
      );
    });
  });

  it('shows empty state when no audit logs found', async () => {
    vi.mocked(auditLogApi.getAuditLogs).mockResolvedValue([]);
    
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('No audit logs found')).toBeInTheDocument();
    });
  });

  it('handles API error gracefully', async () => {
    vi.mocked(auditLogApi.getAuditLogs).mockRejectedValue(new Error('Failed to fetch'));
    
    renderComponent();

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Error',
          variant: 'destructive',
        })
      );
    });
  });

  it('displays status with appropriate styling', async () => {
    renderComponent();

    await waitFor(() => {
      const successStatus = screen.getAllByText('SUCCESS')[0];
      const failureStatus = screen.getByText('FAILURE');
      
      // Check that they have different styling classes
      expect(successStatus.className).toContain('bg-green-100');
      expect(failureStatus.className).toContain('bg-red-100');
    });
  });
});