import { apiUrl, config } from '@/config';

const API_URL = apiUrl(config.apiPaths.logs);

export interface LogEntry {
  id: string;
  timestamp: number;
  recordTime?: number;
  level: string;
  message: string;
  source: string;
  metadata: Record<string, string>;
  rawContent: string;
}

export interface SearchParams {
  query?: string;
  isRegex?: boolean;
  timeRange?: '1h' | '3h' | '12h' | '24h' | 'custom';
  startTime?: number;
  endTime?: number;
}

export interface TimeAggregationParams extends SearchParams {
  slots?: number;
}

export interface TimeSlot {
  time: number;
  count: number;
}

export interface HistogramParams {
  query?: string;
  isRegex?: boolean;
  from: number;
  to: number;
  interval: string; // 1m, 5m, 15m, 30m, 1h, 3h, 6h, 12h, 24h
}

export interface HistogramData {
  timestamp: string;
  count: number;
}
// API object for dashboard widgets
export const exportLogsAsCsv = (params: SearchParams): string => {
  // Build the URL with query parameters
  let url = `${API_URL}/export/csv`;
  const queryParams: string[] = [];

  if (params) {
    if (params.query) {
      queryParams.push(`query=${encodeURIComponent(params.query)}`);
    }
    if (params.isRegex) {
      queryParams.push(`isRegex=true`);
    }
    if (params.timeRange) {
      queryParams.push(`timeRange=${params.timeRange}`);
    }
    if (params.startTime) {
      queryParams.push(`startTime=${params.startTime}`);
    }
    if (params.endTime) {
      queryParams.push(`endTime=${params.endTime}`);
    }
  }

  if (queryParams.length > 0) {
    url += `?${queryParams.join('&')}`;
  }

  return url;
};

export const exportLogsAsJson = (params: SearchParams): string => {
  // Build the URL with query parameters
  let url = `${API_URL}/export/json`;
  const queryParams: string[] = [];

  if (params) {
    if (params.query) {
      queryParams.push(`query=${encodeURIComponent(params.query)}`);
    }
    if (params.isRegex) {
      queryParams.push(`isRegex=true`);
    }
    if (params.timeRange) {
      queryParams.push(`timeRange=${params.timeRange}`);
    }
    if (params.startTime) {
      queryParams.push(`startTime=${params.startTime}`);
    }
    if (params.endTime) {
      queryParams.push(`endTime=${params.endTime}`);
    }
  }

  if (queryParams.length > 0) {
    url += `?${queryParams.join('&')}`;
  }

  return url;
};
