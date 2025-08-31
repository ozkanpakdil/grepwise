// Minimal centralized config with build-time (Vite) and optional runtime (window.__APP_CONFIG__) overrides.
export type AppConfig = {
  apiBaseUrl: string;
  apiPaths: { logs: string };
  defaults: { pageSize: number; refreshInterval: string };
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const RUNTIME: Partial<AppConfig> = (typeof window !== 'undefined' && (window as any).__APP_CONFIG__) || {};
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const ENV: any = (typeof import.meta !== 'undefined' && (import.meta as any).env) || {};

const DEFAULTS: AppConfig = {
  apiBaseUrl: 'http://localhost:8080',
  apiPaths: { logs: '/api/logs' },
  defaults: { pageSize: 100, refreshInterval: '30s' },
};

const toNumber = (v: unknown, fb: number) => {
  const n = typeof v === 'string' ? Number(v) : typeof v === 'number' ? v : NaN;
  return Number.isFinite(n) ? (n as number) : fb;
};

export const config: AppConfig = {
  apiBaseUrl: (RUNTIME.apiBaseUrl as string) || ENV.VITE_API_BASE_URL || DEFAULTS.apiBaseUrl,
  apiPaths: { logs: (RUNTIME.apiPaths?.logs as string) || DEFAULTS.apiPaths.logs },
  defaults: {
    pageSize: toNumber(
      (RUNTIME.defaults?.pageSize as number | string) ?? ENV.VITE_PAGE_SIZE,
      DEFAULTS.defaults.pageSize
    ),
    refreshInterval:
      (RUNTIME.defaults?.refreshInterval as string) || ENV.VITE_REFRESH_INTERVAL || DEFAULTS.defaults.refreshInterval,
  },
};

export const apiUrl = (path: string) => `${config.apiBaseUrl}${path}`;
