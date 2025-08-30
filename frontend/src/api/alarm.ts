export interface NotificationChannel {
  type: string;
  destination: string;
  config?: Record<string, any>;
}

export interface Alarm {
  id: string;
  name: string;
  description?: string;
  query: string;
  condition: string;
  threshold: number;
  timeWindowMinutes: number;
  enabled: boolean;
  createdAt: number;
  updatedAt: number;
  notificationChannels: NotificationChannel[];
  throttleWindowMinutes?: number;
  maxNotificationsPerWindow?: number;
  groupingKey?: string;
  groupingWindowMinutes?: number;
}

export interface AlarmRequest {
  name: string;
  description?: string;
  query: string;
  condition: string;
  threshold: number;
  timeWindowMinutes: number;
  enabled: boolean;
  notificationEmail?: string;
  notificationChannels?: NotificationChannel[];
  throttleWindowMinutes?: number;
  maxNotificationsPerWindow?: number;
  groupingKey?: string;
  groupingWindowMinutes?: number;
}

export interface AlarmStatistics {
  totalAlarms: number;
  enabledAlarms: number;
  disabledAlarms: number;
  recentlyTriggered: number;
}

export interface AlarmEvent {
  id: string;
  alarmId: string;
  alarmName: string;
  timestamp: number;
  status: 'TRIGGERED' | 'ACKNOWLEDGED' | 'RESOLVED';
  acknowledgedBy?: string;
  acknowledgedAt?: number;
  resolvedBy?: string;
  resolvedAt?: number;
  matchCount: number;
  details?: string;
}

import { apiUrl } from '@/config';

const API_BASE_URL = apiUrl('/api/alarms');
const API_EVENTS_URL = apiUrl('/api/alarm-events');

// Alarm API functions
export const alarmApi = {
  // Get all alarms
  getAllAlarms: async (): Promise<Alarm[]> => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
      throw new Error('Failed to fetch alarms');
    }
    return response.json();
  },

  // Get a specific alarm by ID
  getAlarmById: async (id: string): Promise<Alarm> => {
    const response = await fetch(`${API_BASE_URL}/${id}`);
    if (!response.ok) {
      throw new Error('Failed to fetch alarm');
    }
    return response.json();
  },

  // Create a new alarm
  createAlarm: async (alarm: AlarmRequest): Promise<Alarm> => {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      body: JSON.stringify(alarm),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create alarm');
    }
    return response.json();
  },

  // Update an alarm
  updateAlarm: async (id: string, alarm: AlarmRequest): Promise<Alarm> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',

      body: JSON.stringify(alarm),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update alarm');
    }
    return response.json();
  },

  // Delete an alarm
  deleteAlarm: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to delete alarm');
    }
  },

  // Toggle alarm enabled/disabled status
  toggleAlarm: async (id: string): Promise<Alarm> => {
    const response = await fetch(`${API_BASE_URL}/${id}/toggle`, {
      method: 'PUT',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to toggle alarm');
    }
    return response.json();
  },

  // Get alarms by enabled status
  getAlarmsByEnabled: async (enabled: boolean): Promise<Alarm[]> => {
    const response = await fetch(`${API_BASE_URL}/enabled/${enabled}`);
    if (!response.ok) {
      throw new Error('Failed to fetch alarms by enabled status');
    }
    return response.json();
  },

  // Evaluate an alarm against current data
  evaluateAlarm: async (id: string): Promise<any> => {
    const response = await fetch(`${API_BASE_URL}/${id}/evaluate`, {
      method: 'POST',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to evaluate alarm');
    }
    return response.json();
  },

  // Get alarm statistics
  getStatistics: async (): Promise<AlarmStatistics> => {
    const response = await fetch(`${API_BASE_URL}/statistics`);
    if (!response.ok) {
      throw new Error('Failed to fetch alarm statistics');
    }
    return response.json();
  },

  // Get all alarm events
  getAlarmEvents: async (): Promise<AlarmEvent[]> => {
    const response = await fetch(API_EVENTS_URL);
    if (!response.ok) {
      throw new Error('Failed to fetch alarm events');
    }
    return response.json();
  },

  // Get alarm events by status
  getAlarmEventsByStatus: async (status: AlarmEvent['status']): Promise<AlarmEvent[]> => {
    const response = await fetch(`${API_EVENTS_URL}/status/${status}`);
    if (!response.ok) {
      throw new Error('Failed to fetch alarm events by status');
    }
    return response.json();
  },

  // Get alarm events for a specific alarm
  getAlarmEventsForAlarm: async (alarmId: string): Promise<AlarmEvent[]> => {
    const response = await fetch(`${API_EVENTS_URL}/alarm/${alarmId}`);
    if (!response.ok) {
      throw new Error('Failed to fetch alarm events for alarm');
    }
    return response.json();
  },

  // Acknowledge an alarm event
  acknowledgeAlarm: async (eventId: string): Promise<AlarmEvent> => {
    const response = await fetch(`${API_EVENTS_URL}/${eventId}/acknowledge`, {
      method: 'PUT',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to acknowledge alarm');
    }
    return response.json();
  },

  // Resolve an alarm event
  resolveAlarm: async (eventId: string): Promise<AlarmEvent> => {
    const response = await fetch(`${API_EVENTS_URL}/${eventId}/resolve`, {
      method: 'PUT',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to resolve alarm');
    }
    return response.json();
  },
};
