import { useCallback } from 'react';
import { SearchParams } from '@/api/logSearch';

export type TimeRange = SearchParams['timeRange'];

export function useTimeRange() {
  const computeRange = useCallback((timeRange: TimeRange, customStart?: number, customEnd?: number, override?: { start: number; end: number }) => {
    let startTime: number;
    let endTime: number;

    if (override) {
      startTime = override.start;
      endTime = override.end;
    } else if (timeRange === 'custom' && customStart && customEnd) {
      startTime = customStart;
      endTime = customEnd;
    } else {
      endTime = Date.now();
      switch (timeRange) {
        case '1h':
          startTime = endTime - 60 * 60 * 1000;
          break;
        case '3h':
          startTime = endTime - 3 * 60 * 60 * 1000;
          break;
        case '12h':
          startTime = endTime - 12 * 60 * 60 * 1000;
          break;
        case '24h':
        default:
          startTime = endTime - 24 * 60 * 60 * 1000;
          break;
      }
    }

    return { startTime, endTime };
  }, []);

  const pickInterval = useCallback((startTime: number, endTime: number) => {
    const timeRangeMs = endTime - startTime;
    if (timeRangeMs <= 60 * 60 * 1000) return '1m';
    if (timeRangeMs <= 3 * 60 * 60 * 1000) return '5m';
    if (timeRangeMs <= 12 * 60 * 60 * 1000) return '15m';
    return '30m';
  }, []);

  return { computeRange, pickInterval };
}
