import { apiUrl } from '@/config';
import { authHeader } from '@/api/http';

export interface SettingsProperties {
  ldapEnabled: boolean;
}


export const getSettingsProperties = async (): Promise<SettingsProperties> => {
  const response = await fetch(apiUrl('/api/settings/properties'), {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...authHeader(),
    },
  });
  if (!response.ok) {
    let message = 'Failed to load settings properties';
    try {
      const err = await response.json();
      message = err?.error || message;
    } catch {
      try { message = await response.text(); } catch {}
    }
    throw new Error(message);
  }
  try {
    return await response.json();
  } catch {
    throw new Error('Failed to parse settings properties');
  }
};

export const settingsPropertiesApi = { getSettingsProperties };
export default settingsPropertiesApi;
