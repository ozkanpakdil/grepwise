/**
 * API client for LDAP settings operations.
 * This module provides functions to interact with the LDAP settings API endpoints.
 */

/**
 * LDAP settings interface.
 */
import { apiUrl } from '@/config';

export interface LdapSettings {
  enabled: boolean;
  url: string;
  baseDn: string;
  userDnPattern: string;
  managerDn: string;
  managerPassword: string;
  userSearchBase: string;
  userSearchFilter: string;
  groupSearchBase: string;
  groupSearchFilter: string;
  groupRoleAttribute: string;
}

/**
 * Get the current LDAP settings.
 *
 * @returns The current LDAP settings
 */
export const getLdapSettings = async (): Promise<LdapSettings> => {
  try {
    const response = await fetch(apiUrl('/api/settings/ldap'), {
      method: 'GET',
    });

    if (!response.ok) {
      let message = response.statusText;
      try {
        const err = await response.json();
        message = err.error || message;
      } catch (_) {
        // ignore JSON parse errors
      }
      throw new Error(`Failed to get LDAP settings: ${message}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error getting LDAP settings:', error);
    throw error;
  }
};

/**
 * Update LDAP settings.
 *
 * @param settings The new LDAP settings
 * @returns A success message
 */
export const updateLdapSettings = async (settings: LdapSettings): Promise<{ message: string }> => {
  try {
    const response = await fetch(apiUrl('/api/settings/ldap'), {
      method: 'PUT',

      body: JSON.stringify(settings),
    });

    if (!response.ok) {
      let message = response.statusText;
      try {
        const err = await response.json();
        message = err.error || message;
      } catch (_) {}
      throw new Error(message.startsWith('Failed') ? message : `Failed to update LDAP settings: ${message}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error updating LDAP settings:', error);
    throw error;
  }
};

/**
 * Test LDAP connection with the current settings.
 *
 * @returns A success message if the connection is successful
 */
export const testLdapConnection = async (): Promise<{ message: string }> => {
  try {
    const response = await fetch(apiUrl('/api/settings/ldap/test'), {
      method: 'POST',
    });

    if (!response.ok) {
      let message = response.statusText;
      try {
        const err = await response.json();
        message = err.error || message;
      } catch (_) {}
      throw new Error(message.startsWith('Failed') ? message : `Failed to test LDAP connection: ${message}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error testing LDAP connection:', error);
    throw error;
  }
};

/**
 * LDAP settings API client.
 */
export const ldapSettingsApi = {
  getLdapSettings,
  updateLdapSettings,
  testLdapConnection,
};

export default ldapSettingsApi;
