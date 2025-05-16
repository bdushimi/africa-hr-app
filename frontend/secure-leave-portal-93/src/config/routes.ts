import { lazy } from 'react';
import { UserRole } from '@/types/auth';

export interface RouteConfig {
  path: string;
  element: React.LazyExoticComponent<React.ComponentType<any>>;
  requiredRoles?: UserRole[];
  title: string;
  icon?: string;
  children?: RouteConfig[];
}

export const routes: RouteConfig[] = [
  {
    path: '/dashboard',
    element: lazy(() => import('@/pages/Dashboard')),
    title: 'Dashboard',
    icon: 'LayoutDashboard',
  },
  {
    path: '/calendar',
    element: lazy(() => import('@/pages/Calendar')),
    title: 'Calendar',
    icon: 'Calendar',
  },
  {
    path: '/leave-requests',
    element: lazy(() => import('@/pages/LeaveRequests')),
    requiredRoles: ['ROLE_ADMIN', 'ROLE_MANAGER'],
    title: 'My Team Time Off',
    icon: 'FileText',
  },
  {
    path: '/leave-settings',
    element: lazy(() => import('@/pages/LeaveSettings')),
    requiredRoles: ['ROLE_ADMIN'],
    title: 'Leave Settings',
    icon: 'Settings',
  },
];

// Public routes that don't require authentication
export const publicRoutes: RouteConfig[] = [
  {
    path: '/login',
    element: lazy(() => import('@/pages/Login')),
    title: 'Login',
  },
];

// Helper function to check if user has required roles
export const hasRequiredRoles = (userRoles: UserRole[], requiredRoles?: UserRole[]): boolean => {
  if (!requiredRoles || requiredRoles.length === 0) return true;
  return requiredRoles.some(role => userRoles.includes(role));
}; 