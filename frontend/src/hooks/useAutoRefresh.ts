import { useEffect, useRef } from 'react';

export function useAutoRefresh(enabled: boolean, interval: string, onTick: () => void) {
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    if (enabled) {
      const intervalMs = interval === '5s' ? 5000 : interval === '10s' ? 10000 : interval === '30s' ? 30000 : 10000;
      timerRef.current = setInterval(() => onTick(), intervalMs);
    }
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [enabled, interval, onTick]);

  return { timerRef };
}
