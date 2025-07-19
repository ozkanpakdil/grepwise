import { AuthServiceClient } from '../generated/AuthServiceServiceClientPb';
import {
  RegisterRequest,
  LoginRequest,
  AuthResponse,
  ValidateTokenRequest,
  ValidateTokenResponse,
  RefreshTokenRequest,
  LogoutRequest,
  LogoutResponse,
  GetCurrentUserRequest,
  User,
  UpdateUserRequest,
  ChangePasswordRequest,
  ChangePasswordResponse
} from '../generated/authservice_pb';

/**
 * Service for interacting with authentication operations via gRPC-Web.
 */
class AuthService {
  private client: AuthServiceClient;
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private user: User | null = null;

  constructor() {
    // Create a client instance using the gRPC-Web client
    // The URL should match the gRPC server endpoint with the gRPC-Web proxy
    this.client = new AuthServiceClient('http://localhost:9090');
    
    // Load tokens from localStorage if available
    this.loadTokens();
  }

  /**
   * Load tokens from localStorage.
   */
  private loadTokens(): void {
    try {
      this.accessToken = localStorage.getItem('accessToken');
      this.refreshToken = localStorage.getItem('refreshToken');
    } catch (error) {
      console.error('Error loading tokens from localStorage:', error);
    }
  }

  /**
   * Save tokens to localStorage.
   * 
   * @param accessToken Access token
   * @param refreshToken Refresh token
   */
  private saveTokens(accessToken: string, refreshToken: string): void {
    try {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
    } catch (error) {
      console.error('Error saving tokens to localStorage:', error);
    }
  }

  /**
   * Clear tokens from localStorage.
   */
  private clearTokens(): void {
    try {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      this.accessToken = null;
      this.refreshToken = null;
      this.user = null;
    } catch (error) {
      console.error('Error clearing tokens from localStorage:', error);
    }
  }

  /**
   * Register a new user.
   * 
   * @param username Username
   * @param email Email
   * @param password Password
   * @param firstName First name
   * @param lastName Last name
   * @returns Promise with auth response
   */
  async register(
    username: string,
    email: string,
    password: string,
    firstName: string,
    lastName: string
  ): Promise<AuthResponse> {
    const request = new RegisterRequest();
    request.setUsername(username);
    request.setEmail(email);
    request.setPassword(password);
    request.setFirstName(firstName);
    request.setLastName(lastName);
    
    return new Promise((resolve, reject) => {
      this.client.register(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          if (response.getSuccess()) {
            this.saveTokens(response.getAccessToken(), response.getRefreshToken());
            this.user = response.getUser();
          }
          resolve(response);
        }
      });
    });
  }

  /**
   * Login with username and password.
   * 
   * @param username Username
   * @param password Password
   * @returns Promise with auth response
   */
  async login(username: string, password: string): Promise<AuthResponse> {
    const request = new LoginRequest();
    request.setUsername(username);
    request.setPassword(password);
    
    return new Promise((resolve, reject) => {
      this.client.login(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          if (response.getSuccess()) {
            this.saveTokens(response.getAccessToken(), response.getRefreshToken());
            this.user = response.getUser();
          }
          resolve(response);
        }
      });
    });
  }

  /**
   * Validate a token.
   * 
   * @param token Token to validate
   * @returns Promise with validation response
   */
  async validateToken(token: string = this.accessToken || ''): Promise<ValidateTokenResponse> {
    const request = new ValidateTokenRequest();
    request.setToken(token);
    
    return new Promise((resolve, reject) => {
      this.client.validateToken(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Refresh an access token using a refresh token.
   * 
   * @param refreshToken Refresh token (uses stored token if not provided)
   * @returns Promise with auth response
   */
  async refreshToken(refreshToken: string = this.refreshToken || ''): Promise<AuthResponse> {
    const request = new RefreshTokenRequest();
    request.setRefreshToken(refreshToken);
    
    return new Promise((resolve, reject) => {
      this.client.refreshToken(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          if (response.getSuccess()) {
            this.saveTokens(response.getAccessToken(), response.getRefreshToken());
            this.user = response.getUser();
          }
          resolve(response);
        }
      });
    });
  }

  /**
   * Logout and invalidate tokens.
   * 
   * @returns Promise with logout response
   */
  async logout(): Promise<LogoutResponse> {
    const request = new LogoutRequest();
    if (this.accessToken) {
      request.setAccessToken(this.accessToken);
    }
    if (this.refreshToken) {
      request.setRefreshToken(this.refreshToken);
    }
    
    return new Promise((resolve, reject) => {
      this.client.logout(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          this.clearTokens();
          resolve(response);
        }
      });
    });
  }

  /**
   * Get current user information.
   * 
   * @returns Promise with user information
   */
  async getCurrentUser(): Promise<User> {
    if (!this.accessToken) {
      return Promise.reject(new Error('No access token available'));
    }
    
    const request = new GetCurrentUserRequest();
    request.setAccessToken(this.accessToken);
    
    return new Promise((resolve, reject) => {
      this.client.getCurrentUser(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          this.user = response;
          resolve(response);
        }
      });
    });
  }

  /**
   * Update user information.
   * 
   * @param email Email
   * @param firstName First name
   * @param lastName Last name
   * @returns Promise with updated user
   */
  async updateUser(
    email: string,
    firstName: string,
    lastName: string
  ): Promise<User> {
    if (!this.accessToken) {
      return Promise.reject(new Error('No access token available'));
    }
    
    const request = new UpdateUserRequest();
    request.setAccessToken(this.accessToken);
    request.setEmail(email);
    request.setFirstName(firstName);
    request.setLastName(lastName);
    
    return new Promise((resolve, reject) => {
      this.client.updateUser(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          this.user = response;
          resolve(response);
        }
      });
    });
  }

  /**
   * Change password.
   * 
   * @param currentPassword Current password
   * @param newPassword New password
   * @returns Promise with change password response
   */
  async changePassword(
    currentPassword: string,
    newPassword: string
  ): Promise<ChangePasswordResponse> {
    if (!this.accessToken) {
      return Promise.reject(new Error('No access token available'));
    }
    
    const request = new ChangePasswordRequest();
    request.setAccessToken(this.accessToken);
    request.setCurrentPassword(currentPassword);
    request.setNewPassword(newPassword);
    
    return new Promise((resolve, reject) => {
      this.client.changePassword(request, null, (err, response) => {
        if (err) {
          reject(err);
        } else {
          resolve(response);
        }
      });
    });
  }

  /**
   * Check if the user is authenticated.
   * 
   * @returns True if the user is authenticated
   */
  isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  /**
   * Get the current user.
   * 
   * @returns Current user or null
   */
  getUser(): User | null {
    return this.user;
  }

  /**
   * Get the access token.
   * 
   * @returns Access token or null
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }
}

// Export a singleton instance
export const authService = new AuthService();
export default AuthService;