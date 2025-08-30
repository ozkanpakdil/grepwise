// Profile API

export interface ProfileResponse {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  createdAt: number;
  updatedAt: number;
}

export interface ProfileUpdateRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  currentPassword?: string;
  newPassword?: string;
}

import { authHeader } from '@/api/http';
import { apiUrl } from '@/config';
const API_BASE_URL = apiUrl('/api/profile');

// Profile API functions
export const profileApi = {
  // Get current user's profile
  getCurrentProfile: async (): Promise<ProfileResponse> => {
    const response = await fetch(API_BASE_URL, {
      headers: {
        ...authHeader(),
      },
    });
    if (!response.ok) {
      let message = 'Failed to fetch profile';
      try {
        const err = await response.json();
        message = err?.error || message;
      } catch {
        try { message = await response.text(); } catch {}
      }
      throw new Error(message);
    }
    try {
      return await response.json();
    } catch {
      throw new Error('Failed to parse profile response');
    }
  },

  // Update current user's profile
  updateProfile: async (profileData: ProfileUpdateRequest): Promise<ProfileResponse> => {
    const response = await fetch(API_BASE_URL, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(),
      },
      body: JSON.stringify(profileData),
    });
    if (!response.ok) {
      let message = 'Failed to update profile';
      try {
        const err = await response.json();
        message = err?.error || message;
      } catch {
        try { message = await response.text(); } catch {}
      }
      throw new Error(message);
    }
    try {
      return await response.json();
    } catch {
      throw new Error('Failed to parse profile response');
    }
  },

  // Change password
  changePassword: async (currentPassword: string, newPassword: string): Promise<ProfileResponse> => {
    return profileApi.updateProfile({
      currentPassword,
      newPassword,
    });
  },
};