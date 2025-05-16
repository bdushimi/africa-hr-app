import React, { createContext, useContext, useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "@/components/ui/sonner";
import authService, { AuthResponse } from "@/services/authService";

type User = Omit<AuthResponse, 'token'>;

type AuthContextType = {
  user: User | null;
  isLoading: boolean;
  login: (email: string, password: string, rememberMe: boolean) => Promise<void>;
  microsoftLogin: () => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  loginError: string | null;
  clearLoginError: () => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [loginError, setLoginError] = useState<string | null>(null);
  const navigate = useNavigate();

  const clearLoginError = () => setLoginError(null);

  // Check for existing session on load
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const token = authService.getToken();
        const userData = localStorage.getItem("user_data") || sessionStorage.getItem("user_data");

        if (token && userData && authService.isTokenValid()) {
          try {
            const parsedUser = JSON.parse(userData);
            setUser(parsedUser);
          } catch (error) {
            console.error("Failed to parse user data", error);
            authService.logout();
          }
        } else {
          // Only logout if we're not on the login page
          if (!window.location.pathname.includes('/login')) {
            authService.logout();
          }
        }
      } catch (error) {
        console.error("Auth check failed", error);
        // Only logout if we're not on the login page
        if (!window.location.pathname.includes('/login')) {
          authService.logout();
        }
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  // Local login function
  const login = async (email: string, password: string, rememberMe: boolean) => {
    setIsLoading(true);
    setLoginError(null);

    try {
      const response = await authService.login({ email, password }, rememberMe);
      const { token, ...userData } = response;
      setUser(userData);
      toast.success("Login successful");
      navigate("/dashboard");
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Login failed. Please try again.";
      setLoginError(errorMessage);
      toast.error(errorMessage);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  // Microsoft login function
  const microsoftLogin = async () => {
    setIsLoading(true);
    setLoginError(null);

    try {
      const response = await authService.microsoftLogin();
      const { token, ...userData } = response;
      setUser(userData);
      toast.success("Login successful");
      navigate("/dashboard");
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Microsoft login failed. Please try again.";
      setLoginError(errorMessage);
      toast.error(errorMessage);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  // Logout function
  const logout = async () => {
    try {
      await authService.logout();
      setUser(null);
      navigate("/login");
      toast.success("Logged out successfully");
    } catch (error) {
      console.error("Logout failed", error);
      toast.error("Failed to logout. Please try again.");
    }
  };

  const value = {
    user,
    isLoading,
    login,
    microsoftLogin,
    logout,
    isAuthenticated: !!user,
    loginError,
    clearLoginError
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
