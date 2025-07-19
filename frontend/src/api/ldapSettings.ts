/**
 * API client for LDAP settings operations.
 * This module provides functions to interact with the LDAP settings API endpoints.
 */

/**
 * LDAP settings interface.
 */
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
    const response = await fetch('/api/settings/ldap', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to get LDAP settings: ${response.statusText}`);
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
    const response = await fetch('/api/settings/ldap', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(settings),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.error || `Failed to update LDAP settings: ${response.statusText}`);
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
    const response = await fetch('/api/settings/ldap/test', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.error || `Failed to test LDAP connection: ${response.statusText}`);
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