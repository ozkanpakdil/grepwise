// Global fetch interceptor to automatically attach Authorization header for API requests
// Works with Option B: token in localStorage under 'grepwise-auth'

import { config } from '@/config';
import { getAccessToken } from '@/api/http';

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

      // Skip attaching for authentication endpoints
      const isAuthEndpoint = urlString.includes('/api/auth/');

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

        const nextInit: RequestInit = { ...(init || {}), headers };
        return originalFetch(input, nextInit);
      }

      return originalFetch(input, init);
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
