import api from './api';
import { jwtDecode } from 'jwt-decode';
import axios from 'axios';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  token: string;
  roles: string[];
}

export interface DecodedToken {
  sub: string;
  email: string;
  iat: number;
  exp: number;
}

class AuthService {
  private static instance: AuthService;

  private constructor() {}

  public static getInstance(): AuthService {
    if (!AuthService.instance) {
      AuthService.instance = new AuthService();
    }
    return AuthService.instance;
  }

  public async login(credentials: LoginCredentials, rememberMe: boolean): Promise<AuthResponse> {
    try {
      const response = await api.post<AuthResponse>('/auth/login', credentials);
      this.handleAuthResponse(response.data, rememberMe);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 401) {
          throw new Error('Invalid email or password');
        }
        if (error.response?.status === 429) {
          throw new Error('Too many login attempts. Please try again later.');
        }
        throw new Error(error.response?.data?.message || 'Login failed. Please try again.');
      }
      throw new Error('An unexpected error occurred during login.');
    }
  }

  public async microsoftLogin(): Promise<AuthResponse> {
    try {
      const response = await api.get<AuthResponse>('/auth/microsoft');
      this.handleAuthResponse(response.data, true);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.message || 'Microsoft login failed. Please try again.');
      }
      throw new Error('An unexpected error occurred during Microsoft login.');
    }
  }

  public async logout(): Promise<void> {
    try {
      const token = this.getToken();
      if (token) {
        // Call the logout API endpoint
        await api.post('/auth/logout');
      }
    } catch (error) {
      console.error('Logout API call failed:', error);
      // Continue with local logout even if API call fails
    } finally {
      // Clear local storage regardless of API call success
      this.clearLocalStorage();
    }
  }

  private clearLocalStorage(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_data');
    sessionStorage.removeItem('auth_token');
    sessionStorage.removeItem('user_data');
  }

  public getToken(): string | null {
    return localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token');
  }

  public isTokenValid(): boolean {
    const token = this.getToken();
    if (!token) return false;

    try {
      const decoded = jwtDecode<DecodedToken>(token);
      return decoded.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private handleAuthResponse(response: AuthResponse, rememberMe: boolean): void {
    const { token, ...userData } = response;
    this.setToken(token, rememberMe);
    this.setUserData(userData, rememberMe);
  }

  private setToken(token: string, rememberMe: boolean = true): void {
    if (rememberMe) {
      localStorage.setItem('auth_token', token);
    } else {
      sessionStorage.setItem('auth_token', token);
    }
  }

  private setUserData(userData: Omit<AuthResponse, 'token'>, rememberMe: boolean = true): void {
    const userDataString = JSON.stringify(userData);
    if (rememberMe) {
      localStorage.setItem('user_data', userDataString);
    } else {
      sessionStorage.setItem('user_data', userDataString);
    }
  }
}

export default AuthService.getInstance(); 