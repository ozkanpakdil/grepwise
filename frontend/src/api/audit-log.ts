export interface AuditLog {
  id: string;
  timestamp: number;
  userId: string;
  username: string;
  ipAddress: string;
  category: string;
  action: string;
  status: string;
  description: string;
  targetId: string;
  targetType: string;
  details: Record<string, string>;
}

export interface AuditLogFilter {
  page?: number;
  size?: number;
  userId?: string;
  username?: string;
  category?: string;
  action?: string;
  status?: string;
  targetId?: string;
  targetType?: string;
  startTime?: number;
  endTime?: number;
  searchText?: string;
}

import { apiUrl } from '@/config';

const API_BASE_URL = apiUrl('/api/audit-logs');

// Audit Log API functions
export const auditLogApi = {
  // Get audit logs with optional filtering and pagination
  getAuditLogs: async (filters: AuditLogFilter = {}): Promise<AuditLog[]> => {
    const queryParams = new URLSearchParams();

    // Add all non-undefined filters to query params
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined) {
        queryParams.append(key, value.toString());
      }
    });

    const url = `${API_BASE_URL}?${queryParams.toString()}`;
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error('Failed to fetch audit logs');
    }

    return response.json();
  },

  // Get a specific audit log by ID
  getAuditLogById: async (id: string): Promise<AuditLog> => {
    const response = await fetch(`${API_BASE_URL}/${id}`);

    if (!response.ok) {
      throw new Error('Failed to fetch audit log');
    }

    return response.json();
  },

  // Get distinct categories
  getCategories: async (): Promise<string[]> => {
    const response = await fetch(`${API_BASE_URL}/categories`);

    if (!response.ok) {
      throw new Error('Failed to fetch categories');
    }

    return response.json();
  },

  // Get distinct actions
  getActions: async (): Promise<string[]> => {
    const response = await fetch(`${API_BASE_URL}/actions`);

    if (!response.ok) {
      throw new Error('Failed to fetch actions');
    }

    return response.json();
  },

  // Get distinct target types
  getTargetTypes: async (): Promise<string[]> => {
    const response = await fetch(`${API_BASE_URL}/target-types`);

    if (!response.ok) {
      throw new Error('Failed to fetch target types');
    }

    return response.json();
  },

  // Get audit log counts by category
  getCountsByCategory: async (): Promise<Record<string, number>> => {
    const response = await fetch(`${API_BASE_URL}/counts/by-category`);

    if (!response.ok) {
      throw new Error('Failed to fetch counts by category');
    }

    return response.json();
  },

  // Get audit log counts by action
  getCountsByAction: async (): Promise<Record<string, number>> => {
    const response = await fetch(`${API_BASE_URL}/counts/by-action`);

    if (!response.ok) {
      throw new Error('Failed to fetch counts by action');
    }

    return response.json();
  },

  // Get audit log counts by status
  getCountsByStatus: async (): Promise<Record<string, number>> => {
    const response = await fetch(`${API_BASE_URL}/counts/by-status`);

    if (!response.ok) {
      throw new Error('Failed to fetch counts by status');
    }

    return response.json();
  },

  // Get total count of audit logs
  getCount: async (): Promise<number> => {
    const response = await fetch(`${API_BASE_URL}/count`);

    if (!response.ok) {
      throw new Error('Failed to fetch audit log count');
    }

    return response.json();
  },

  // Delete an audit log by ID (admin only)
  deleteAuditLog: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error('Failed to delete audit log');
    }
  },

  // Delete all audit logs (admin only)
  deleteAllAuditLogs: async (): Promise<void> => {
    const response = await fetch(API_BASE_URL, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error('Failed to delete all audit logs');
    }
  },
};
