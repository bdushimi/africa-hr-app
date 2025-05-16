import { useAuth } from '@/contexts/AuthContext';
import { Bell, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/button';
import NotificationsDropdown from '../notifications/NotificationsDropdown';

const Header = () => {
    const { logout } = useAuth();

    return (
        <header className="sticky top-0 z-30 border-b bg-white">
            <div className="flex h-16 items-center justify-between px-4 sm:px-6 lg:px-8">
                {/* Left side - Page title */}
                <h1 className="text-lg font-semibold text-gray-900"></h1>

                {/* Right side - Actions */}
                <div className="flex items-center space-x-4">
                    {/* Notifications */}
                    <NotificationsDropdown />

                    {/* Logout button */}
                    <Button
                        variant="ghost"
                        size="icon"
                        className="text-gray-600 hover:text-red-600"
                        onClick={logout}
                    >
                        <LogOut className="h-5 w-5" />
                        <span className="sr-only">Logout</span>
                    </Button>
                </div>
            </div>
        </header>
    );
};

export default Header; 