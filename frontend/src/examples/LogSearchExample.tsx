import React, { useState, useEffect } from 'react';
import { logService, LogEntry } from '../services';
import { formatTimestamp } from '@/lib/utils';

/**
 * Example component demonstrating how to use the gRPC-Web LogService.
 * This component shows how to search logs and display the results.
 */
const LogSearchExample: React.FC = () => {
  // State for search query and results
  const [query, setQuery] = useState<string>('');
  const [timeRange, setTimeRange] = useState<string>('24h');
  const [isRegex, setIsRegex] = useState<boolean>(false);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [totalResults, setTotalResults] = useState<number>(0);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);

  // Function to search logs
  const searchLogs = async () => {
    setLoading(true);
    setError(null);

    try {
      // Calculate time range
      let startTime: number | undefined;
      let endTime: number | undefined;

      if (timeRange !== 'custom') {
        const now = Date.now();
        endTime = now;

        switch (timeRange) {
          case '1h':
            startTime = now - (1 * 60 * 60 * 1000);
            break;
          case '3h':
            startTime = now - (3 * 60 * 60 * 1000);
            break;
          case '12h':
            startTime = now - (12 * 60 * 60 * 1000);
            break;
          case '24h':
            startTime = now - (24 * 60 * 60 * 1000);
            break;
          default:
            // No time range
            break;
        }
      }

      // Call the gRPC-Web service
      const response = await logService.searchLogs(
        query,
        isRegex,
        startTime,
        endTime,
        currentPage,
        10 // Page size
      );

      // Update state with results
      setLogs(response.getLogsList());
      setTotalResults(response.getTotalResults());
      setTotalPages(response.getTotalPages());
      setCurrentPage(response.getCurrentPage());
    } catch (err) {
      console.error('Error searching logs:', err);
      setError('Error searching logs. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Search when query, timeRange, isRegex, or currentPage changes
  useEffect(() => {
    if (query || timeRange) {
      searchLogs();
    }
  }, [query, timeRange, isRegex, currentPage]);

  // Handle form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    searchLogs();
  };

  // Handle page change
  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold mb-4">Log Search Example (gRPC-Web)</h1>
      
      {/* Search form */}
      <form onSubmit={handleSubmit} className="mb-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-grow">
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search logs..."
              className="w-full p-2 border rounded"
            />
          </div>
          
          <div>
            <select
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value)}
              className="p-2 border rounded"
            >
              <option value="1h">Last 1 hour</option>
              <option value="3h">Last 3 hours</option>
              <option value="12h">Last 12 hours</option>
              <option value="24h">Last 24 hours</option>
              <option value="custom">Custom range</option>
            </select>
          </div>
          
          <div className="flex items-center">
            <input
              type="checkbox"
              id="isRegex"
              checked={isRegex}
              onChange={(e) => setIsRegex(e.target.checked)}
              className="mr-2"
            />
            <label htmlFor="isRegex">Regex</label>
          </div>
          
          <button
            type="submit"
            className="px-4 py-2 bg-blue-500 text-white rounded"
            disabled={loading}
          >
            {loading ? 'Searching...' : 'Search'}
          </button>
        </div>
      </form>
      
      {/* Error message */}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}
      
      {/* Results */}
      <div>
        <h2 className="text-xl font-semibold mb-2">Results</h2>
        
        {/* Results count */}
        {logs.length > 0 && (
          <p className="mb-2">
            Showing {logs.length} of {totalResults} results
            (Page {currentPage + 1} of {totalPages})
          </p>
        )}
        
        {/* Log entries */}
        {logs.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full bg-white border">
              <thead>
                <tr>
                  <th className="px-4 py-2 border">Timestamp</th>
                  <th className="px-4 py-2 border">Level</th>
                  <th className="px-4 py-2 border">Source</th>
                  <th className="px-4 py-2 border">Message</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.getId()}>
                    <td className="px-4 py-2 border">
                      {formatTimestamp(log.getTimestamp())}
                    </td>
                    <td className="px-4 py-2 border">{log.getLevel()}</td>
                    <td className="px-4 py-2 border">{log.getSource()}</td>
                    <td className="px-4 py-2 border">{log.getMessage()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p>No logs found.</p>
        )}
        
        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex justify-center mt-4">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
              className="px-3 py-1 border rounded mr-2"
            >
              Previous
            </button>
            
            <span className="px-3 py-1">
              Page {currentPage + 1} of {totalPages}
            </span>
            
            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages - 1}
              className="px-3 py-1 border rounded ml-2"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default LogSearchExample;