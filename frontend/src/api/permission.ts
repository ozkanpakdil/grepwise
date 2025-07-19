export interface Permission {
  id: string;
  name: string;
  description: string;
  category: string;
  createdAt: number;
  updatedAt: number;
}

// We don't need separate API functions for permissions as they are managed through the role API
// This file just defines the Permission interface for use in other components