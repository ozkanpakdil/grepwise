import React, { useState, useMemo } from 'react';
import { DashboardWidget } from '@/api/dashboard';
import { ChevronUp, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';

interface TableWidgetProps {
  data: any;
  widget: DashboardWidget;
}

interface TableColumn {
  key: string;
  label: string;
  type: 'string' | 'number' | 'date';
}

const TableWidget: React.FC<TableWidgetProps> = ({ data, widget: _widget }) => {
  const [sortColumn, setSortColumn] = useState<string>('');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  // Process the data for table display
  const { columns, rows } = useMemo(() => {
    if (!data || !data.results || data.results.length === 0) {
      return { columns: [], rows: [] };
    }

    const results = data.results;
    const allKeys = new Set<string>();
    
    // Collect all possible keys from all results
    results.forEach((result: any) => {
      Object.keys(result).forEach(key => allKeys.add(key));
    });

    // Create columns with type detection
    const columns: TableColumn[] = Array.from(allKeys).map(key => {
      // Detect column type based on first non-null value
      let type: 'string' | 'number' | 'date' = 'string';
      
      for (const result of results) {
        const value = result[key];
        if (value != null) {
          if (typeof value === 'number') {
            type = 'number';
          } else if (key.includes('time') || key.includes('date') || key === 'timestamp') {
            type = 'date';
          }
          break;
        }
      }

      return {
        key,
        label: key.charAt(0).toUpperCase() + key.slice(1).replace(/_/g, ' '),
        type,
      };
    });

    // Sort columns to put important ones first
    columns.sort((a, b) => {
      const importantColumns = ['timestamp', 'time', 'level', 'message', 'count'];
      const aImportance = importantColumns.indexOf(a.key);
      const bImportance = importantColumns.indexOf(b.key);
      
      if (aImportance !== -1 && bImportance !== -1) {
        return aImportance - bImportance;
      } else if (aImportance !== -1) {
        return -1;
      } else if (bImportance !== -1) {
        return 1;
      }
      
      return a.label.localeCompare(b.label);
    });

    return { columns, rows: results };
  }, [data]);

  // Sort the data
  const sortedRows = useMemo(() => {
    if (!sortColumn) return rows;

    const column = columns.find(col => col.key === sortColumn);
    if (!column) return rows;

    return [...rows].sort((a, b) => {
      let aVal = a[sortColumn];
      let bVal = b[sortColumn];

      // Handle null/undefined values
      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return sortDirection === 'asc' ? 1 : -1;
      if (bVal == null) return sortDirection === 'asc' ? -1 : 1;

      // Type-specific sorting
      if (column.type === 'number') {
        aVal = Number(aVal) || 0;
        bVal = Number(bVal) || 0;
      } else if (column.type === 'date') {
        aVal = new Date(aVal).getTime() || 0;
        bVal = new Date(bVal).getTime() || 0;
      } else {
        aVal = String(aVal).toLowerCase();
        bVal = String(bVal).toLowerCase();
      }

      if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [rows, sortColumn, sortDirection, columns]);

  // Paginate the data
  const paginatedRows = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    return sortedRows.slice(startIndex, startIndex + itemsPerPage);
  }, [sortedRows, currentPage]);

  const totalPages = Math.ceil(sortedRows.length / itemsPerPage);

  const handleSort = (columnKey: string) => {
    if (sortColumn === columnKey) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(columnKey);
      setSortDirection('asc');
    }
    setCurrentPage(1);
  };

  const formatValue = (value: any, column: TableColumn) => {
    if (value == null) return '-';

    switch (column.type) {
      case 'date':
        return new Date(value).toLocaleString();
      case 'number':
        return typeof value === 'number' ? value.toLocaleString() : value;
      default:
        return String(value);
    }
  };

  if (columns.length === 0) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-sm text-muted-foreground">
          No data available for table display
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* Table */}
      <div className="flex-1 overflow-auto">
        <table className="w-full text-xs">
          <thead className="bg-gray-50 sticky top-0">
            <tr>
              {columns.slice(0, 6).map((column) => (
                <th
                  key={column.key}
                  className="px-2 py-1 text-left font-medium cursor-pointer hover:bg-gray-100 border-b"
                  onClick={() => handleSort(column.key)}
                >
                  <div className="flex items-center space-x-1">
                    <span className="truncate">{column.label}</span>
                    {sortColumn === column.key && (
                      sortDirection === 'asc' ? 
                        <ChevronUp className="h-3 w-3" /> : 
                        <ChevronDown className="h-3 w-3" />
                    )}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {paginatedRows.map((row: any, index: number) => (
              <tr key={index} className="hover:bg-gray-50 border-b">
                {columns.slice(0, 6).map((column) => (
                  <td key={column.key} className="px-2 py-1 truncate max-w-0">
                    <div className="truncate" title={formatValue(row[column.key], column)}>
                      {formatValue(row[column.key], column)}
                    </div>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between px-2 py-1 border-t bg-gray-50 text-xs">
          <div className="text-muted-foreground">
            {sortedRows.length} rows
          </div>
          
          <div className="flex items-center space-x-1">
            <button
              onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
              disabled={currentPage === 1}
              className="p-1 rounded hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="h-3 w-3" />
            </button>
            
            <span className="px-2">
              {currentPage} of {totalPages}
            </span>
            
            <button
              onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
              disabled={currentPage === totalPages}
              className="p-1 rounded hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronRight className="h-3 w-3" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default TableWidget;