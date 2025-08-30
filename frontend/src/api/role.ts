import { Permission } from './permission';
import { authHeader } from '@/api/http';
import { apiUrl } from '@/config';

export interface Role {
  id: string;
  name: string;
  description: string;
  permissions: Permission[];
  createdAt: number;
  updatedAt: number;
}

export interface RoleRequest {
  name: string;
  description: string;
  permissionIds: string[];
}

const API_BASE_URL = apiUrl('/api/roles');

// Role API functions
export const roleApi = {
  // Get all roles
  getAllRoles: async (): Promise<Role[]> => {
    const response = await fetch(API_BASE_URL, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch roles');
    }
    return response.json();
  },

  // Get a specific role by ID
  getRoleById: async (id: string): Promise<Role> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch role');
    }
    return response.json();
  },

  // Create a new role
  createRole: async (role: RoleRequest): Promise<Role> => {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(),
      },
      body: JSON.stringify(role),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create role');
    }
    return response.json();
  },

  // Update a role
  updateRole: async (id: string, role: Partial<RoleRequest>): Promise<Role> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(),
      },
      body: JSON.stringify(role),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update role');
    }
    return response.json();
  },

  // Delete a role
  deleteRole: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
      headers: { ...authHeader() },
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to delete role');
    }
  },

  // Get all permissions
  getAllPermissions: async (): Promise<Permission[]> => {
    const response = await fetch(`${API_BASE_URL}/permissions`, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch permissions');
    }
    return response.json();
  },

  // Get permissions by category
  getPermissionsByCategory: async (category: string): Promise<Permission[]> => {
    const response = await fetch(`${API_BASE_URL}/permissions/category/${category}`, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch permissions by category');
    }
    return response.json();
  },

  // Get all permission categories
  getAllPermissionCategories: async (): Promise<string[]> => {
    const response = await fetch(`${API_BASE_URL}/permissions/categories`, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch permission categories');
    }
    return response.json();
  },
};