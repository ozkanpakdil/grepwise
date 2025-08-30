// Shared HTTP utilities for API calls
// Implements Option B: read tokens from persisted localStorage key 'grepwise-auth'

export type PersistedAuthState = {
  state?: {
    accessToken?: string | null;
    refreshToken?: string | null;
    user?: unknown;
    isAuthenticated?: boolean;
  };
  version?: number;
};

export const getAuthState = (): PersistedAuthState => {
  try {
    if (typeof window === 'undefined') return {};
    const raw = localStorage.getItem('grepwise-auth');
    return raw ? (JSON.parse(raw) as PersistedAuthState) : {};
  } catch {
    return {};
  }
};

export const setAuthState = (partial: PersistedAuthState["state"]) => {
  try {
    if (typeof window === 'undefined') return;
    const current = getAuthState();
    const next: PersistedAuthState = {
      version: current.version ?? 0,
      state: { ...(current.state || {}), ...(partial || {}) },
    };
    localStorage.setItem('grepwise-auth', JSON.stringify(next));
    try {
      // Notify this tab's React app since 'storage' doesn't fire in same tab
      window.dispatchEvent(new Event('grepwise-auth-changed'));
    } catch {}
  } catch {
    // ignore
  }
};

export const clearAuthState = () => {
  try {
    if (typeof window === 'undefined') return;
    localStorage.removeItem('grepwise-auth');
  } catch {
    // ignore
  }
};

export const getAccessToken = (): string | null => {
  const st = getAuthState();
  const token = st?.state?.accessToken || null;
  return token || null;
};

export const authHeader = (): Record<string, string> => {
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};
