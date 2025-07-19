import { AlarmServiceClient } from '../generated/AlarmServiceServiceClientPb';
import {
  Alarm,
  CreateAlarmRequest,
  GetAlarmsRequest,
  GetAlarmsResponse,
  AlarmIdRequest,
  UpdateAlarmRequest,
  DeleteAlarmResponse,
  TestAlarmRequest,
  TestAlarmResponse,
  NotificationChannel
} from '../generated/alarmservice_pb';

/**
 * Service for interacting with alarm operations via gRPC-Web.
 */
class AlarmService {
  private client: AlarmServiceClient;

  constructor() {
    // Create a client instance using the gRPC-Web client
    // The URL should match the gRPC server endpoint with the gRPC-Web proxy
    this.client = new AlarmServiceClient('http://localhost:9090');
  }

  /**
   * Create a new alarm.
   * 
   * @param name Alarm name
   * @param description Alarm description
   * @param query Search query for the alarm
   * @param condition Condition (e.g., '>', '<', '=')
   * @param threshold Threshold value
   * @param timeWindowMinutes Time window in minutes
   * @param notificationChannels Notification channels
   * @param enabled Whether the alarm is enabled
   * @returns Promise with the created alarm
   */
  async createAlarm(
    name: string,
    description: string,
    query: string,
    condition: string,
    threshold: number,
    timeWindowMinutes: number,
    notificationChannels: NotificationChannel[] = [],
    enabled: boolean = true
  ): Promise<Alarm> {
    const request = new CreateAlarmRequest();
    request.setName(name);
    request.setDescription(description);
    request.setQuery(query);
    request.setCondition(condition);
    request.setThreshold(threshold);
    request.setTimeWindowMinutes(timeWindowMinutes);
    request.setNotificationChannelsList(notificationChannels);
    request.setEnabled(enabled);
    
    return new Promise((resolve, reject) => {
      this.client.createAlarm(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Get all alarms with pagination.
   * 
   * @param page Page number (0-based)
   * @param size Page size
   * @param sortField Field to sort by
   * @param sortAscending Whether to sort in ascending order
   * @returns Promise with alarms response
   */
  async getAlarms(
    page: number = 0,
    size: number = 20,
    sortField: string = 'createdAt',
    sortAscending: boolean = false
  ): Promise<GetAlarmsResponse> {
    const request = new GetAlarmsRequest();
    request.setPage(page);
    request.setSize(size);
    request.setSortField(sortField);
    request.setSortAscending(sortAscending);
    
    return new Promise((resolve, reject) => {
      this.client.getAlarms(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Get an alarm by ID.
   * 
   * @param id The ID of the alarm
   * @returns Promise with the alarm
   */
  async getAlarmById(id: string): Promise<Alarm> {
    const request = new AlarmIdRequest();
    request.setId(id);
    
    return new Promise((resolve, reject) => {
      this.client.getAlarmById(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Update an existing alarm.
   * 
   * @param id Alarm ID
   * @param name Alarm name
   * @param description Alarm description
   * @param query Search query for the alarm
   * @param condition Condition (e.g., '>', '<', '=')
   * @param threshold Threshold value
   * @param timeWindowMinutes Time window in minutes
   * @param notificationChannels Notification channels
   * @param enabled Whether the alarm is enabled
   * @returns Promise with the updated alarm
   */
  async updateAlarm(
    id: string,
    name: string,
    description: string,
    query: string,
    condition: string,
    threshold: number,
    timeWindowMinutes: number,
    notificationChannels: NotificationChannel[] = [],
    enabled: boolean = true
  ): Promise<Alarm> {
    const request = new UpdateAlarmRequest();
    request.setId(id);
    request.setName(name);
    request.setDescription(description);
    request.setQuery(query);
    request.setCondition(condition);
    request.setThreshold(threshold);
    request.setTimeWindowMinutes(timeWindowMinutes);
    request.setNotificationChannelsList(notificationChannels);
    request.setEnabled(enabled);
    
    return new Promise((resolve, reject) => {
      this.client.updateAlarm(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Delete an alarm by ID.
   * 
   * @param id The ID of the alarm to delete
   * @returns Promise with delete response
   */
  async deleteAlarm(id: string): Promise<DeleteAlarmResponse> {
    const request = new AlarmIdRequest();
    request.setId(id);
    
    return new Promise((resolve, reject) => {
      this.client.deleteAlarm(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Test an alarm against current data.
   * 
   * @param id Alarm ID (optional)
   * @param query Search query (if ID not provided)
   * @param condition Condition (if ID not provided)
   * @param threshold Threshold (if ID not provided)
   * @param timeWindowMinutes Time window (if ID not provided)
   * @returns Promise with test results
   */
  async testAlarm(
    id?: string,
    query?: string,
    condition?: string,
    threshold?: number,
    timeWindowMinutes?: number
  ): Promise<TestAlarmResponse> {
    const request = new TestAlarmRequest();
    
    if (id) {
      request.setId(id);
    } else {
      if (query) request.setQuery(query);
      if (condition) request.setCondition(condition);
      if (threshold !== undefined) request.setThreshold(threshold);
      if (timeWindowMinutes !== undefined) request.setTimeWindowMinutes(timeWindowMinutes);
    }
    
    return new Promise((resolve, reject) => {
      this.client.testAlarm(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }
}

// Export a singleton instance
export const alarmService = new AlarmService();
export default AlarmService;