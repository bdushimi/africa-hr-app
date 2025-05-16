import { useEffect, Suspense } from 'react';
import { useNavigate, Outlet, useLocation } from 'react-router-dom';
import { Loader } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/contexts/AuthContext';
import { hasRequiredRoles } from '@/config/routes';
import { UserRole } from '@/types/auth';

const ProtectedRoute = () => {
  const { user, isLoading, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        navigate('/login', { replace: true });
        return;
      }

      // Check if user has required roles for the current path
      const currentPath = location.pathname;
      const requiredRoles = getRequiredRolesForPath(currentPath);

      if (requiredRoles && !hasRequiredRoles(user?.roles || [], requiredRoles)) {
        toast.error("You don't have permission to access this page.");
        navigate('/dashboard', { replace: true });
      }
    }
  }, [isLoading, isAuthenticated, user?.roles, location.pathname, navigate]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center">
        <Loader className="h-12 w-12 text-primary animate-spin mb-4" />
        <p className="text-lg text-muted-foreground">Loading...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex flex-col items-center justify-center">
          <Loader className="h-12 w-12 text-primary animate-spin mb-4" />
          <p className="text-lg text-muted-foreground">Loading page...</p>
        </div>
      }
    >
      <Outlet />
    </Suspense>
  );
};

// Helper function to get required roles for a specific path
const getRequiredRolesForPath = (path: string): UserRole[] | undefined => {
  if (path.includes('/leave-settings')) {
    return ['ROLE_ADMIN'];
  }
  if (path.includes('/leave-requests')) {
    return ['ROLE_ADMIN', 'ROLE_MANAGER'];
  }
  return undefined; // No specific role requirements for other paths
};

export default ProtectedRoute;
