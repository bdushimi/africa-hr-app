export type UserRole = 'ROLE_ADMIN' | 'ROLE_MANAGER' | 'ROLE_STAFF';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: UserRole[];
} 