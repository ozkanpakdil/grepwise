import { User } from '@/store/auth-store';

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

const API_BASE_URL = 'http://localhost:8080/api/profile';

// Profile API functions
export const profileApi = {
  // Get current user's profile
  getCurrentProfile: async (): Promise<ProfileResponse> => {
    const response = await fetch(API_BASE_URL, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('grepwise-auth-accessToken')}`,
      },
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to fetch profile');
    }
    return response.json();
  },

  // Update current user's profile
  updateProfile: async (profileData: ProfileUpdateRequest): Promise<ProfileResponse> => {
    const response = await fetch(API_BASE_URL, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('grepwise-auth-accessToken')}`,
      },
      body: JSON.stringify(profileData),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update profile');
    }
    return response.json();
  },

  // Change password
  changePassword: async (currentPassword: string, newPassword: string): Promise<ProfileResponse> => {
    return profileApi.updateProfile({
      currentPassword,
      newPassword,
    });
  },
};