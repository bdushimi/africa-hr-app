import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import { Toaster } from '@/components/ui/sonner';
import { Button } from '@/components/ui/button';
import { Menu } from 'lucide-react';

const DashboardLayout = () => {
    const [isMobileOpen, setIsMobileOpen] = useState(false);

    return (
        <div className="flex min-h-screen bg-gray-50">
            {/* Mobile menu button */}
            <div className="fixed left-4 top-4 z-40 lg:hidden">
                <Button
                    variant="ghost"
                    size="icon"
                    className="rounded-lg bg-slate-900 text-white hover:bg-slate-800"
                    onClick={() => setIsMobileOpen(true)}
                >
                    <Menu className="h-6 w-6" />
                </Button>
            </div>

            {/* Sidebar */}
            <Sidebar
                isMobileOpen={isMobileOpen}
                onMobileClose={() => setIsMobileOpen(false)}
            />

            {/* Main content */}
            <div className="flex-1 lg:ml-64">
                <Header />
                <main>
                    <div className="container mx-auto px-4 py-8 sm:px-6 lg:px-8">
                        <Outlet />
                    </div>
                </main>
            </div>

            <Toaster />
        </div>
    );
};

export default DashboardLayout; 