import { useState, FormEvent, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Separator } from "@/components/ui/separator";
import { Loader, AlertCircle } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Navigate, useLocation } from "react-router-dom";

const Login = () => {
  const { login, microsoftLogin, isLoading, isAuthenticated, loginError, clearLoginError } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const isProduction = process.env.NODE_ENV === "production";
  const location = useLocation();

  // Clear validation error when changing input values
  useEffect(() => {
    if (validationError) setValidationError(null);
    if (loginError) clearLoginError();
  }, [email, password, loginError, clearLoginError]);

  // Check for error in redirect state
  useEffect(() => {
    if (location.state?.error) {
      setValidationError(location.state.error);
    }
  }, [location]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault(); // Prevent default form submission behavior

    // Clear previous errors
    setValidationError(null);
    clearLoginError();

    // Form validation
    if (!email) {
      setValidationError("Email is required");
      return;
    }

    if (!password) {
      setValidationError("Password is required");
      return;
    }

    // If validation passes, attempt login
    await login(email, password, rememberMe);
    // No need to catch errors - they are now handled in the AuthContext
  };

  const handleMicrosoftLogin = async (e: React.MouseEvent) => {
    e.preventDefault(); // Prevent default button behavior
    clearLoginError();
    setValidationError(null);
    await microsoftLogin();
    // No need to catch errors - they are now handled in the AuthContext
  };

  // If user is already authenticated, redirect to dashboard
  if (isAuthenticated) {
    return <Navigate to="/dashboard" />;
  }

  // Determine which error to show - validation errors take precedence
  const displayError = validationError || loginError;

  return (
    <div className="min-h-screen flex items-center justify-center bg-blue-50/30 px-4 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md border-teal-100 shadow-lg">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold text-teal-800">Sign In</CardTitle>
          <CardDescription>Access your account to continue</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {/* Microsoft Login Button */}
            <Button
              variant="outline"
              className="w-full flex items-center justify-center gap-2 h-10 border-blue-200 text-blue-700 hover:bg-blue-50"
              onClick={handleMicrosoftLogin}
              disabled={isLoading}
              type="button" // Explicitly set type to button to avoid form submission
            >
              {isLoading ? (
                <Loader className="h-4 w-4 animate-spin" />
              ) : (
                <svg width="16" height="16" viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg">
                  <path d="M7.462 0H0v7.391h7.462V0z" fill="#F25022" />
                  <path d="M16 0H8.539v7.391H16V0z" fill="#7FBA00" />
                  <path d="M7.462 8.609H0V16h7.462V8.609z" fill="#00A4EF" />
                  <path d="M16 8.609H8.539V16H16V8.609z" fill="#FFB900" />
                </svg>
              )}
              <span>Sign in with Microsoft</span>
            </Button>

            {/* Show local login only if not in production */}
            {!isProduction && (
              <>
                <div className="relative flex items-center">
                  <Separator className="flex-1" />
                  <span className="mx-2 text-xs text-gray-500 uppercase">or</span>
                  <Separator className="flex-1" />
                </div>

                <form onSubmit={handleSubmit} noValidate>
                  <div className="space-y-4">
                    {/* Email Field */}
                    <div className="space-y-1">
                      <Label htmlFor="email">Email</Label>
                      <Input
                        id="email"
                        type="email"
                        placeholder="name@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        disabled={isLoading}
                        className={validationError?.includes("Email") ? "border-red-300 focus-visible:ring-red-400" : ""}
                      />
                    </div>

                    {/* Password Field */}
                    <div className="space-y-1">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="password">Password</Label>
                        <a
                          href="#"
                          className="text-sm text-teal-600 hover:text-teal-800"
                          onClick={(e) => e.preventDefault()}
                        >
                          Forgot password?
                        </a>
                      </div>
                      <div className="relative">
                        <Input
                          id="password"
                          type={passwordVisible ? "text" : "password"}
                          placeholder="••••••••"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          required
                          disabled={isLoading}
                          className={`pr-10 ${validationError?.includes("Password") ? "border-red-300 focus-visible:ring-red-400" : ""}`}
                        />
                        <button
                          type="button"
                          className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-500 hover:text-gray-700"
                          onClick={() => setPasswordVisible(!passwordVisible)}
                        >
                          {passwordVisible ? (
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"></path>
                              <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"></path>
                              <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"></path>
                              <line x1="2" x2="22" y1="2" y2="22"></line>
                            </svg>
                          ) : (
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"></path>
                              <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                          )}
                        </button>
                      </div>
                    </div>

                    {/* Remember Me Checkbox */}
                    <div className="flex items-center space-x-2">
                      <Checkbox
                        id="remember"
                        checked={rememberMe}
                        onCheckedChange={(checked) => setRememberMe(checked === true)}
                      />
                      <label
                        htmlFor="remember"
                        className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                      >
                        Remember me
                      </label>
                    </div>

                    {/* Sign In Button */}
                    <Button
                      type="submit"
                      className="w-full bg-teal-600 hover:bg-teal-700 text-white"
                      disabled={isLoading}
                    >
                      {isLoading ? (
                        <Loader className="h-4 w-4 animate-spin mr-2" />
                      ) : null}
                      Sign In
                    </Button>

                    {/* Error message displayed below Sign In button */}
                    {displayError && (
                      <div className="mt-3 p-3 text-center rounded-md bg-red-50 border border-red-200">
                        <p className="text-sm text-red-700 flex items-center justify-center">
                          <AlertCircle className="h-4 w-4 mr-2 flex-shrink-0" />
                          {displayError}
                        </p>
                      </div>
                    )}
                  </div>
                </form>
              </>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Login;
