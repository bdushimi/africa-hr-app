import { Outlet } from "react-router-dom";
import Header from "./Header.copy";
import Sidebar from "./Sidebar"
import { Toaster } from "@/components/ui/toaster";

export default function DashboardLayout() {
    return (
        <div className="flex h-screen overflow-hidden bg-gray-50">
            <Sidebar />
            <div className="flex flex-col flex-1 overflow-x-hidden overflow-y-auto">
                <Header />
                <main className="flex-1 p-6">
                    <Outlet />
                </main>
                <Toaster />
            </div>
        </div>
    );
}
