export type StreamCallbacks = {
  onLogsPage?: (logsJson: string) => void;
  onLogsDone?: (doneJson: string) => void;
  onLogsError?: (e: any) => void;
  onHistInit?: (initJson: string) => void;
  onHistUpdate?: (histJson: string) => void;
  onHistDone?: () => void;
  onHistError?: (e: any) => void;
};

import { getAccessToken } from '@/api/http';

export class StreamingService {
  private logsEs: EventSource | null = null;
  private histEs: EventSource | null = null;

  start(logsUrl: string, cb: StreamCallbacks) {
    this.stopAll();

    const token = getAccessToken();
    const logsWithToken = this.appendToken(logsUrl, token);
    const esLogs = new EventSource(logsWithToken);
    this.logsEs = esLogs;

    // Always listen for log pages and overall done on the main stream
    esLogs.addEventListener('page', (ev: MessageEvent) => {
      cb.onLogsPage?.((ev as any).data);
    });
    esLogs.addEventListener('done', (ev: MessageEvent) => {
      cb.onLogsDone?.((ev as any).data);
      try {
        esLogs.close();
      } catch {}
      this.logsEs = null;
    });
    esLogs.addEventListener('error', (e) => {
      cb.onLogsError?.(e);
    });

    // Consume histogram events (init/hist) directly from the logs stream
    esLogs.addEventListener('init', (ev: MessageEvent) => {
      cb.onHistInit?.((ev as any).data);
    });
  }

  stopAll() {
    if (this.logsEs) {
      try {
        this.logsEs.close();
      } catch {}
      this.logsEs = null;
    }
    if (this.histEs) {
      try {
        this.histEs.close();
      } catch {}
      this.histEs = null;
    }
  }

  private appendToken(url: string, token: string | null): string {
    if (!token) return url;
    try {
      const u = new URL(url, window.location.origin);
      if (!u.searchParams.get('access_token')) {
        u.searchParams.set('access_token', token);
      }
      return u.toString();
    } catch {
      // Fallback if URL constructor fails
      const sep = url.includes('?') ? '&' : '?';
      if (url.includes('access_token=')) return url;
      return `${url}${sep}access_token=${encodeURIComponent(token)}`;
    }
  }
}
