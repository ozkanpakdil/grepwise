import axios from 'axios';
import { apiUrl } from '@/config';

export interface FieldConfiguration {
  id?: string;
  name: string;
  description: string;
  fieldType: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';
  extractionPattern: string | null;
  sourceField: string;
  isStored: boolean;
  isIndexed: boolean;
  isTokenized: boolean;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export const fieldConfigurationApi = {
  /**
   * Get all field configurations
   */
  getFieldConfigurations: async (): Promise<FieldConfiguration[]> => {
    const response = await axios.get(apiUrl('/api/config/field-configurations'));
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Get all enabled field configurations
   */
  getEnabledFieldConfigurations: async (): Promise<FieldConfiguration[]> => {
    const response = await axios.get(apiUrl('/api/config/field-configurations/enabled'));
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Get a field configuration by ID
   */
  getFieldConfiguration: async (id: string): Promise<FieldConfiguration> => {
    const response = await axios.get(apiUrl(`/api/config/field-configurations/${id}`));
    return response.data;
  },

  /**
   * Create a new field configuration
   */
  createFieldConfiguration: async (fieldConfiguration: FieldConfiguration): Promise<FieldConfiguration> => {
    const response = await axios.post(apiUrl('/api/config/field-configurations'), fieldConfiguration);
    return response.data;
  },

  /**
   * Update an existing field configuration
   */
  updateFieldConfiguration: async (id: string, fieldConfiguration: FieldConfiguration): Promise<FieldConfiguration> => {
    const response = await axios.put(apiUrl(`/api/config/field-configurations/${id}`), fieldConfiguration);
    return response.data;
  },

  /**
   * Delete a field configuration
   */
  deleteFieldConfiguration: async (id: string): Promise<void> => {
    await axios.delete(apiUrl(`/api/config/field-configurations/${id}`));
  },

  /**
   * Test a field configuration with a sample string
   */
  testFieldConfiguration: async (fieldConfiguration: FieldConfiguration, sampleString: string): Promise<string> => {
    const response = await axios.post(
      apiUrl(`/api/config/field-configurations/test?sampleString=${encodeURIComponent(sampleString)}`),
      fieldConfiguration
    );
    return response.data;
  },
};
