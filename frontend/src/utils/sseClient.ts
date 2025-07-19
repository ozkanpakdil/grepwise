/**
 * Server-Sent Events (SSE) client utility for real-time updates
 */

type EventCallback = (data: any) => void;
type ErrorCallback = (error: Event) => void;

interface SSEOptions {
  onOpen?: () => void;
  onError?: ErrorCallback;
  withCredentials?: boolean;
  retryInterval?: number;
  maxRetries?: number;
}

interface EventListeners {
  [eventName: string]: EventCallback[];
}

class SSEClient {
  private eventSource: EventSource | null = null;
  private url: string;
  private options: SSEOptions;
  private eventListeners: EventListeners = {};
  private retryCount = 0;
  private isConnecting = false;
  private reconnectTimeout: NodeJS.Timeout | null = null;

  /**
   * Creates a new SSE client
   * @param url The URL to connect to
   * @param options Configuration options
   */
  constructor(url: string, options: SSEOptions = {}) {
    this.url = url;
    this.options = {
      withCredentials: false,
      retryInterval: 5000, // 5 seconds
      maxRetries: 5,
      ...options,
    };
  }

  /**
   * Connects to the SSE endpoint
   */
  connect(): void {
    if (this.eventSource || this.isConnecting) {
      return;
    }

    this.isConnecting = true;

    try {
      this.eventSource = new EventSource(this.url, {
        withCredentials: this.options.withCredentials,
      });

      // Handle connection open
      this.eventSource.onopen = () => {
        this.retryCount = 0;
        this.isConnecting = false;
        if (this.options.onOpen) {
          this.options.onOpen();
        }
        console.log(`SSE connection established to ${this.url}`);
      };

      // Handle errors
      this.eventSource.onerror = (error) => {
        this.isConnecting = false;
        this.handleError(error);
      };

      // Add default message handler
      this.eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          this.dispatchEvent('message', data);
        } catch (error) {
          console.error('Error parsing SSE message:', error);
          this.dispatchEvent('message', event.data);
        }
      };

      // Add listeners for specific events
      Object.keys(this.eventListeners).forEach((eventName) => {
        if (eventName !== 'message') {
          this.addEventSourceListener(eventName);
        }
      });
    } catch (error) {
      this.isConnecting = false;
      console.error('Error creating EventSource:', error);
      this.scheduleReconnect();
    }
  }

  /**
   * Adds a listener for a specific event
   * @param eventName The name of the event to listen for
   * @param callback The callback to execute when the event is received
   */
  on(eventName: string, callback: EventCallback): void {
    if (!this.eventListeners[eventName]) {
      this.eventListeners[eventName] = [];
      
      // If already connected, add the event listener to the EventSource
      if (this.eventSource && eventName !== 'message') {
        this.addEventSourceListener(eventName);
      }
    }
    
    this.eventListeners[eventName].push(callback);
  }

  /**
   * Removes a listener for a specific event
   * @param eventName The name of the event
   * @param callback The callback to remove
   */
  off(eventName: string, callback?: EventCallback): void {
    if (!this.eventListeners[eventName]) {
      return;
    }

    if (callback) {
      this.eventListeners[eventName] = this.eventListeners[eventName].filter(
        (cb) => cb !== callback
      );
    } else {
      delete this.eventListeners[eventName];
    }
  }

  /**
   * Closes the SSE connection
   */
  close(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.isConnecting = false;
    this.retryCount = 0;
  }

  /**
   * Adds an event listener to the EventSource
   * @param eventName The name of the event to listen for
   */
  private addEventSourceListener(eventName: string): void {
    if (!this.eventSource) return;

    this.eventSource.addEventListener(eventName, (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        this.dispatchEvent(eventName, data);
      } catch (error) {
        console.error(`Error parsing SSE ${eventName} event:`, error);
        this.dispatchEvent(eventName, event.data);
      }
    });
  }

  /**
   * Dispatches an event to all registered callbacks
   * @param eventName The name of the event
   * @param data The event data
   */
  private dispatchEvent(eventName: string, data: any): void {
    const callbacks = this.eventListeners[eventName];
    if (callbacks) {
      callbacks.forEach((callback) => {
        try {
          callback(data);
        } catch (error) {
          console.error(`Error in SSE ${eventName} callback:`, error);
        }
      });
    }
  }

  /**
   * Handles connection errors
   * @param error The error event
   */
  private handleError(error: Event): void {
    if (this.options.onError) {
      this.options.onError(error);
    }

    // Close the current connection
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.scheduleReconnect();
  }

  /**
   * Schedules a reconnection attempt
   */
  private scheduleReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    this.retryCount++;
    
    if (this.options.maxRetries && this.retryCount > this.options.maxRetries) {
      console.error(`SSE connection failed after ${this.retryCount - 1} retries`);
      return;
    }

    console.log(`Scheduling SSE reconnect attempt ${this.retryCount} in ${this.options.retryInterval}ms`);
    
    this.reconnectTimeout = setTimeout(() => {
      console.log(`Attempting to reconnect SSE (attempt ${this.retryCount})`);
      this.connect();
    }, this.options.retryInterval);
  }
}

// Create a singleton instance for log updates
let logUpdateClient: SSEClient | null = null;

/**
 * Gets or creates an SSE client for log updates
 * @param query The log search query
 * @param isRegex Whether the query is a regex
 * @param timeRange The time range for the query
 */
export const getLogUpdateClient = (
  query?: string,
  isRegex?: boolean,
  timeRange?: string
): SSEClient => {
  if (logUpdateClient) {
    logUpdateClient.close();
  }

  // Build the URL with query parameters
  let url = 'http://localhost:8080/api/realtime/logs';
  const params = new URLSearchParams();
  
  if (query) params.append('query', query);
  if (isRegex !== undefined) params.append('isRegex', String(isRegex));
  if (timeRange) params.append('timeRange', timeRange);
  
  const queryString = params.toString();
  if (queryString) {
    url += `?${queryString}`;
  }

  logUpdateClient = new SSEClient(url, {
    onOpen: () => console.log('Log update SSE connection established'),
    onError: (error) => console.error('Log update SSE error:', error),
    retryInterval: 3000,
    maxRetries: 10,
  });

  return logUpdateClient;
};

// Create a singleton instance for widget updates
let widgetUpdateClients: Record<string, SSEClient> = {};

/**
 * Gets or creates an SSE client for widget updates
 * @param dashboardId The dashboard ID
 * @param widgetId The widget ID
 */
export const getWidgetUpdateClient = (
  dashboardId: string,
  widgetId: string
): SSEClient => {
  const clientKey = `${dashboardId}:${widgetId}`;
  
  if (widgetUpdateClients[clientKey]) {
    return widgetUpdateClients[clientKey];
  }

  const url = `http://localhost:8080/api/realtime/widgets/${widgetId}?dashboardId=${dashboardId}`;
  
  const client = new SSEClient(url, {
    onOpen: () => console.log(`Widget update SSE connection established for widget ${widgetId}`),
    onError: (error) => console.error(`Widget update SSE error for widget ${widgetId}:`, error),
    retryInterval: 3000,
    maxRetries: 10,
  });

  widgetUpdateClients[clientKey] = client;
  return client;
};

/**
 * Closes all widget update SSE connections
 */
export const closeAllWidgetConnections = (): void => {
  Object.values(widgetUpdateClients).forEach((client) => client.close());
  widgetUpdateClients = {};
};

export default SSEClient;