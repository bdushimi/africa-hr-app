import { useState } from "react";
import { CardTitle } from "@/components/ui/card";
import LeaveRequestDialog from "@/components/leave/LeaveRequestDialog";
import { toast } from "@/components/ui/sonner";
import { StyledCard, StyledCardHeader, StyledCardContent } from "@/components/ui/styled-card";
import { CustomProgress } from "@/components/ui/custom-progress";
import { Clock } from "lucide-react";

type LeaveType = {
  type: string;
  used: number;
  total: number;
  daysLeft: number;
};

type LeaveBalanceCardProps = {
  leaveTypes: LeaveType[];
  loading?: boolean;
};

const LeaveBalanceCard = ({ leaveTypes, loading = false }: LeaveBalanceCardProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const handleOpenRequestDialog = () => {
    setIsDialogOpen(true);
  };

  // Function to determine progress color based on percentage used
  const getProgressColor = (percentUsed: number) => {
    if (percentUsed >= 80) return "bg-red-500";
    if (percentUsed >= 60) return "bg-amber-500";
    return "bg-blue-500";
  };

  if (loading) {
    return (
      <StyledCard variant="info">
        <StyledCardHeader>
          <div className="flex items-center space-x-2">
            <Clock className="h-5 w-5 text-sky-500" />
            <CardTitle className="text-lg font-medium">Leave Balance Details</CardTitle>
          </div>
          <p className="text-sm text-muted-foreground">Breakdown by leave type</p>
        </StyledCardHeader>
        <StyledCardContent>
          <div className="flex justify-center items-center h-32">
            <p>Loading leave balances...</p>
          </div>
        </StyledCardContent>
      </StyledCard>
    );
  }

  return (
    <StyledCard variant="info">
      <StyledCardHeader>
        <div className="flex items-center space-x-2">
          <Clock className="h-5 w-5 text-sky-500" />
          <CardTitle className="text-lg font-medium">Leave Balance Details</CardTitle>
        </div>
        <p className="text-sm text-muted-foreground">Breakdown by leave type</p>
      </StyledCardHeader>
      <StyledCardContent>
        {leaveTypes.map((leave, index) => {
          const percentUsed = Math.round((leave.used / leave.total) * 100);

          return (
            <div key={index} className="space-y-2">
              <div className="flex justify-between items-center">
                <div className="font-medium">{leave.type}</div>
                <div className="text-sm text-muted-foreground">
                  <span>{leave.daysLeft} days left</span>
                </div>
              </div>
              <div className="flex justify-between items-center text-sm">
                <span>Used: {leave.used} of {leave.total}</span>
                <span className={percentUsed >= 80 ? "text-red-600 font-medium" : ""}>
                  {percentUsed}%
                </span>
              </div>
              <CustomProgress
                value={percentUsed}
                className="h-2"
                indicatorClassName={getProgressColor(percentUsed)}
              />
            </div>
          );
        })}
        <button
          className="w-full bg-sky-500 text-white hover:bg-sky-600 py-2 px-4 rounded-md text-sm font-medium flex items-center justify-center mt-4 transition-colors"
          onClick={handleOpenRequestDialog}
        >
          <span className="mr-1">+</span> Request Leave
        </button>
      </StyledCardContent>

      <LeaveRequestDialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
      />
    </StyledCard>
  );
};

export default LeaveBalanceCard;
