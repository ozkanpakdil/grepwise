import { useCallback } from 'react';
import { SearchParams } from '@/api/logSearch';

export type UrlState = {
  query: string;
  isRegex: boolean;
  timeRange: SearchParams['timeRange'];
  startTime?: number;
  endTime?: number;
  pageSize: number;
  autoRefreshEnabled: boolean;
  autoRefreshInterval: string;
};

export function useUrlState() {
  const serialize = useCallback((state: UrlState) => {
    const sp = new URLSearchParams();
    if (state.query?.trim()) sp.set('query', state.query.trim());
    if (state.isRegex) sp.set('isRegex', 'true');
    sp.set('pageSize', String(state.pageSize));
    if (state.autoRefreshEnabled) {
      sp.set('autoRefresh', 'on');
      sp.set('autoRefreshInterval', state.autoRefreshInterval);
    }
    if (state.timeRange === 'custom' && state.startTime && state.endTime) {
      sp.set('timeRange', 'custom');
      sp.set('startTime', String(state.startTime));
      sp.set('endTime', String(state.endTime));
    } else {
      sp.set('timeRange', state.timeRange || '24h');
    }
    return sp;
  }, []);

  const parse = useCallback((): Partial<SearchParams> & {
    pageSize?: number;
    autoRefreshEnabled?: boolean;
    autoRefreshInterval?: string;
    query?: string;
    isRegex?: boolean;
  } => {
    const sp = new URLSearchParams(window.location.search);
    const parsed: Partial<SearchParams> & {
      pageSize?: number;
      autoRefreshEnabled?: boolean;
      autoRefreshInterval?: string;
      query?: string;
      isRegex?: boolean;
    } = {};
    const q = sp.get('query') || '';
    const isRegex = sp.get('isRegex') === 'true';
    const tr = (sp.get('timeRange') as SearchParams['timeRange']) || undefined;
    const st = sp.get('startTime');
    const et = sp.get('endTime');
    const ps = sp.get('pageSize');
    const ar = sp.get('autoRefresh');
    const ari = sp.get('autoRefreshInterval') || '10s';
    if (q) parsed.query = q;
    if (isRegex) parsed.isRegex = true;
    if (ps) parsed.pageSize = parseInt(ps, 10) || undefined;
    if (ar === 'on') parsed.autoRefreshEnabled = true;
    parsed.autoRefreshInterval = ari;
    if (tr === 'custom' && st && et) {
      parsed.timeRange = 'custom';
      parsed.startTime = Number(st);
      parsed.endTime = Number(et);
    } else if (tr) {
      parsed.timeRange = tr;
    }
    return parsed;
  }, []);

  const push = useCallback((sp: URLSearchParams) => {
    const newUrl = `${window.location.pathname}?${sp.toString()}`;
    window.history.pushState({ type: 'grepwise-search' }, '', newUrl);
  }, []);

  return { serialize, parse, push };
}
