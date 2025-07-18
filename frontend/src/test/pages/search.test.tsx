import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
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
});