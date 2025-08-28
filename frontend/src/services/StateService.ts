import { UrlState } from '@/hooks/useUrlState';

export const StateService = {
  serializeToUrl(state: UrlState): string {
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
    return `${window.location.pathname}?${sp.toString()}`;
  },
};
