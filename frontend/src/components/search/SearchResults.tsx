import { Fragment, useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import SearchFilters, { FilterValues } from '@/components/search/SearchFilters';
import SearchPagination from '@/components/search/SearchPagination';
import { LogEntry, SearchParams, getLogById } from '@/api/logSearch';

export type SortColumn = 'timestamp' | 'level' | 'message' | 'source' | null;
export type SortDirection = 'asc' | 'desc';

type Props = {
  results: LogEntry[];
  processedResults: LogEntry[];
  totalCount: number | null;
  pageSize: number;
  currentPage: number;
  sortColumn: SortColumn;
  sortDirection: SortDirection;
  onSort: (column: Exclude<SortColumn, null>) => void;
  expandedLogId: string | null;
  onRowClick: (id: string) => void;
  renderHighlighted: (text: string) => JSX.Element | string;
  getLevelClass: (level: string) => string;
  formatTimestamp: (ts: number) => string;
  showFilters: boolean;
  filterValues: FilterValues;
  onFilterChange: (field: keyof FilterValues, value: string) => void;
  onToggleFilters: () => void;
  currentSearchParams: SearchParams;
  onExportCsv: (params: SearchParams) => void;
  onExportJson: (params: SearchParams) => void;
  onPageChange: (page: number) => void;
  onPageSizeChange?: (size: number) => void;
};

export default function SearchResults(props: Props) {
  const {
    processedResults,
    totalCount,
    pageSize,
    currentPage,
    sortColumn,
    sortDirection,
    onSort,
    expandedLogId,
    onRowClick,
    renderHighlighted,
    getLevelClass,
    formatTimestamp,
    showFilters,
    filterValues,
    onFilterChange,
    onToggleFilters,
    currentSearchParams,
    onExportCsv,
    onExportJson,
    onPageChange,
    onPageSizeChange,
    } = props;

  // State for reveal/unredact of the currently expanded row
  const [revealedLog, setRevealedLog] = useState<LogEntry | null>(null);
  const [isRevealing, setIsRevealing] = useState(false);
  const [revealError, setRevealError] = useState<string | null>(null);

  // Clear reveal state whenever expanded row changes
  useEffect(() => {
    setRevealedLog(null);
    setIsRevealing(false);
    setRevealError(null);
  }, [expandedLogId]);

  const handleRevealClick = async (e: React.MouseEvent, log: LogEntry) => {
    e.stopPropagation();
    if (revealedLog && revealedLog.id === log.id) {
      // Hide again
      setRevealedLog(null);
      setRevealError(null);
      return;
    }
    try {
      setIsRevealing(true);
      setRevealError(null);
      const full = await getLogById(log.id, true);
      setRevealedLog(full);
    } catch (err: any) {
      setRevealError(err?.message || 'Failed to reveal');
    } finally {
      setIsRevealing(false);
    }
  };

  return (
    <div className="space-y-2" data-testid="results-section">
      <div className="flex justify-between items-center">
        <div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground" data-testid="results-summary">
            <div className="hidden sm:flex items-center gap-2">
              <span className="text-sm text-muted-foreground">Show:</span>
              <select
                className="h-8 w-[110px] rounded-md border border-input bg-background px-2 text-xs"
                value={String(pageSize)}
                onChange={(e) => {
                  const val = parseInt(e.target.value, 10);
                  if (onPageSizeChange) onPageSizeChange(val);
                }}
                data-testid="page-size"
              >
                <option value="10">10 Per Page</option>
                <option value="20">20 Per Page</option>
                <option value="50">50 Per Page</option>
                <option value="100">100 Per Page</option>
              </select>
            </div>
            <SearchPagination
              totalCount={totalCount}
              pageSize={pageSize}
              currentPage={currentPage}
              onPageChange={onPageChange}
            />
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onExportCsv(currentSearchParams)}
            className="flex items-center gap-1"
            data-testid="export-csv"
          >
            Export CSV
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => onExportJson(currentSearchParams)}
            className="flex items-center gap-1"
            data-testid="export-json"
          >
            Export JSON
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={onToggleFilters}
            className="flex items-center gap-1"
            data-testid="toggle-filters"
          >
            {showFilters ? 'Hide Filters' : 'Show Filters'}
          </Button>
        </div>
      </div>

      <SearchFilters
        visible={showFilters}
        values={filterValues}
        onChange={(field, value) => onFilterChange(field as any, value)}
      />

      <div className="rounded-md border" data-testid="results-table-container">
        <div className="overflow-x-auto">
          <table className="w-full" data-testid="results-table">
            <thead>
              <tr className="border-b bg-muted/50">
                <th
                  className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                  onClick={() => onSort('timestamp')}
                  data-testid="col-timestamp"
                >
                  <div className="flex items-center gap-1">
                    Timestamp
                    {sortColumn === 'timestamp' && <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>}
                  </div>
                </th>
                <th
                  className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                  onClick={() => onSort('level')}
                  data-testid="col-level"
                >
                  <div className="flex items-center gap-1">
                    Level
                    {sortColumn === 'level' && <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>}
                  </div>
                </th>
                <th
                  className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                  onClick={() => onSort('message')}
                  data-testid="col-message"
                >
                  <div className="flex items-center gap-1">
                    Message
                    {sortColumn === 'message' && <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>}
                  </div>
                </th>
                <th
                  className="px-4 py-2 text-left font-medium cursor-pointer hover:bg-muted/70"
                  onClick={() => onSort('source')}
                  data-testid="col-source"
                >
                  <div className="flex items-center gap-1">
                    Source
                    {sortColumn === 'source' && <span>{sortDirection === 'asc' ? '↑' : '↓'}</span>}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {processedResults.map((log) => (
                <Fragment key={log.id}>
                  <tr
                    className="border-b hover:bg-muted/50 cursor-pointer"
                    onClick={() => onRowClick(log.id)}
                    data-testid="result-row"
                  >
                    <td className="px-4 py-2 text-sm">{formatTimestamp(log.timestamp)}</td>
                    <td className="px-4 py-2 text-sm">
                      <span className={getLevelClass(log.level)}>{log.level}</span>
                    </td>
                    <td className="px-4 py-2 text-sm">{renderHighlighted(log.message)}</td>
                    <td className="px-4 py-2 text-sm">{log.source}</td>
                  </tr>
                  {expandedLogId === log.id && (
                    <tr className="border-b bg-muted/20" data-testid="result-row-expanded">
                      <td colSpan={4} className="px-4 py-3">
                        <div className="flex justify-between items-start mb-2">
                          <div className="flex items-center gap-3">
                            <h3 className="text-sm font-medium">Log Details</h3>
                            <span className="text-[10px] text-muted-foreground">Sensitive fields are redacted by default</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Button
                              variant="secondary"
                              size="sm"
                              onClick={(e) => handleRevealClick(e, log)}
                              disabled={isRevealing}
                            >
                              {revealedLog && revealedLog.id === log.id ? 'Hide secrets' : isRevealing ? 'Revealing…' : 'Reveal secrets'}
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={(e) => {
                                e.stopPropagation();
                                onRowClick(log.id);
                              }}
                            >
                              Close
                            </Button>
                          </div>
                        </div>
                        {revealError && (
                          <div className="text-[11px] text-red-600 mb-2">{revealError}</div>
                        )}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <p className="text-xs font-medium">Timestamp</p>
                            <p className="text-xs">{formatTimestamp(log.timestamp)}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium">Level</p>
                            <p className={`text-xs ${getLevelClass(log.level)}`}>{log.level}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium">Source</p>
                            <p className="text-xs">{log.source}</p>
                          </div>
                          <div>
                            <p className="text-xs font-medium">ID</p>
                            <p className="text-xs">{log.id}</p>
                          </div>
                        </div>
                        <div className="mt-3">
                          <p className="text-xs font-medium">Message</p>
                          <p className="text-xs mt-1 p-2 bg-background rounded-md">{revealedLog && revealedLog.id === log.id ? revealedLog.message : log.message}</p>
                        </div>
                        <div className="mt-3">
                          <p className="text-xs font-medium">Metadata</p>
                          <pre className="text-xs mt-1 p-2 bg-background rounded-md overflow-x-auto">
                            {JSON.stringify((revealedLog && revealedLog.id === log.id ? revealedLog.metadata : log.metadata), null, 2)}
                          </pre>
                        </div>
                        { (revealedLog && revealedLog.id === log.id && revealedLog.rawContent) ? (
                          <div className="mt-3">
                            <p className="text-xs font-medium">Raw</p>
                            <pre className="text-xs mt-1 p-2 bg-background rounded-md overflow-x-auto">{revealedLog.rawContent}</pre>
                          </div>
                        ) : null }
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
