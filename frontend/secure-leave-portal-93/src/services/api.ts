import axios from 'axios';
import { toast } from '@/components/ui/sonner';

// Create axios instance with default config
const api = axios.create({
  baseURL: '/api', // Use relative URL with Vite proxy
  headers: {
    'Content-Type': 'application/json',
  },
});

// Keep track of the current URL to avoid redirect loops
const isLoginPage = () => {
  return window.location.pathname.includes('/login');
};

// Request interceptor for adding auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for handling common errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Check if this is a login attempt (URL contains /auth/login)
      const isLoginAttempt = error.config.url?.includes('/auth/login');

      // Handle specific error cases
      switch (error.response.status) {
        case 400:
          // Handle business logic errors (e.g., insufficient leave balance)
          if (error.response.data && error.response.data.error) {
            // Optionally show toast here, or let the caller handle it
            toast.error(error.response.data.error);
            // Attach error message to the error object for the caller
            error.businessError = error.response.data.error;
          }
          break;
        case 401:
          // Don't redirect if already on login page or if this is a login attempt
          if (!isLoginPage() && !isLoginAttempt) {
            // Clear auth data and redirect to login
            localStorage.removeItem('auth_token');
            localStorage.removeItem('user_data');
            sessionStorage.removeItem('auth_token');
            sessionStorage.removeItem('user_data');
            
            // Use History API instead of location.href to prevent full page reload
            window.history.pushState({}, '', '/login');
            // Dispatch an event so React Router can detect the change
            window.dispatchEvent(new Event('popstate'));
          }
          break;
        case 403:
          // Handle forbidden access
          console.error('Access forbidden');
          break;
        case 429:
          // Handle rate limiting
          console.error('Too many requests');
          break;
        default:
          console.error('API Error:', error.response.data);
      }
    }
    return Promise.reject(error);
  }
);

export default api; 