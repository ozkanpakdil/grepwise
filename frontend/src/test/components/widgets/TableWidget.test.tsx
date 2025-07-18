import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TableWidget from '@/components/widgets/TableWidget';

describe('TableWidget', () => {
  const mockWidget = {
    id: 'w1',
    title: 'Test Table',
    type: 'table',
    query: 'search *',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 3,
    userId: 'current-user',
  };

  const mockTableData = {
    results: [
      {
        id: '1',
        timestamp: 1640995200000,
        level: 'ERROR',
        message: 'Database connection failed',
        count: 5,
      },
      {
        id: '2',
        timestamp: 1640998800000,
        level: 'INFO',
        message: 'User logged in successfully',
        count: 1,
      },
      {
        id: '3',
        timestamp: 1641002400000,
        level: 'WARNING',
        message: 'High memory usage detected',
        count: 3,
      },
    ],
    timeSlots: [],
    total: 3,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders table with data', () => {
    render(<TableWidget data={mockTableData} widget={mockWidget} />);

    // Check for table headers (should show first 6 columns)
    expect(screen.getByText('Timestamp')).toBeInTheDocument();
    expect(screen.getByText('Level')).toBeInTheDocument();
    expect(screen.getByText('Message')).toBeInTheDocument();
    expect(screen.getByText('Count')).toBeInTheDocument();

    // Check for table data
    expect(screen.getByText('ERROR')).toBeInTheDocument();
    expect(screen.getByText('INFO')).toBeInTheDocument();
    expect(screen.getByText('WARNING')).toBeInTheDocument();
    expect(screen.getByText('Database connection failed')).toBeInTheDocument();
  });

  it('sorts columns by importance (timestamp, level, message first)', () => {
    render(<TableWidget data={mockTableData} widget={mockWidget} />);

    const headers = screen.getAllByRole('columnheader');
    const headerTexts = headers.map(header => header.textContent);

    // Important columns should come first
    expect(headerTexts[0]).toContain('Timestamp');
    expect(headerTexts[1]).toContain('Level');
    expect(headerTexts[2]).toContain('Message');
    expect(headerTexts[3]).toContain('Count');
  });

  it('limits display to 6 columns maximum', () => {
    const dataWithManyColumns = {
      results: [
        {
          col1: 'value1',
          col2: 'value2',
          col3: 'value3',
          col4: 'value4',
          col5: 'value5',
          col6: 'value6',
          col7: 'value7', // Should not be displayed
          col8: 'value8', // Should not be displayed
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<TableWidget data={dataWithManyColumns} widget={mockWidget} />);

    const headers = screen.getAllByRole('columnheader');
    expect(headers.length).toBe(6); // Should be limited to 6 columns
  });

  it('sorts data when column header is clicked', async () => {
    const user = userEvent.setup();
    render(<TableWidget data={mockTableData} widget={mockWidget} />);

    // Click on Level column to sort
    const levelHeader = screen.getByText('Level');
    await user.click(levelHeader);

    // Should show sort indicator
    const sortIcon = levelHeader.parentElement?.querySelector('svg');
    expect(sortIcon).toBeInTheDocument();

    // Data should be sorted (ERROR, INFO, WARNING alphabetically)
    const rows = screen.getAllByRole('row');
    const firstDataRow = rows[1]; // Skip header row
    expect(firstDataRow).toHaveTextContent('ERROR');
  });

  it('toggles sort direction when clicking same column twice', async () => {
    const user = userEvent.setup();
    render(<TableWidget data={mockTableData} widget={mockWidget} />);

    const levelHeader = screen.getByText('Level');
    
    // First click - ascending
    await user.click(levelHeader);
    let rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('ERROR'); // First alphabetically

    // Second click - descending
    await user.click(levelHeader);
    rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('WARNING'); // Last alphabetically
  });

  it('formats different data types correctly', () => {
    const mixedTypeData = {
      results: [
        {
          timestamp: 1640995200000,
          count: 42,
          message: 'Test message',
          date_field: '2024-01-01T00:00:00Z',
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<TableWidget data={mixedTypeData} widget={mockWidget} />);

    // Number should be formatted with locale
    expect(screen.getByText('42')).toBeInTheDocument();
    
    // Timestamp should be formatted with commas as a number
    expect(screen.getByText('1,640,995,200,000')).toBeInTheDocument();
    
    // Date field should be formatted as locale date string
    expect(screen.getByText(/01\/01\/2024, 00:00:00/)).toBeInTheDocument();
    
    // String should remain as-is
    expect(screen.getByText('Test message')).toBeInTheDocument();
  });

  it('handles null and undefined values', () => {
    const dataWithNulls = {
      results: [
        {
          id: '1',
          message: 'Valid message',
          empty_field: null,
          undefined_field: undefined,
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<TableWidget data={dataWithNulls} widget={mockWidget} />);

    // Null/undefined values should show as '-'
    const nullCells = screen.getAllByText('-');
    expect(nullCells.length).toBeGreaterThan(0);
    
    // Valid data should still show
    expect(screen.getByText('Valid message')).toBeInTheDocument();
  });

  it('shows pagination when data exceeds 10 items', () => {
    const largeData = {
      results: Array.from({ length: 25 }, (_, i) => ({
        id: `${i + 1}`,
        message: `Message ${i + 1}`,
        count: i + 1,
      })),
      timeSlots: [],
      total: 25,
    };

    render(<TableWidget data={largeData} widget={mockWidget} />);

    // Should show pagination controls
    expect(screen.getByText('1 of 3')).toBeInTheDocument(); // 25 items / 10 per page = 3 pages
    expect(screen.getByText('25 rows')).toBeInTheDocument();

    // Should show only first 10 items
    expect(screen.getByText('Message 1')).toBeInTheDocument();
    expect(screen.getByText('Message 10')).toBeInTheDocument();
    expect(screen.queryByText('Message 11')).not.toBeInTheDocument();
  });

  it('navigates between pages', async () => {
    const user = userEvent.setup();
    const largeData = {
      results: Array.from({ length: 25 }, (_, i) => ({
        id: `${i + 1}`,
        message: `Message ${i + 1}`,
        count: i + 1,
      })),
      timeSlots: [],
      total: 25,
    };

    render(<TableWidget data={largeData} widget={mockWidget} />);

    // Go to next page (second button in pagination)
    const buttons = screen.getAllByRole('button');
    const nextButton = buttons[1]; // Second button is next
    await user.click(nextButton);

    // Should show page 2 data
    expect(screen.getByText('2 of 3')).toBeInTheDocument();
    expect(screen.getByText('Message 11')).toBeInTheDocument();
    expect(screen.getByText('Message 20')).toBeInTheDocument();
    expect(screen.queryByText('Message 1')).not.toBeInTheDocument();

    // Go back to previous page (first button in pagination)
    const buttonsAfter = screen.getAllByRole('button');
    const prevButton = buttonsAfter[0]; // First button is previous
    await user.click(prevButton);

    // Should show page 1 data again
    expect(screen.getByText('1 of 3')).toBeInTheDocument();
    expect(screen.getByText('Message 1')).toBeInTheDocument();
  });

  it('disables pagination buttons appropriately', async () => {
    const user = userEvent.setup();
    const largeData = {
      results: Array.from({ length: 25 }, (_, i) => ({
        id: `${i + 1}`,
        message: `Message ${i + 1}`,
        count: i + 1,
      })),
      timeSlots: [],
      total: 25,
    };

    render(<TableWidget data={largeData} widget={mockWidget} />);

    const buttons = screen.getAllByRole('button');
    const prevButton = buttons[0]; // First button is previous
    const nextButton = buttons[1]; // Second button is next

    // On first page, previous should be disabled
    expect(prevButton).toBeDisabled();
    expect(nextButton).not.toBeDisabled();

    // Go to last page
    await user.click(nextButton);
    await user.click(nextButton);

    // On last page, next should be disabled
    expect(screen.getByText('3 of 3')).toBeInTheDocument();
    expect(nextButton).toBeDisabled();
    expect(prevButton).not.toBeDisabled();
  });

  it('resets to first page when sorting', async () => {
    const user = userEvent.setup();
    const largeData = {
      results: Array.from({ length: 25 }, (_, i) => ({
        id: `${i + 1}`,
        message: `Message ${i + 1}`,
        count: i + 1,
      })),
      timeSlots: [],
      total: 25,
    };

    render(<TableWidget data={largeData} widget={mockWidget} />);

    // Go to page 2
    const buttons = screen.getAllByRole('button');
    const nextButton = buttons[1]; // Second button is next
    await user.click(nextButton);
    expect(screen.getByText('2 of 3')).toBeInTheDocument();

    // Sort by a column
    const messageHeader = screen.getByText('Message');
    await user.click(messageHeader);

    // Should reset to page 1
    expect(screen.getByText('1 of 3')).toBeInTheDocument();
  });

  it('shows no data message when data is empty', () => {
    const emptyData = {
      results: [],
      timeSlots: [],
      total: 0,
    };

    render(<TableWidget data={emptyData} widget={mockWidget} />);

    expect(screen.getByText('No data available for table display')).toBeInTheDocument();
  });

  it('shows no data message when data is null', () => {
    render(<TableWidget data={null} widget={mockWidget} />);

    expect(screen.getByText('No data available for table display')).toBeInTheDocument();
  });

  it('shows no data message when results is missing', () => {
    const dataWithoutResults = {
      timeSlots: [],
      total: 0,
    };

    render(<TableWidget data={dataWithoutResults} widget={mockWidget} />);

    expect(screen.getByText('No data available for table display')).toBeInTheDocument();
  });

  it('applies hover effects to table rows', () => {
    render(<TableWidget data={mockTableData} widget={mockWidget} />);

    const rows = screen.getAllByRole('row');
    const dataRows = rows.slice(1); // Skip header row

    dataRows.forEach(row => {
      expect(row).toHaveClass('hover:bg-gray-50');
    });
  });

  it('truncates long cell content with tooltip', () => {
    const dataWithLongContent = {
      results: [
        {
          id: '1',
          message: 'This is a very long message that should be truncated in the table cell but still be accessible via tooltip',
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<TableWidget data={dataWithLongContent} widget={mockWidget} />);

    const cellContent = screen.getByText(/This is a very long message/);
    const cell = cellContent.closest('td');
    
    expect(cell).toHaveClass('truncate');
    expect(cellContent).toHaveClass('truncate');
    expect(cellContent).toHaveAttribute('title');
  });

  it('detects column types correctly', () => {
    const typedData = {
      results: [
        {
          string_field: 'text',
          number_field: 42,
          timestamp: 1640995200000,
          date_field: '2024-01-01T00:00:00Z',
          time_field: 'some_time_value',
        },
      ],
      timeSlots: [],
      total: 1,
    };

    render(<TableWidget data={typedData} widget={mockWidget} />);

    // Number should be displayed as number
    expect(screen.getByText('42')).toBeInTheDocument();
    
    // String should be displayed as string
    expect(screen.getByText('text')).toBeInTheDocument();
    
    // Time-related fields should be formatted as dates
    const dateElements = screen.getAllByText(/2024/);
    expect(dateElements.length).toBeGreaterThan(0);
  });

  it('sorts different data types correctly', async () => {
    const user = userEvent.setup();
    const mixedData = {
      results: [
        { id: '1', count: 30, level: 'ERROR' },
        { id: '2', count: 10, level: 'INFO' },
        { id: '3', count: 20, level: 'WARNING' },
      ],
      timeSlots: [],
      total: 3,
    };

    render(<TableWidget data={mixedData} widget={mockWidget} />);

    // Sort by count (numeric)
    const countHeader = screen.getByText('Count');
    await user.click(countHeader);

    const rows = screen.getAllByRole('row');
    const firstDataRow = rows[1];
    expect(firstDataRow).toHaveTextContent('10'); // Smallest number first

    // Sort by level (string)
    const levelHeader = screen.getByText('Level');
    await user.click(levelHeader);

    const sortedRows = screen.getAllByRole('row');
    const firstSortedRow = sortedRows[1];
    expect(firstSortedRow).toHaveTextContent('ERROR'); // Alphabetically first
  });
});