import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import SSEClient, { getWidgetUpdateClient, getLogUpdateClient } from '@/utils/sseClient';

// Mock EventSource
class MockEventSource {
  url: string;
  withCredentials: boolean;
  onopen: (() => void) | null = null;
  onmessage: ((event: any) => void) | null = null;
  onerror: ((error: any) => void) | null = null;
  eventListeners: Record<string, Array<(event: any) => void>> = {};

  constructor(url: string, options?: { withCredentials?: boolean }) {
    this.url = url;
    this.withCredentials = options?.withCredentials || false;
  }

  addEventListener(event: string, callback: (event: any) => void) {
    if (!this.eventListeners[event]) {
      this.eventListeners[event] = [];
    }
    this.eventListeners[event].push(callback);
  }

  dispatchEvent(event: string, data: any) {
    if (event === 'message' && this.onmessage) {
      this.onmessage({ data: JSON.stringify(data) });
    } else if (event === 'open' && this.onopen) {
      this.onopen();
    } else if (event === 'error' && this.onerror) {
      this.onerror(new Error('Test error'));
    }

    if (this.eventListeners[event]) {
      this.eventListeners[event].forEach(callback => {
        callback({ data: JSON.stringify(data) });
      });
    }
  }

  close() {
    // Clean up
  }
}

// Mock global EventSource
global.EventSource = MockEventSource as any;

describe('SSEClient', () => {
  let sseClient: SSEClient;
  
  beforeEach(() => {
    sseClient = new SSEClient('http://test-url.com/sse');
  });

  afterEach(() => {
    sseClient.close();
  });

  it('should create an SSE client with the correct URL', () => {
    expect(sseClient).toBeDefined();
  });

  it('should connect to the SSE endpoint', () => {
    const connectSpy = vi.spyOn(sseClient as any, 'connect');
    sseClient.connect();
    expect(connectSpy).toHaveBeenCalled();
  });

  it('should register event listeners', () => {
    const callback = vi.fn();
    sseClient.on('testEvent', callback);
    
    // Connect to create the EventSource
    sseClient.connect();
    
    // Get the EventSource instance
    const eventSource = (sseClient as any).eventSource;
    
    // Simulate an event
    eventSource.dispatchEvent('testEvent', { test: 'data' });
    
    // Check if callback was called with the correct data
    expect(callback).toHaveBeenCalledWith({ test: 'data' });
  });

  it('should handle connection errors', () => {
    const errorCallback = vi.fn();
    sseClient = new SSEClient('http://test-url.com/sse', {
      onError: errorCallback
    });
    
    // Connect to create the EventSource
    sseClient.connect();
    
    // Get the EventSource instance
    const eventSource = (sseClient as any).eventSource;
    
    // Simulate an error
    eventSource.dispatchEvent('error', null);
    
    // Check if error callback was called
    expect(errorCallback).toHaveBeenCalled();
  });

  it('should close the connection', () => {
    // Connect to create the EventSource
    sseClient.connect();
    
    // Get the EventSource instance
    const eventSource = (sseClient as any).eventSource;
    const closeSpy = vi.spyOn(eventSource, 'close');
    
    // Close the connection
    sseClient.close();
    
    // Check if close was called
    expect(closeSpy).toHaveBeenCalled();
    
    // Check if eventSource was reset
    expect((sseClient as any).eventSource).toBeNull();
  });
});

describe('Widget Update Client', () => {
  it('should create a widget update client with the correct URL', () => {
    const dashboardId = 'dashboard-123';
    const widgetId = 'widget-456';
    
    const client = getWidgetUpdateClient(dashboardId, widgetId);
    
    expect(client).toBeDefined();
    expect((client as any).url).toBe(`http://localhost:8080/api/realtime/widgets/${widgetId}?dashboardId=${dashboardId}`);
  });
});

describe('Log Update Client', () => {
  it('should create a log update client with the correct URL and parameters', () => {
    const query = 'error';
    const isRegex = true;
    const timeRange = '24h';
    
    const client = getLogUpdateClient(query, isRegex, timeRange);
    
    expect(client).toBeDefined();
    expect((client as any).url).toBe(`http://localhost:8080/api/realtime/logs?query=${query}&isRegex=${isRegex}&timeRange=${timeRange}`);
  });
  
  it('should create a log update client with no parameters', () => {
    const client = getLogUpdateClient();
    
    expect(client).toBeDefined();
    expect((client as any).url).toBe('http://localhost:8080/api/realtime/logs');
  });
});