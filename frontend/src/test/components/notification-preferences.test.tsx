import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import NotificationPreferences from '@/components/NotificationPreferences';
import { NotificationChannel } from '@/api/alarm';
import '@testing-library/jest-dom';

describe('NotificationPreferences', () => {
  const mockOnChange = vi.fn();

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  it('renders empty state when no channels are provided', () => {
    render(<NotificationPreferences channels={[]} onChange={mockOnChange} />);

    expect(screen.getByText('No notification channels configured')).toBeInTheDocument();
    expect(screen.getByText('Add channels to receive notifications when this alarm is triggered')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add Channel' })).toBeInTheDocument();
  });

  it('renders a list of notification channels', () => {
    const channels: NotificationChannel[] = [
      { type: 'EMAIL', destination: 'test@example.com' },
      { type: 'SLACK', destination: '#alerts', config: { username: 'AlertBot' } },
      { type: 'WEBHOOK', destination: 'https://example.com/webhook', config: { method: 'POST' } },
    ];

    render(<NotificationPreferences channels={channels} onChange={mockOnChange} />);

    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
    expect(screen.getByText('Slack')).toBeInTheDocument();
    expect(screen.getByText('#alerts')).toBeInTheDocument();
    expect(screen.getByText('Webhook')).toBeInTheDocument();
    expect(screen.getByText('https://example.com/webhook')).toBeInTheDocument();
  });

  it('opens the add dialog when Add Channel button is clicked', async () => {
    render(<NotificationPreferences channels={[]} onChange={mockOnChange} />);

    const addButton = screen.getByRole('button', { name: 'Add Channel' });
    fireEvent.click(addButton);

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText('Add Notification Channel')).toBeInTheDocument();
    });
  });

  it('adds a new email notification channel', async () => {
    const user = userEvent.setup();
    render(<NotificationPreferences channels={[]} onChange={mockOnChange} />);

    // Open the dialog
    const addButton = screen.getByRole('button', { name: 'Add Channel' });
    await user.click(addButton);

    // Fill in the form
    const emailInput = screen.getByLabelText('Email Address');
    await user.type(emailInput, 'new@example.com');

    // Submit the form
    const submitButton = screen.getByRole('button', { name: 'Add' });
    await user.click(submitButton);

    // Check that onChange was called with the new channel
    expect(mockOnChange).toHaveBeenCalledWith([{ type: 'EMAIL', destination: 'new@example.com', config: {} }]);
  });

  it('removes a notification channel', async () => {
    const user = userEvent.setup();
    const channels: NotificationChannel[] = [{ type: 'EMAIL', destination: 'test@example.com' }];

    render(<NotificationPreferences channels={channels} onChange={mockOnChange} />);

    // Click the remove button
    const removeButton = screen.getByRole('button', { name: 'Remove' });
    await user.click(removeButton);

    // Check that onChange was called with an empty array
    expect(mockOnChange).toHaveBeenCalledWith([]);
  });
});
