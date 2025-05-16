import { useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { routes } from '@/config/routes';
import { hasRequiredRoles } from '@/config/routes';
import { cn } from '@/lib/utils';
import {
    LayoutDashboard,
    Calendar,
    FileText,
    Settings,
    LogOut,
    Menu,
    X,
} from 'lucide-react';
import { Button } from '@/components/ui/button';

// Map of icon names to their components
const iconMap = {
    LayoutDashboard,
    Calendar,
    FileText,
    Settings,
};

interface SidebarProps {
    isMobileOpen: boolean;
    onMobileClose: () => void;
}

const Sidebar = ({ isMobileOpen, onMobileClose }: SidebarProps) => {
    const { user, logout } = useAuth();
    const location = useLocation();

    // Filter routes based on user roles
    const accessibleRoutes = useMemo(() => {
        if (!user) return [];
        return routes.filter(route => {
            if (!route.requiredRoles) return true;
            return hasRequiredRoles(user.roles, route.requiredRoles);
        });
    }, [user]);

    const handleLogout = async () => {
        await logout();
    };

    return (
        <>
            {/* Mobile backdrop */}
            {isMobileOpen && (
                <div
                    className="fixed inset-0 z-40 bg-black/50 lg:hidden"
                    onClick={onMobileClose}
                />
            )}

            {/* Sidebar */}
            <div
                className={cn(
                    'fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r bg-slate-900 text-slate-200 transition-transform duration-300 ease-in-out lg:translate-x-0',
                    isMobileOpen ? 'translate-x-0' : '-translate-x-full'
                )}
            >
                {/* Logo/Brand */}
                <div className="flex h-16 items-center justify-between border-b border-slate-800 px-6">
                    <h1 className="text-xl font-semibold text-white">Leave Portal</h1>
                    <button
                        className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-white lg:hidden"
                        onClick={onMobileClose}
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>

                {/* Navigation */}
                <nav className="flex-1 space-y-1 px-3 py-4">
                    {accessibleRoutes.map((route) => {
                        const Icon = iconMap[route.icon as keyof typeof iconMap];
                        const isActive = location.pathname === route.path;

                        return (
                            <Link
                                key={route.path}
                                to={route.path}
                                className={cn(
                                    'flex items-center rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                                    isActive
                                        ? 'bg-slate-800 text-white'
                                        : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                                )}
                                onClick={onMobileClose}
                            >
                                {Icon && <Icon className="mr-3 h-5 w-5" />}
                                {route.title}
                            </Link>
                        );
                    })}
                </nav>

                {/* User Profile & Logout */}
                <div className="border-t border-slate-800 p-4">
                    <div className="mb-4 flex items-center space-x-3">
                        <div className="h-8 w-8 rounded-full bg-slate-800 flex items-center justify-center">
                            <span className="text-sm font-medium text-white">
                                {user?.firstName?.[0]}
                                {user?.lastName?.[0]}
                            </span>
                        </div>
                        <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-white truncate">
                                {user?.firstName} {user?.lastName}
                            </p>
                            <p className="text-xs text-slate-400 truncate">{user?.email}</p>
                        </div>
                    </div>
                    <Button
                        variant="ghost"
                        className="w-full justify-start text-slate-400 hover:text-red-400 hover:bg-slate-800"
                        onClick={handleLogout}
                    >
                        <LogOut className="mr-3 h-5 w-5" />
                        Logout
                    </Button>
                </div>
            </div>
        </>
    );
};

export default Sidebar; 