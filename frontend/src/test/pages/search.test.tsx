import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchPage from '@/pages/search';
import { searchLogs, getTimeAggregation } from '@/api/logSearch';

// Mock the Monaco Editor
vi.mock('@monaco-editor/react', () => ({
  default: ({ onChange, defaultValue }: { onChange: (value: string) => void, defaultValue: string }) => {
    return (
      <div data-testid="monaco-editor" className="monaco-editor-mock">
        <textarea
          data-testid="monaco-editor-textarea"
          defaultValue={defaultValue}
          onChange={(e) => onChange(e.target.value)}
        />
      </div>
    );
  },
}));

// Mock the log search API
vi.mock('@/api/logSearch', () => ({
  searchLogs: vi.fn(),
  getTimeAggregation: vi.fn(),
}));

// Mock toast hook
const mockToast = vi.fn();
vi.mock('@/components/ui/use-toast', () => ({
  useToast: () => ({ toast: mockToast }),
}));

describe('SearchPage', () => {
  const mockLogs = [
    {
      id: '1',
      timestamp: 1625097600000, // 2021-07-01T00:00:00Z
      level: 'ERROR',
      message: 'Test error message',
      source: 'app.log',
      metadata: {},
      rawContent: 'Test error message',
    },
    {
      id: '2',
      timestamp: 1625097660000, // 2021-07-01T00:01:00Z
      level: 'INFO',
      message: 'Test info message',
      source: 'app.log',
      metadata: {},
      rawContent: 'Test info message',
    },
  ];

  const mockTimeSlots = [
    { time: 1625097600000, count: 5 },
    { time: 1625097660000, count: 3 },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(searchLogs).mockResolvedValue(mockLogs);
    vi.mocked(getTimeAggregation).mockResolvedValue(mockTimeSlots);
  });

  it('renders search page with title and Monaco Editor', async () => {
    render(<SearchPage />);

    expect(screen.getByText('Log Search')).toBeInTheDocument();
    expect(screen.getByText('Search for logs using simple queries or advanced syntax')).toBeInTheDocument();
    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('performs search when form is submitted', async () => {
    const user = userEvent.setup();
    render(<SearchPage />);

    // Type in the Monaco Editor
    const editorTextarea = screen.getByTestId('monaco-editor-textarea');
    await user.type(editorTextarea, 'error');

    // Submit the form
    const searchButton = screen.getByText('Search');
    await user.click(searchButton);

    // Check that the API was called with the correct parameters
    await waitFor(() => {
      expect(searchLogs).toHaveBeenCalledWith({
        query: 'error',
        isRegex: false,
        timeRange: '24h',
      });
    });

    // Check that time aggregation API was called
    await waitFor(() => {
      expect(getTimeAggregation).toHaveBeenCalled();
    });

    // Check that results are displayed
    await waitFor(() => {
      expect(screen.getByText('Test error message')).toBeInTheDocument();
      expect(screen.getByText('Test info message')).toBeInTheDocument();
    });
  });

  it('toggles regex search when checkbox is clicked', async () => {
    const user = userEvent.setup();
    render(<SearchPage />);

    // Type in the Monaco Editor
    const editorTextarea = screen.getByTestId('monaco-editor-textarea');
    await user.type(editorTextarea, '.*error.*');

    // Toggle regex checkbox
    const regexCheckbox = screen.getByLabelText('Use regex');
    await user.click(regexCheckbox);

    // Submit the form
    const searchButton = screen.getByText('Search');
    await user.click(searchButton);

    // Check that the API was called with regex enabled
    await waitFor(() => {
      expect(searchLogs).toHaveBeenCalledWith({
        query: '.*error.*',
        isRegex: true,
        timeRange: '24h',
      });
    });
  });

  it('changes time range when dropdown is changed', async () => {
    const user = userEvent.setup();
    render(<SearchPage />);

    // Type in the Monaco Editor
    const editorTextarea = screen.getByTestId('monaco-editor-textarea');
    await user.type(editorTextarea, 'error');

    // Open time range dropdown
    const timeRangeSelect = screen.getByLabelText('Time range:');
    await user.click(timeRangeSelect);

    // Select a different time range
    const timeRangeOption = screen.getByText('Last 1 hour', { exact: false }) || screen.getByText('1h');
    await user.click(timeRangeOption);

    // Submit the form
    const searchButton = screen.getByText('Search');
    await user.click(searchButton);

    // Check that the API was called with the correct time range
    await waitFor(() => {
      expect(searchLogs).toHaveBeenCalledWith({
        query: 'error',
        isRegex: false,
        timeRange: '1h',
      });
    });
  });

  it('shows error toast when search fails', async () => {
    const user = userEvent.setup();
    vi.mocked(searchLogs).mockRejectedValue(new Error('Search failed'));

    render(<SearchPage />);

    // Type in the Monaco Editor
    const editorTextarea = screen.getByTestId('monaco-editor-textarea');
    await user.type(editorTextarea, 'error');

    // Submit the form
    const searchButton = screen.getByText('Search');
    await user.click(searchButton);

    // Check that error toast was shown
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'Error',
        description: 'An error occurred during search',
        variant: 'destructive',
      });
    });
  });

  it('shows no results message when search returns empty array', async () => {
    const user = userEvent.setup();
    vi.mocked(searchLogs).mockResolvedValue([]);

    render(<SearchPage />);

    // Type in the Monaco Editor
    const editorTextarea = screen.getByTestId('monaco-editor-textarea');
    await user.type(editorTextarea, 'nonexistent');

    // Submit the form
    const searchButton = screen.getByText('Search');
    await user.click(searchButton);

    // Check that no results message is shown
    await waitFor(() => {
      expect(screen.getByText('No logs found matching your query')).toBeInTheDocument();
    });

    // Check that toast was shown
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: 'No results',
        description: 'No logs found matching your search criteria',
      });
    });
  });

  it('shows log details when a log entry is clicked', async () => {
    const user = userEvent.setup();
    
    render(<SearchPage />);

    // Type in the Monaco Editor
    const editorTextarea = screen.getByTestId('monaco-editor-textarea');
    await user.type(editorTextarea, 'error');

    // Submit the form
    const searchButton = screen.getByText('Search');
    await user.click(searchButton);

    // Wait for results to load
    await waitFor(() => {
      expect(screen.getByText('Test error message')).toBeInTheDocument();
    });

    // Click on a log entry
    await user.click(screen.getByText('Test error message'));

    // Check that log details are shown
    await waitFor(() => {
      expect(screen.getByText('Log Details')).toBeInTheDocument();
      expect(screen.getByText('ID')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  // Tests for sorting functionality
  describe('Sorting functionality', () => {
    const setupSearchResults = async () => {
      const user = userEvent.setup();
      render(<SearchPage />);

      // Type in the Monaco Editor
      const editorTextarea = screen.getByTestId('monaco-editor-textarea');
      await user.type(editorTextarea, 'test');

      // Submit the form
      const searchButton = screen.getByText('Search');
      await user.click(searchButton);

      // Wait for results to load
      await waitFor(() => {
        expect(screen.getByText('Test error message')).toBeInTheDocument();
        expect(screen.getByText('Test info message')).toBeInTheDocument();
      });

      return user;
    };

    it('sorts by level in ascending order when Level header is clicked', async () => {
      const user = await setupSearchResults();

      // Find and click the Level header
      const levelHeader = screen.getByText('Level').closest('th');
      await user.click(levelHeader!);

      // Check that the results are sorted by level in ascending order (ERROR before INFO)
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      expect(within(rows[0]).getByText('ERROR')).toBeInTheDocument();
      expect(within(rows[1]).getByText('INFO')).toBeInTheDocument();

      // Check that the sort indicator is shown
      expect(levelHeader!.textContent).toContain('↑');
    });

    it('toggles sort direction when clicking the same header twice', async () => {
      const user = await setupSearchResults();

      // Find the Level header
      const levelHeader = screen.getByText('Level').closest('th');
      
      // Click once for ascending
      await user.click(levelHeader!);
      
      // Click again for descending
      await user.click(levelHeader!);

      // Check that the results are sorted by level in descending order (INFO before ERROR)
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      expect(within(rows[0]).getByText('INFO')).toBeInTheDocument();
      expect(within(rows[1]).getByText('ERROR')).toBeInTheDocument();

      // Check that the sort indicator is shown
      expect(levelHeader!.textContent).toContain('↓');
    });

    it('sorts by timestamp in descending order by default when Timestamp header is clicked', async () => {
      const user = await setupSearchResults();

      // Find and click the Timestamp header
      const timestampHeader = screen.getByText('Timestamp').closest('th');
      await user.click(timestampHeader!);

      // Check that the results are sorted by timestamp in descending order (newer first)
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      
      // The second log has a newer timestamp
      expect(within(rows[0]).getByText('Test info message')).toBeInTheDocument();
      expect(within(rows[1]).getByText('Test error message')).toBeInTheDocument();

      // Check that the sort indicator is shown
      expect(timestampHeader!.textContent).toContain('↓');
    });

    it('changes sort column when clicking a different header', async () => {
      const user = await setupSearchResults();

      // First sort by Level
      const levelHeader = screen.getByText('Level').closest('th');
      await user.click(levelHeader!);
      
      // Then sort by Message
      const messageHeader = screen.getByText('Message').closest('th');
      await user.click(messageHeader!);

      // Check that the Level header no longer has a sort indicator
      expect(levelHeader!.textContent).not.toContain('↑');
      expect(levelHeader!.textContent).not.toContain('↓');
      
      // Check that the Message header has a sort indicator
      expect(messageHeader!.textContent).toContain('↑');
      
      // Check that the results are sorted by message in ascending order
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      expect(within(rows[0]).getByText('Test error message')).toBeInTheDocument();
      expect(within(rows[1]).getByText('Test info message')).toBeInTheDocument();
    });
  });

  // Tests for filtering functionality
  describe('Filtering functionality', () => {
    const setupSearchResults = async () => {
      const user = userEvent.setup();
      render(<SearchPage />);

      // Type in the Monaco Editor
      const editorTextarea = screen.getByTestId('monaco-editor-textarea');
      await user.type(editorTextarea, 'test');

      // Submit the form
      const searchButton = screen.getByText('Search');
      await user.click(searchButton);

      // Wait for results to load
      await waitFor(() => {
        expect(screen.getByText('Test error message')).toBeInTheDocument();
        expect(screen.getByText('Test info message')).toBeInTheDocument();
      });

      return user;
    };

    it('shows filter controls when Show Filters button is clicked', async () => {
      const user = await setupSearchResults();

      // Initially, filter inputs should not be visible
      expect(screen.queryByLabelText('Filter by Level')).not.toBeInTheDocument();

      // Click the Show Filters button
      const showFiltersButton = screen.getByText('Show Filters');
      await user.click(showFiltersButton);

      // Check that filter inputs are now visible
      expect(screen.getByLabelText('Filter by Level')).toBeInTheDocument();
      expect(screen.getByLabelText('Filter by Message')).toBeInTheDocument();
      expect(screen.getByLabelText('Filter by Source')).toBeInTheDocument();
    });

    it('filters results by level when level filter is used', async () => {
      const user = await setupSearchResults();

      // Show filters
      await user.click(screen.getByText('Show Filters'));

      // Enter a filter value for level
      const levelFilter = screen.getByLabelText('Filter by Level');
      await user.type(levelFilter, 'ERROR');

      // Check that only ERROR logs are shown
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      expect(rows.length).toBe(1);
      expect(within(rows[0]).getByText('ERROR')).toBeInTheDocument();
      expect(screen.queryByText('INFO')).not.toBeInTheDocument();

      // Check that the results count is updated
      expect(screen.getByText('Showing 1 of 2 logs')).toBeInTheDocument();
    });

    it('filters results by message when message filter is used', async () => {
      const user = await setupSearchResults();

      // Show filters
      await user.click(screen.getByText('Show Filters'));

      // Enter a filter value for message
      const messageFilter = screen.getByLabelText('Filter by Message');
      await user.type(messageFilter, 'info');

      // Check that only logs with 'info' in the message are shown
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      expect(rows.length).toBe(1);
      expect(within(rows[0]).getByText('Test info message')).toBeInTheDocument();
      expect(screen.queryByText('Test error message')).not.toBeInTheDocument();
    });

    it('combines multiple filters when more than one filter is used', async () => {
      const user = await setupSearchResults();

      // Show filters
      await user.click(screen.getByText('Show Filters'));

      // Enter filter values for level and source
      const levelFilter = screen.getByLabelText('Filter by Level');
      await user.type(levelFilter, 'INFO');
      
      const sourceFilter = screen.getByLabelText('Filter by Source');
      await user.type(sourceFilter, 'app.log');

      // Check that only logs matching both filters are shown
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      expect(rows.length).toBe(1);
      expect(within(rows[0]).getByText('INFO')).toBeInTheDocument();
      expect(within(rows[0]).getByText('app.log')).toBeInTheDocument();
    });

    it('hides filter controls when Hide Filters button is clicked', async () => {
      const user = await setupSearchResults();

      // Show filters
      await user.click(screen.getByText('Show Filters'));
      
      // Check that filter inputs are visible
      expect(screen.getByLabelText('Filter by Level')).toBeInTheDocument();
      
      // Hide filters
      await user.click(screen.getByText('Hide Filters'));
      
      // Check that filter inputs are no longer visible
      expect(screen.queryByLabelText('Filter by Level')).not.toBeInTheDocument();
    });

    it('combines sorting and filtering when both are used', async () => {
      const user = await setupSearchResults();

      // Show filters
      await user.click(screen.getByText('Show Filters'));

      // Enter a filter value for level
      const levelFilter = screen.getByLabelText('Filter by Level');
      await user.type(levelFilter, 'E'); // This will match ERROR but not INFO

      // Sort by timestamp
      const timestampHeader = screen.getByText('Timestamp').closest('th');
      await user.click(timestampHeader!);

      // Check that only ERROR logs are shown and they're sorted by timestamp
      const rows = screen.getAllByRole('row').slice(1); // Skip header row
      expect(rows.length).toBe(1);
      expect(within(rows[0]).getByText('ERROR')).toBeInTheDocument();
      expect(screen.queryByText('INFO')).not.toBeInTheDocument();
    });
  });
});