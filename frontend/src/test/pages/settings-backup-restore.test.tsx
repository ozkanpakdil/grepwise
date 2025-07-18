import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { ThemeProvider } from '@/components/theme-provider';
import SettingsPage from '@/pages/settings';
import { configBackupApi } from '@/api/configBackup';
import { getLogDirectoryConfigs } from '@/api/logDirectoryConfig';
import { getRetentionPolicies } from '@/api/retentionPolicy';
import { fieldConfigurationApi } from '@/api/fieldConfiguration';

// Mock the API modules
jest.mock('@/api/configBackup');
jest.mock('@/api/logDirectoryConfig');
jest.mock('@/api/retentionPolicy');
jest.mock('@/api/fieldConfiguration');
jest.mock('@/components/theme-provider', () => ({
  ThemeProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  useTheme: () => ({ theme: 'light', setTheme: jest.fn() }),
}));
jest.mock('@/store/auth-store', () => ({
  useAuthStore: () => ({
    user: { firstName: 'Test', lastName: 'User', email: 'test@example.com', username: 'testuser', roles: ['admin'] },
    updateUser: jest.fn(),
  }),
}));

describe('Settings Page - Backup and Restore', () => {
  beforeEach(() => {
    // Reset mocks
    jest.clearAllMocks();
    
    // Mock API responses
    (configBackupApi.exportConfigurations as jest.Mock).mockResolvedValue(undefined);
    (configBackupApi.importConfigurations as jest.Mock).mockResolvedValue({
      logDirectoryConfigsImported: 2,
      retentionPoliciesImported: 1,
      fieldConfigurationsImported: 3,
      totalImported: 6,
    });
    (getLogDirectoryConfigs as jest.Mock).mockResolvedValue([]);
    (getRetentionPolicies as jest.Mock).mockResolvedValue([]);
    (fieldConfigurationApi.getFieldConfigurations as jest.Mock).mockResolvedValue([]);
  });

  test('renders backup and restore section', async () => {
    render(
      <ThemeProvider>
        <SettingsPage />
      </ThemeProvider>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('Backup and Restore')).toBeInTheDocument();
    });

    // Check if export section is rendered
    expect(screen.getByText('Export Configurations')).toBeInTheDocument();
    expect(screen.getByText(/Export all your configurations/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export Configurations' })).toBeInTheDocument();

    // Check if import section is rendered
    expect(screen.getByText('Import Configurations')).toBeInTheDocument();
    expect(screen.getByText(/Import configurations from a previously exported JSON file/)).toBeInTheDocument();
    expect(screen.getByLabelText('Configuration File')).toBeInTheDocument();
    expect(screen.getByLabelText('Overwrite existing configurations')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Import Configurations' })).toBeDisabled(); // Should be disabled initially
  });

  test('clicking export button calls exportConfigurations API', async () => {
    render(
      <ThemeProvider>
        <SettingsPage />
      </ThemeProvider>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('Backup and Restore')).toBeInTheDocument();
    });

    // Click the export button
    const exportButton = screen.getByRole('button', { name: 'Export Configurations' });
    fireEvent.click(exportButton);

    // Check if the API was called
    await waitFor(() => {
      expect(configBackupApi.exportConfigurations).toHaveBeenCalledTimes(1);
    });
  });

  test('selecting a file enables the import button', async () => {
    render(
      <ThemeProvider>
        <SettingsPage />
      </ThemeProvider>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('Backup and Restore')).toBeInTheDocument();
    });

    // Import button should be disabled initially
    const importButton = screen.getByRole('button', { name: 'Import Configurations' });
    expect(importButton).toBeDisabled();

    // Select a file
    const fileInput = screen.getByLabelText('Configuration File');
    const file = new File(['{}'], 'config.json', { type: 'application/json' });
    fireEvent.change(fileInput, { target: { files: [file] } });

    // Import button should be enabled after selecting a file
    expect(importButton).not.toBeDisabled();
  });

  test('submitting import form calls importConfigurations API', async () => {
    render(
      <ThemeProvider>
        <SettingsPage />
      </ThemeProvider>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('Backup and Restore')).toBeInTheDocument();
    });

    // Select a file
    const fileInput = screen.getByLabelText('Configuration File');
    const file = new File(['{}'], 'config.json', { type: 'application/json' });
    fireEvent.change(fileInput, { target: { files: [file] } });

    // Check the overwrite checkbox
    const overwriteCheckbox = screen.getByLabelText('Overwrite existing configurations');
    fireEvent.click(overwriteCheckbox);

    // Submit the form
    const importButton = screen.getByRole('button', { name: 'Import Configurations' });
    fireEvent.click(importButton);

    // Check if the API was called with the correct parameters
    await waitFor(() => {
      expect(configBackupApi.importConfigurations).toHaveBeenCalledTimes(1);
      expect(configBackupApi.importConfigurations).toHaveBeenCalledWith(file, true);
    });

    // Check if the import summary is displayed
    await waitFor(() => {
      expect(screen.getByText('Import Summary')).toBeInTheDocument();
      expect(screen.getByText('Log Directory Configurations: 2')).toBeInTheDocument();
      expect(screen.getByText('Retention Policies: 1')).toBeInTheDocument();
      expect(screen.getByText('Field Configurations: 3')).toBeInTheDocument();
      expect(screen.getByText('Total Items Imported: 6')).toBeInTheDocument();
    });

    // Check if the configurations were refreshed
    expect(getLogDirectoryConfigs).toHaveBeenCalledTimes(1);
    expect(getRetentionPolicies).toHaveBeenCalledTimes(1);
    expect(fieldConfigurationApi.getFieldConfigurations).toHaveBeenCalledTimes(1);
  });
});