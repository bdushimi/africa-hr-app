import { Suspense } from 'react';
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { NotificationProvider } from "@/contexts/NotificationContext";
import ProtectedRoute from "@/components/ProtectedRoute";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { routes, publicRoutes } from "@/config/routes";
import { Loader } from "lucide-react";
import { ThemeProvider } from '@/contexts/ThemeContext';

const queryClient = new QueryClient();

const App = () => (
  <ThemeProvider>
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <BrowserRouter>
          <AuthProvider>
            <NotificationProvider>
              <Routes>
                {/* Public Routes */}
                {publicRoutes.map(({ path, element: Element }) => (
                  <Route
                    key={path}
                    path={path}
                    element={
                      <Suspense
                        fallback={
                          <div className="min-h-screen flex flex-col items-center justify-center">
                            <Loader className="h-12 w-12 text-primary animate-spin mb-4" />
                            <p className="text-lg text-muted-foreground">Loading page...</p>
                          </div>
                        }
                      >
                        <Element />
                      </Suspense>
                    }
                  />
                ))}

                {/* Protected Routes */}
                <Route element={<ProtectedRoute />}>
                  <Route element={<DashboardLayout />}>
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                    {routes.map(({ path, element: Element }) => (
                      <Route
                        key={path}
                        path={path}
                        element={
                          <Suspense
                            fallback={
                              <div className="min-h-screen flex flex-col items-center justify-center">
                                <Loader className="h-12 w-12 text-primary animate-spin mb-4" />
                                <p className="text-lg text-muted-foreground">Loading page...</p>
                              </div>
                            }
                          >
                            <Element />
                          </Suspense>
                        }
                      />
                    ))}
                  </Route>
                </Route>

                {/* 404 Route */}
                <Route
                  path="*"
                  element={
                    <Suspense
                      fallback={
                        <div className="min-h-screen flex flex-col items-center justify-center">
                          <Loader className="h-12 w-12 text-primary animate-spin mb-4" />
                          <p className="text-lg text-muted-foreground">Loading page...</p>
                        </div>
                      }
                    >
                      <div className="min-h-screen flex flex-col items-center justify-center">
                        <h1 className="text-4xl font-bold text-gray-800 mb-4">404</h1>
                        <p className="text-lg text-muted-foreground">Page not found</p>
                      </div>
                    </Suspense>
                  }
                />
              </Routes>
            </NotificationProvider>
          </AuthProvider>
        </BrowserRouter>
      </TooltipProvider>
    </QueryClientProvider>
  </ThemeProvider>
);

export default App;
