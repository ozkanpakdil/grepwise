/**
 * API client for configuration backup and restore operations.
 * This module provides functions to interact with the configuration backup API endpoints.
 */

/**
 * Export all configurations as a JSON file.
 * This function triggers a file download in the browser.
 */
export const exportConfigurations = async (): Promise<void> => {
  try {
    // Use window.location to trigger a file download
    window.location.href = '/api/config/backup/export';
  } catch (error) {
    console.error('Error exporting configurations:', error);
    throw error;
  }
};

/**
 * Import configurations from a JSON file.
 * 
 * @param file The JSON file containing configurations
 * @param overwrite Whether to overwrite existing configurations
 * @returns A summary of the import operation
 */
export const importConfigurations = async (
  file: File,
  overwrite: boolean = false
): Promise<{
  logDirectoryConfigsImported: number;
  retentionPoliciesImported: number;
  fieldConfigurationsImported: number;
  totalImported: number;
}> => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('overwrite', overwrite.toString());

    const response = await fetch('/api/config/backup/import', {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Failed to import configurations: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error importing configurations:', error);
    throw error;
  }
};

/**
 * Configuration backup API client.
 */
export const configBackupApi = {
  exportConfigurations,
  importConfigurations,
};

export default configBackupApi;