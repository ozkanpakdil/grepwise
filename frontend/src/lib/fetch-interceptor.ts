// Global fetch interceptor to automatically attach Authorization header for API requests
// Also handles 401 by attempting a token refresh once, then retrying the original request.
// Works with Option B: token in localStorage under 'grepwise-auth'

import { config } from '@/config';
import { getAccessToken, getAuthState, updateTokens, logout } from '@/api/http';

// Single-flight refresh holder
let refreshInFlight: Promise<boolean> | null = null;

async function refreshTokenOnce(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = (async () => {
    try {
      const st = getAuthState();
      const refreshToken = st?.state?.refreshToken;
      if (!refreshToken) return false;

      const res = await fetch(`${config.apiBaseUrl}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (!res.ok) return false;
      const data = await res.json().catch(() => null);
      if (!data || !data.accessToken) return false;

      updateTokens(data.accessToken, data.refreshToken ?? refreshToken, data.user);
      return true;
    } catch {
      return false;
    } finally {
      // Allow next refresh attempt after resolving all awaiters
      setTimeout(() => {
        refreshInFlight = null;
      }, 0);
    }
  })();

  return refreshInFlight;
}

// Idempotent install: don't patch twice
export function installFetchAuthInterceptor() {
  if (typeof window === 'undefined') return;
  const w = window as unknown as { __GW_FETCH_PATCHED__?: boolean; fetch: typeof window.fetch };
  if (w.__GW_FETCH_PATCHED__) return;

  const originalFetch = w.fetch.bind(window);

  w.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    try {
      // Resolve URL to absolute string
      const urlString = (() => {
        if (typeof input === 'string') return input;
        if (input instanceof URL) return input.toString();
        try {
          return input.url;
        } catch {
          return String(input as any);
        }
      })();

      const isRelativeApi = urlString.startsWith('/api');
      const apiBase = config.apiBaseUrl.replace(/\/$/, '');
      const isAbsoluteToApi = urlString.startsWith(apiBase + '/');

      const shouldAttach = isRelativeApi || isAbsoluteToApi;

      // Skip attaching for authentication endpoints (login/refresh/logout/etc.)
      const isAuthEndpoint = urlString.includes('/api/auth/');

      let nextInput = input;
      let nextInit: RequestInit | undefined = init;

      if (shouldAttach && !isAuthEndpoint) {
        // Build headers preserving caller-provided headers
        const headers = new Headers(
          (init && init.headers) ||
            (typeof input !== 'string' && !(input instanceof URL) ? (input as Request).headers : undefined)
        );

        // Only set Authorization if not already set
        if (!headers.has('Authorization')) {
          const token = getAccessToken();
          if (token) {
            headers.set('Authorization', `Bearer ${token}`);
          }
        }

        nextInit = { ...(init || {}), headers };
      }

      // Mark to avoid infinite retry loops
      const hasRetried = (nextInit as any)?.__retired === true;

      const response = await originalFetch(nextInput, nextInit);

      if (response.status !== 401 || isAuthEndpoint || hasRetried) {
        return response;
      }

      // Attempt refresh once
      const ok = await refreshTokenOnce();
      if (!ok) {
        logout(true);
        return response; // Give original 401 back (UI may handle)
      }

      // Retry original request with new token
      const retryHeaders = new Headers(
        (nextInit && nextInit.headers) ||
          (typeof input !== 'string' && !(input instanceof URL) ? (input as Request).headers : undefined)
      );
      const newAccess = getAccessToken();
      if (newAccess) {
        retryHeaders.set('Authorization', `Bearer ${newAccess}`);
      } else {
        // If still no token somehow, logout
        logout(true);
        return response;
      }

      const retryInit: RequestInit = { ...(nextInit || {}), headers: retryHeaders } as RequestInit;
      (retryInit as any).__retired = true;
      return originalFetch(nextInput, retryInit);
    } catch {
      // On any error in patch logic, fall back to original fetch
      return originalFetch(input as any, init as any);
    }
  };

  w.__GW_FETCH_PATCHED__ = true;
}

// Auto-install when module is imported in the browser
if (typeof window !== 'undefined') {
  try {
    installFetchAuthInterceptor();
  } catch {}
}
