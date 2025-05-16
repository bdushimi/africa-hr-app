import { useState, useEffect } from 'react';
import api from '@/services/api';

// Types for the leave statistics
export type LeaveStats = {
  totalLeaveBalance: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  cancelledRequests: number;
};

export type LeaveType = {
  type: string;
  used: number;
  total: number;
  daysLeft: number;
};

// Type for the leave requests stats API response
type LeaveStatsResponse = {
  PENDING: number;
  APPROVED: number;
  REJECTED: number;
  CANCELLED: number;
};

export const useLeaveStats = () => {
  const [leaveStats, setLeaveStats] = useState<LeaveStats>({
    totalLeaveBalance: 0,
    pendingRequests: 0,
    approvedRequests: 0,
    rejectedRequests: 0,
    cancelledRequests: 0
  });
  const [leaveTypes, setLeaveTypes] = useState<LeaveType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLeaveStats = async () => {
    try {
      setLoading(true);
      
      // Fetch leave request statistics
      const statsResponse = await api.get<LeaveStatsResponse>("/leaveRequests/stats");
      
      // Fetch leave balance statistics - now returns the exact format we need
      const balanceResponse = await api.get<LeaveType[]>("/balances/stats");
      
      // Calculate total leave balance by summing up daysLeft from all leave types
      const totalBalance = balanceResponse.data.reduce(
        (sum, leaveType) => sum + leaveType.daysLeft, 
        0
      );
      
      // Map the API responses to our state format
      setLeaveStats({
        totalLeaveBalance: parseFloat(totalBalance.toFixed(2)), // Round to 2 decimal places
        pendingRequests: statsResponse.data.PENDING || 0,
        approvedRequests: statsResponse.data.APPROVED || 0,
        rejectedRequests: statsResponse.data.REJECTED || 0,
        cancelledRequests: statsResponse.data.CANCELLED || 0
      });
      
      // Set leave types directly from the API response
      setLeaveTypes(balanceResponse.data);
      setError(null);
    } catch (err) {
      setError("Failed to fetch leave statistics");
      console.error("Error fetching leave statistics:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeaveStats();
  }, []);

  return { 
    leaveStats, 
    leaveTypes, 
    loading, 
    error, 
    refetch: fetchLeaveStats 
  };
};

export default useLeaveStats; 