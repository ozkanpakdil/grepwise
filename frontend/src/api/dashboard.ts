export interface Dashboard {
  id: string;
  name: string;
  description?: string;
  createdBy: string;
  createdAt: number;
  updatedAt: number;
  isShared: boolean;
  widgets: DashboardWidget[];
}

export interface DashboardWidget {
  id: string;
  dashboardId: string;
  title: string;
  type: string;
  query: string;
  configuration?: Record<string, any>;
  positionX: number;
  positionY: number;
  width: number;
  height: number;
  createdAt: number;
  updatedAt: number;
}

export interface DashboardRequest {
  name: string;
  description?: string;
  createdBy: string;
  isShared?: boolean;
}

export interface WidgetRequest {
  title: string;
  type: string;
  query: string;
  configuration?: Record<string, any>;
  positionX: number;
  positionY: number;
  width: number;
  height: number;
  userId: string;
}

export interface WidgetData {
  resultType: string;
  logEntries: any[];
  statistics: Record<string, any>;
}

const API_BASE_URL = 'http://localhost:8080/api/dashboards';

// Dashboard API functions
export const dashboardApi = {
  // Get all dashboards for a user
  getDashboards: async (userId: string): Promise<Dashboard[]> => {
    const response = await fetch(`${API_BASE_URL}?userId=${userId}`);
    if (!response.ok) {
      throw new Error('Failed to fetch dashboards');
    }
    return response.json();
  },

  // Get a specific dashboard by ID
  getDashboard: async (id: string, userId: string): Promise<Dashboard> => {
    const response = await fetch(`${API_BASE_URL}/${id}?userId=${userId}`);
    if (!response.ok) {
      throw new Error('Failed to fetch dashboard');
    }
    return response.json();
  },

  // Create a new dashboard
  createDashboard: async (dashboard: DashboardRequest): Promise<Dashboard> => {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(dashboard),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create dashboard');
    }
    return response.json();
  },

  // Update a dashboard
  updateDashboard: async (id: string, dashboard: DashboardRequest): Promise<Dashboard> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(dashboard),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update dashboard');
    }
    return response.json();
  },

  // Delete a dashboard
  deleteDashboard: async (id: string, userId: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${id}?userId=${userId}`, {
      method: 'DELETE',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to delete dashboard');
    }
  },

  // Share/unshare a dashboard
  shareDashboard: async (id: string, isShared: boolean, userId: string): Promise<Dashboard> => {
    const response = await fetch(`${API_BASE_URL}/${id}/share`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ isShared, userId }),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to share dashboard');
    }
    return response.json();
  },

  // Add a widget to a dashboard
  addWidget: async (dashboardId: string, widget: WidgetRequest): Promise<DashboardWidget> => {
    const response = await fetch(`${API_BASE_URL}/${dashboardId}/widgets`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(widget),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to add widget');
    }
    return response.json();
  },

  // Update a widget
  updateWidget: async (dashboardId: string, widgetId: string, widget: WidgetRequest): Promise<DashboardWidget> => {
    const response = await fetch(`${API_BASE_URL}/${dashboardId}/widgets/${widgetId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(widget),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update widget');
    }
    return response.json();
  },

  // Delete a widget
  deleteWidget: async (dashboardId: string, widgetId: string, userId: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${dashboardId}/widgets/${widgetId}?userId=${userId}`, {
      method: 'DELETE',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to delete widget');
    }
  },

  // Update widget positions
  updateWidgetPositions: async (dashboardId: string, widgetPositions: Record<string, Record<string, number>>, userId: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${dashboardId}/widgets/positions`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ widgetPositions, userId }),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update widget positions');
    }
  },

  // Get widget data
  getWidgetData: async (dashboardId: string, widgetId: string, userId: string): Promise<WidgetData> => {
    const response = await fetch(`${API_BASE_URL}/${dashboardId}/widgets/${widgetId}/data?userId=${userId}`);
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to fetch widget data');
    }
    return response.json();
  },

  // Get dashboard statistics
  getStatistics: async (): Promise<Record<string, any>> => {
    const response = await fetch(`${API_BASE_URL}/statistics`);
    if (!response.ok) {
      throw new Error('Failed to fetch dashboard statistics');
    }
    return response.json();
  },
};