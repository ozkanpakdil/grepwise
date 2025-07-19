import { LogServiceClient } from '../generated/LogServiceServiceClientPb';
import {
  LogEntry,
  SearchRequest,
  SearchResponse,
  LogIdRequest,
  DeleteRequest,
  DeleteResponse
} from '../generated/logservice_pb';

/**
 * Service for interacting with log operations via gRPC-Web.
 */
class LogService {
  private client: LogServiceClient;

  constructor() {
    // Create a client instance using the gRPC-Web client
    // The URL should match the gRPC server endpoint with the gRPC-Web proxy
    this.client = new LogServiceClient('http://localhost:9090');
  }

  /**
   * Search logs with a query and time range.
   * 
   * @param query The search query
   * @param isRegex Whether the query is a regex pattern
   * @param startTime Start time in milliseconds
   * @param endTime End time in milliseconds
   * @param page Page number (0-based)
   * @param size Page size
   * @returns Promise with search results
   */
  async searchLogs(
    query?: string,
    isRegex: boolean = false,
    startTime?: number,
    endTime?: number,
    page: number = 0,
    size: number = 20
  ): Promise<SearchResponse> {
    const request = new SearchRequest();
    
    if (query) {
      request.setQuery(query);
    }
    
    if (startTime) {
      request.setStartTime(startTime);
    }
    
    if (endTime) {
      request.setEndTime(endTime);
    }
    
    request.setPage(page);
    request.setSize(size);
    request.setSortField('timestamp');
    request.setSortAscending(false);
    
    return new Promise((resolve, reject) => {
      this.client.searchLogs(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Get a log entry by ID.
   * 
   * @param id The ID of the log entry
   * @returns Promise with the log entry
   */
  async getLogById(id: string): Promise<LogEntry> {
    const request = new LogIdRequest();
    request.setId(id);
    
    return new Promise((resolve, reject) => {
      this.client.getLogById(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Delete logs matching a query and time range.
   * 
   * @param query The search query
   * @param startTime Start time in milliseconds
   * @param endTime End time in milliseconds
   * @returns Promise with delete results
   */
  async deleteLogs(
    query?: string,
    startTime?: number,
    endTime?: number
  ): Promise<DeleteResponse> {
    const request = new DeleteRequest();
    
    if (query) {
      request.setQuery(query);
    }
    
    if (startTime) {
      request.setStartTime(startTime);
    }
    
    if (endTime) {
      request.setEndTime(endTime);
    }
    
    return new Promise((resolve, reject) => {
      this.client.deleteLogs(request, null, (err, response) => {
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
export const logService = new LogService();
export default LogService;