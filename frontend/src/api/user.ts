import { authHeader } from '@/api/http';
import { apiUrl } from '@/config';

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  roleIds?: string[];
  enabled?: boolean;
}

export interface UserRequest {
  username: string;
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  roleIds: string[];
  enabled?: boolean;
}

const API_BASE_URL = apiUrl('/api/users');

// User API functions
export const userApi = {
  // Get all users
  getAllUsers: async (): Promise<User[]> => {
    const response = await fetch(API_BASE_URL, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch users');
    }
    return response.json();
  },

  // Get a specific user by ID
  getUserById: async (id: string): Promise<User> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch user');
    }
    return response.json();
  },

  // Create a new user
  createUser: async (user: UserRequest): Promise<User> => {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(),
      },
      body: JSON.stringify(user),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create user');
    }
    return response.json();
  },

  // Update a user
  updateUser: async (id: string, user: Partial<UserRequest>): Promise<User> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(),
      },
      body: JSON.stringify(user),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update user');
    }
    return response.json();
  },

  // Delete a user
  deleteUser: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
      headers: { ...authHeader() },
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to delete user');
    }
  },

  // Get users by role
  getUsersByRole: async (role: string): Promise<User[]> => {
    const response = await fetch(`${API_BASE_URL}/role/${role}`, { headers: { ...authHeader() } });
    if (!response.ok) {
      throw new Error('Failed to fetch users by role');
    }
    return response.json();
  },
};