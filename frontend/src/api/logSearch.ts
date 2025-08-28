import axios from 'axios';

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

export const searchLogs = async (params?: SearchParams | string): Promise<LogEntry[]> => {
  // Handle backward compatibility with just a query string
  if (typeof params === 'string') {
    params = { query: params };
  }

  // Build the URL with query parameters
  let url = `${API_URL}/search`;
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

  const response = await axios.get<LogEntry[]>(url);
  return response.data;
};

export const getLogById = async (id: string): Promise<LogEntry> => {
  const response = await axios.get<LogEntry>(`${API_URL}/${id}`);
  return response.data;
};

export const getAllLogs = async (): Promise<LogEntry[]> => {
  const response = await axios.get<LogEntry[]>(API_URL);
  return response.data;
};

export const getLogsByLevel = async (level: string): Promise<LogEntry[]> => {
  const response = await axios.get<LogEntry[]>(`${API_URL}/level/${level}`);
  return response.data;
};

export const getLogsBySource = async (source: string): Promise<LogEntry[]> => {
  const response = await axios.get<LogEntry[]>(`${API_URL}/source/${source}`);
  return response.data;
};

export const getLogsByTimeRange = async (startTime: number, endTime: number): Promise<LogEntry[]> => {
  const response = await axios.get<LogEntry[]>(`${API_URL}/time-range?startTime=${startTime}&endTime=${endTime}`);
  return response.data;
};

export const getLogLevels = async (): Promise<string[]> => {
  const response = await axios.get<string[]>(`${API_URL}/levels`);
  return response.data;
};

export const getLogSources = async (): Promise<string[]> => {
  const response = await axios.get<string[]>(`${API_URL}/sources`);
  return response.data;
};

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

export const getTimeAggregation = async (params?: TimeAggregationParams): Promise<TimeSlot[]> => {
  // Build the URL with query parameters
  let url = `${API_URL}/time-aggregation`;
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
    if (params.slots) {
      queryParams.push(`slots=${params.slots}`);
    }
  }

  if (queryParams.length > 0) {
    url += `?${queryParams.join('&')}`;
  }

  const response = await axios.get<Record<string, number>>(url);

  // Convert the response data to an array of TimeSlot objects
  return Object.entries(response.data).map(([time, count]) => ({
    time: parseInt(time),
    count
  }));
};

export const getHistogram = async (params: HistogramParams): Promise<HistogramData[]> => {
  // Build the URL with query parameters
  let url = `${API_URL}/histogram`;
  const queryParams: string[] = [];

  if (params.query) {
    queryParams.push(`query=${encodeURIComponent(params.query)}`);
  }
  if (params.isRegex) {
    queryParams.push(`isRegex=true`);
  }
  queryParams.push(`from=${params.from}`);
  queryParams.push(`to=${params.to}`);
  queryParams.push(`interval=${params.interval}`);

  if (queryParams.length > 0) {
    url += `?${queryParams.join('&')}`;
  }

  const response = await axios.get<HistogramData[]>(url);
  return response.data;
};

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

export const logSearchApi = {
  search: async (params: { query: string; timeRange?: string; maxResults?: number }) => {
    const searchParams: SearchParams = {
      query: params.query,
      timeRange: params.timeRange as any || '24h'
    };
    
    const [results, timeSlots] = await Promise.all([
      searchLogs(searchParams),
      getTimeAggregation(searchParams)
    ]);
    
    return {
      results: params.maxResults ? results.slice(0, params.maxResults) : results,
      timeSlots,
      total: results.length
    };
  }
};
