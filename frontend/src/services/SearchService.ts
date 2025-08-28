import { SearchParams, exportLogsAsCsv, exportLogsAsJson } from '@/api/logSearch';

export type PageResponse<T> = {
  items: T[];
  total: number;
  page: number;
};

function buildSearchQuery(params: URLSearchParams, opts: {
  query?: string;
  isRegex?: boolean;
  timeRange?: SearchParams['timeRange'];
  startTime?: number;
  endTime?: number;
}) {
  const { query, isRegex, timeRange, startTime, endTime } = opts;
  const trimmed = (query || '').trim();
  if (trimmed && trimmed !== '*') params.set('query', trimmed);
  if (isRegex) params.set('isRegex', 'true');
  if (timeRange !== 'custom' && (!startTime || !endTime)) {
    if (timeRange && timeRange !== '24h') params.set('timeRange', timeRange);
  } else {
    if (startTime != null) params.set('startTime', String(startTime));
    if (endTime != null) params.set('endTime', String(endTime));
  }
}

export const SearchService = {
  buildStreamParams(opts: {
    query?: string;
    isRegex?: boolean;
    timeRange?: SearchParams['timeRange'];
    startTime?: number;
    endTime?: number;
    interval?: string;
    pageSize: number;
  }): URLSearchParams {
    const sp = new URLSearchParams();
    buildSearchQuery(sp, opts);
    if (opts.interval) sp.set('interval', opts.interval);
    sp.set('pageSize', String(opts.pageSize));
    return sp;
  },

  buildHistogramParamsFrom(spLike: URLSearchParams): URLSearchParams {
    const histParams = new URLSearchParams(spLike.toString());
    histParams.delete('pageSize');
    return histParams;
  },

  fetchPage: async <T>(opts: {
    query?: string;
    isRegex?: boolean;
    timeRange?: SearchParams['timeRange'];
    startTime?: number;
    endTime?: number;
    page: number;
    pageSize: number;
  }): Promise<PageResponse<T>> => {
    const sp = new URLSearchParams();
    buildSearchQuery(sp, opts);
    sp.set('page', String(opts.page));
    sp.set('pageSize', String(opts.pageSize));
    const res = await fetch(`http://localhost:8080/api/logs/search/page?${sp.toString()}`);
    if (!res.ok) throw new Error('Failed to fetch page');
    return res.json();
  },

  exportCsvUrl: (params: SearchParams) => exportLogsAsCsv(params),
  exportJsonUrl: (params: SearchParams) => exportLogsAsJson(params),
};
