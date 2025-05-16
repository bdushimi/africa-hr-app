import { useEffect } from "react";
import StatCard from "@/components/dashboard/StatCard";
import UserProfileCard from "@/components/dashboard/UserProfileCard";
import LeaveBalanceCard from "@/components/dashboard/LeaveBalanceCard";
import RecentLeaveRequests from "@/components/dashboard/RecentLeaveRequests";
import { toast } from "@/components/ui/sonner";
import { useAuth } from "@/contexts/AuthContext";
import useLeaveStats from "@/hooks/useLeaveStats";
import { StyledCard, StyledCardContent } from "@/components/ui/styled-card";
import { Compass } from "lucide-react";

const Dashboard = () => {
  const { user } = useAuth();
  const { leaveStats, leaveTypes, loading, error } = useLeaveStats();

  const handleRequestLeave = () => {
    toast.info("Leave request feature will be implemented in the next phase!");
  };

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-blue-50/30">
      <div className="w-full flex justify-center">
        <main className="w-full max-w-[1440px] p-4 sm:p-6 md:px-8">
          {/* Welcome Card */}
          <StyledCard variant="primary" className="mb-6">
            <StyledCardContent className="p-6">
              <div className="flex items-start space-x-4">
                <div className="bg-blue-100 p-3 rounded-full">
                  <Compass className="h-6 w-6 text-blue-600" />
                </div>
                <div>
                  <h2 className="text-xl font-bold">Welcome back, {user.firstName} {user.lastName}</h2>
                  <p className="text-gray-500 mt-1">Here's an overview of your leave information</p>
                  {error && (
                    <div className="mt-2 text-sm text-red-500">
                      {error}
                    </div>
                  )}
                </div>
              </div>
            </StyledCardContent>
          </StyledCard>

          {/* Stats Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 sm:gap-6 mb-6">
            <StatCard
              icon={<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2" /></svg>}
              title="Leave Balance"
              value={loading ? "Loading..." : leaveStats.totalLeaveBalance}
              description="Total days remaining"
              formatDecimals={true}
              variant="primary"
            />
            <StatCard
              icon={<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>}
              title="Pending"
              value={loading ? "Loading..." : leaveStats.pendingRequests}
              description="Awaiting approval"
              variant="warning"
            />
            <StatCard
              icon={<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>}
              title="Approved"
              value={loading ? "Loading..." : leaveStats.approvedRequests}
              description="Leave requests approved"
              variant="success"
            />
            <StatCard
              icon={<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17" /></svg>}
              title="Rejected"
              value={loading ? "Loading..." : leaveStats.rejectedRequests}
              description="Leave requests denied"
              variant="destructive"
            />
            <StatCard
              icon={<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" /></svg>}
              title="Cancelled"
              value={loading ? "Loading..." : leaveStats.cancelledRequests}
              description="Leave requests cancelled"
              variant="muted"
            />
          </div>

          {/* User Profile and Leave Balance */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <UserProfileCard />
            <LeaveBalanceCard leaveTypes={leaveTypes} loading={loading} />
          </div>

          {/* Recent Leave Requests */}
          <div className="mb-6">
            <RecentLeaveRequests />
          </div>
        </main>
      </div>
    </div>
  );
};

export default Dashboard;
