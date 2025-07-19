import { useState, useEffect } from 'react';
import { useToast } from '@/components/ui/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { AuditLog, AuditLogFilter, auditLogApi } from '@/api/audit-log';
import { format } from 'date-fns';

export default function AuditLogsPage() {
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [categories, setCategories] = useState<string[]>([]);
  const [actions, setActions] = useState<string[]>([]);
  const [targetTypes, setTargetTypes] = useState<string[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const { toast } = useToast();

  // Pagination state
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  // Filter state
  const [filters, setFilters] = useState<AuditLogFilter>({
    page: 0,
    size: 20,
  });

  // Load audit logs and metadata on component mount
  useEffect(() => {
    loadAuditLogsAndMetadata();
  }, []);

  // Load audit logs when filters or pagination changes
  useEffect(() => {
    loadAuditLogs();
  }, [filters, page, pageSize]);

  const loadAuditLogsAndMetadata = async () => {
    try {
      setLoading(true);
      
      // Load metadata for filters
      const [categoriesData, actionsData, targetTypesData, count] = await Promise.all([
        auditLogApi.getCategories(),
        auditLogApi.getActions(),
        auditLogApi.getTargetTypes(),
        auditLogApi.getCount()
      ]);
      
      setCategories(categoriesData);
      setActions(actionsData);
      setTargetTypes(targetTypesData);
      setTotalCount(count);
      
      // Load initial audit logs
      await loadAuditLogs();
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to load audit logs data',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const loadAuditLogs = async () => {
    try {
      setLoading(true);
      
      // Update filters with current pagination
      const currentFilters = {
        ...filters,
        page,
        size: pageSize,
      };
      
      const logs = await auditLogApi.getAuditLogs(currentFilters);
      setAuditLogs(logs);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to load audit logs',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // Handle filter changes
  const handleFilterChange = (name: keyof AuditLogFilter, value: string | number | undefined) => {
    // If value is empty string, set it to undefined to remove the filter
    const processedValue = value === '' ? undefined : value;
    
    setFilters(prev => ({
      ...prev,
      [name]: processedValue,
    }));
    
    // Reset to first page when filters change
    setPage(0);
  };

  // Handle pagination
  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = parseInt(e.target.value);
    setPageSize(newSize);
    setPage(0); // Reset to first page when page size changes
  };

  // Format timestamp to readable date
  const formatTimestamp = (timestamp: number) => {
    return format(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss');
  };

  // Clear all filters
  const clearFilters = () => {
    setFilters({
      page: 0,
      size: pageSize,
    });
    setPage(0);
  };

  // Calculate total pages
  const totalPages = Math.ceil(totalCount / pageSize);

  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Audit Log Viewer</h1>
      </div>

      {/* Filters */}
      <div className="bg-card rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Filters</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          <div className="space-y-2">
            <label htmlFor="username" className="block text-sm font-medium">
              Username
            </label>
            <Input
              id="username"
              value={filters.username || ''}
              onChange={(e) => handleFilterChange('username', e.target.value)}
              placeholder="Filter by username"
            />
          </div>

          <div className="space-y-2">
            <label htmlFor="category" className="block text-sm font-medium">
              Category
            </label>
            <select
              id="category"
              value={filters.category || ''}
              onChange={(e) => handleFilterChange('category', e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2"
            >
              <option value="">All Categories</option>
              {categories.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label htmlFor="action" className="block text-sm font-medium">
              Action
            </label>
            <select
              id="action"
              value={filters.action || ''}
              onChange={(e) => handleFilterChange('action', e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2"
            >
              <option value="">All Actions</option>
              {actions.map((action) => (
                <option key={action} value={action}>
                  {action}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label htmlFor="status" className="block text-sm font-medium">
              Status
            </label>
            <select
              id="status"
              value={filters.status || ''}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2"
            >
              <option value="">All Statuses</option>
              <option value="SUCCESS">Success</option>
              <option value="FAILURE">Failure</option>
            </select>
          </div>

          <div className="space-y-2">
            <label htmlFor="targetType" className="block text-sm font-medium">
              Target Type
            </label>
            <select
              id="targetType"
              value={filters.targetType || ''}
              onChange={(e) => handleFilterChange('targetType', e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2"
            >
              <option value="">All Target Types</option>
              {targetTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label htmlFor="searchText" className="block text-sm font-medium">
              Search Text
            </label>
            <Input
              id="searchText"
              value={filters.searchText || ''}
              onChange={(e) => handleFilterChange('searchText', e.target.value)}
              placeholder="Search in description"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div className="space-y-2">
            <label htmlFor="startTime" className="block text-sm font-medium">
              Start Time
            </label>
            <Input
              id="startTime"
              type="datetime-local"
              onChange={(e) => {
                const date = new Date(e.target.value);
                handleFilterChange('startTime', date.getTime());
              }}
            />
          </div>

          <div className="space-y-2">
            <label htmlFor="endTime" className="block text-sm font-medium">
              End Time
            </label>
            <Input
              id="endTime"
              type="datetime-local"
              onChange={(e) => {
                const date = new Date(e.target.value);
                handleFilterChange('endTime', date.getTime());
              }}
            />
          </div>
        </div>

        <div className="flex justify-end">
          <Button onClick={clearFilters} variant="outline" className="mr-2">
            Clear Filters
          </Button>
          <Button onClick={loadAuditLogs}>Apply Filters</Button>
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="bg-card rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium">Timestamp</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Username</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Category</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Action</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Status</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Description</th>
                <th className="px-4 py-3 text-left text-sm font-medium">Target</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-3 text-center">Loading audit logs...</td>
                </tr>
              ) : auditLogs.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-3 text-center">No audit logs found</td>
                </tr>
              ) : (
                auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-muted/50">
                    <td className="px-4 py-3 text-sm">{formatTimestamp(log.timestamp)}</td>
                    <td className="px-4 py-3 text-sm">{log.username}</td>
                    <td className="px-4 py-3 text-sm">{log.category}</td>
                    <td className="px-4 py-3 text-sm">{log.action}</td>
                    <td className="px-4 py-3 text-sm">
                      <span
                        className={`px-2 py-1 rounded-full text-xs ${
                          log.status === 'SUCCESS'
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {log.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">{log.description}</td>
                    <td className="px-4 py-3 text-sm">
                      {log.targetType && log.targetId
                        ? `${log.targetType}: ${log.targetId}`
                        : '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 bg-muted/50">
          <div className="flex items-center">
            <span className="text-sm text-muted-foreground">
              Showing {auditLogs.length > 0 ? page * pageSize + 1 : 0} to{' '}
              {Math.min((page + 1) * pageSize, totalCount)} of {totalCount} entries
            </span>
            <div className="ml-4">
              <select
                value={pageSize}
                onChange={handlePageSizeChange}
                className="rounded-md border border-input bg-background px-2 py-1 text-sm"
              >
                <option value="10">10</option>
                <option value="20">20</option>
                <option value="50">50</option>
                <option value="100">100</option>
              </select>
            </div>
          </div>
          <div className="flex space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => handlePageChange(page - 1)}
              disabled={page === 0}
            >
              Previous
            </Button>
            <div className="flex items-center space-x-1">
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                // Show pages around current page
                let pageNum = page;
                if (page < 2) {
                  pageNum = i;
                } else if (page > totalPages - 3) {
                  pageNum = totalPages - 5 + i;
                } else {
                  pageNum = page - 2 + i;
                }
                
                if (pageNum >= 0 && pageNum < totalPages) {
                  return (
                    <Button
                      key={pageNum}
                      variant={pageNum === page ? "default" : "outline"}
                      size="sm"
                      onClick={() => handlePageChange(pageNum)}
                    >
                      {pageNum + 1}
                    </Button>
                  );
                }
                return null;
              })}
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => handlePageChange(page + 1)}
              disabled={page >= totalPages - 1}
            >
              Next
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}