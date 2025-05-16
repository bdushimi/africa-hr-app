import { CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useEffect, useState } from "react";
import { StyledCard, StyledCardHeader, StyledCardContent } from "@/components/ui/styled-card";
import {
  ClipboardList,
  Loader2,
  ChevronDown,
  ChevronUp,
  Filter,
  CheckCircle2,
  XCircle,
  Clock,
  Check
} from "lucide-react";
import { LeaveRequest, fetchLeaveRequests, fetchAllLeaveRequests } from "@/services/leaveRequestService";
import LeaveRequestDetailDialog from "../leave/LeaveRequestDetailDialog";
import { toast } from "@/components/ui/sonner";
import { useNavigate } from "react-router-dom";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";

type LeaveStatus = "APPROVED" | "PENDING" | "REJECTED" | "CANCELLED" | "ALL";

const RecentLeaveRequests = () => {
  const [requests, setRequests] = useState<LeaveRequest[]>([]);
  const [allRequests, setAllRequests] = useState<LeaveRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingAll, setLoadingAll] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedRequest, setSelectedRequest] = useState<LeaveRequest | null>(null);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [showAllRequests, setShowAllRequests] = useState(false);
  const [selectedStatus, setSelectedStatus] = useState<LeaveStatus>("ALL");
  const navigate = useNavigate();

  const loadRecentRequests = async () => {
    try {
      setLoading(true);
      const data = await fetchLeaveRequests(0, 5);
      setRequests(data.leaveRequests);
      setAllRequests(data.leaveRequests); // Store original list for filtering
      setError(null);
    } catch (err) {
      setError("Failed to fetch leave requests");
      console.error("Error fetching leave requests:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRecentRequests();
  }, []);

  // Apply status filter
  useEffect(() => {
    if (selectedStatus === "ALL") {
      setRequests(allRequests);
    } else {
      setRequests(allRequests.filter(request => request.status === selectedStatus));
    }
  }, [selectedStatus, allRequests]);

  const handleViewAll = async () => {
    if (showAllRequests) {
      // If already showing all, revert to showing only recent requests
      setShowAllRequests(false);
      await loadRecentRequests();
      return;
    }

    try {
      setLoadingAll(true);
      const data = await fetchAllLeaveRequests(selectedStatus === "ALL" ? undefined : selectedStatus);
      setAllRequests(data.leaveRequests);
      setRequests(data.leaveRequests);
      setShowAllRequests(true);
    } catch (err) {
      toast.error("Failed to load all leave requests");
      console.error("Error fetching all leave requests:", err);
    } finally {
      setLoadingAll(false);
    }
  };

  const handleStatusChange = (status: LeaveStatus) => {
    setSelectedStatus(status);

    // If showing all requests, refetch with the new status filter
    if (showAllRequests) {
      fetchAllLeaveRequests(status === "ALL" ? undefined : status)
        .then(data => {
          setAllRequests(data.leaveRequests);
          setRequests(data.leaveRequests);
        })
        .catch(err => {
          console.error("Error fetching filtered leave requests:", err);
        });
    }
  };

  const handleRowClick = (request: LeaveRequest) => {
    setSelectedRequest(request);
    setIsDetailDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setIsDetailDialogOpen(false);
    setSelectedRequest(null);
  };

  // Function to determine badge color based on status
  const getBadgeColor = (status: string) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-100 text-green-800";
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      case "REJECTED":
        return "bg-red-100 text-red-800";
      case "CANCELLED":
        return "bg-gray-100 text-gray-600";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  const getStatusIcon = (status: LeaveStatus) => {
    switch (status) {
      case "APPROVED":
        return <CheckCircle2 className="mr-2 h-4 w-4 text-green-600" />;
      case "PENDING":
        return <Clock className="mr-2 h-4 w-4 text-yellow-600" />;
      case "REJECTED":
        return <XCircle className="mr-2 h-4 w-4 text-red-600" />;
      case "CANCELLED":
        return <XCircle className="mr-2 h-4 w-4 text-gray-600" />;
      case "ALL":
        return <Filter className="mr-2 h-4 w-4 text-blue-600" />;
      default:
        return null;
    }
  };

  const getStatusBadgeStyle = (status: LeaveStatus) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-100 text-green-800 border-green-200 hover:bg-green-200";
      case "PENDING":
        return "bg-yellow-100 text-yellow-800 border-yellow-200 hover:bg-yellow-200";
      case "REJECTED":
        return "bg-red-100 text-red-800 border-red-200 hover:bg-red-200";
      case "CANCELLED":
        return "bg-gray-100 text-gray-600 border-gray-200 hover:bg-gray-200";
      case "ALL":
        return "bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-200";
      default:
        return "bg-gray-100 text-gray-800 border-gray-200";
    }
  };

  if (loading) {
    return (
      <StyledCard variant="accent">
        <StyledCardHeader>
          <div className="flex items-center space-x-2">
            <ClipboardList className="h-5 w-5 text-teal-500" />
            <CardTitle className="text-lg font-medium">Recent Leave Requests</CardTitle>
          </div>
        </StyledCardHeader>
        <StyledCardContent>
          <div className="flex justify-center items-center h-32">
            <Loader2 className="h-8 w-8 animate-spin text-teal-500" />
            <p className="ml-2">Loading requests...</p>
          </div>
        </StyledCardContent>
      </StyledCard>
    );
  }

  if (error) {
    return (
      <StyledCard variant="accent">
        <StyledCardHeader>
          <div className="flex items-center space-x-2">
            <ClipboardList className="h-5 w-5 text-teal-500" />
            <CardTitle className="text-lg font-medium">Recent Leave Requests</CardTitle>
          </div>
        </StyledCardHeader>
        <StyledCardContent>
          <div className="flex justify-center items-center h-32">
            <p className="text-red-500">{error}</p>
          </div>
        </StyledCardContent>
      </StyledCard>
    );
  }

  return (
    <>
      <StyledCard variant="accent">
        <StyledCardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between space-y-3 sm:space-y-0">
            <div className="flex items-center space-x-2">
              <ClipboardList className="h-5 w-5 text-teal-500" />
              <div>
                <CardTitle className="text-lg font-medium">Recent Leave Requests</CardTitle>
                <p className="text-sm text-muted-foreground">
                  {showAllRequests
                    ? `Showing ${selectedStatus === "ALL" ? "all" : selectedStatus.toLowerCase()} leave requests (${requests.length})`
                    : `Your 5 most recent ${selectedStatus === "ALL" ? "" : selectedStatus.toLowerCase() + " "}leave requests`}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    className={`h-8 border flex items-center ${getStatusBadgeStyle(selectedStatus)}`}
                  >
                    {getStatusIcon(selectedStatus)}
                    <span>{selectedStatus === "ALL" ? "Filter by Status" : selectedStatus}</span>
                    <ChevronDown className="ml-2 h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-[200px]">
                  <DropdownMenuLabel>Filter by Status</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuCheckboxItem
                    checked={selectedStatus === "ALL"}
                    onCheckedChange={() => handleStatusChange("ALL")}
                  >
                    <div className="flex items-center">
                      <Filter className="mr-2 h-4 w-4 text-blue-600" />
                      All Statuses
                    </div>
                  </DropdownMenuCheckboxItem>
                  <DropdownMenuCheckboxItem
                    checked={selectedStatus === "APPROVED"}
                    onCheckedChange={() => handleStatusChange("APPROVED")}
                  >
                    <div className="flex items-center">
                      <CheckCircle2 className="mr-2 h-4 w-4 text-green-600" />
                      Approved
                    </div>
                  </DropdownMenuCheckboxItem>
                  <DropdownMenuCheckboxItem
                    checked={selectedStatus === "PENDING"}
                    onCheckedChange={() => handleStatusChange("PENDING")}
                  >
                    <div className="flex items-center">
                      <Clock className="mr-2 h-4 w-4 text-yellow-600" />
                      Pending
                    </div>
                  </DropdownMenuCheckboxItem>
                  <DropdownMenuCheckboxItem
                    checked={selectedStatus === "REJECTED"}
                    onCheckedChange={() => handleStatusChange("REJECTED")}
                  >
                    <div className="flex items-center">
                      <XCircle className="mr-2 h-4 w-4 text-red-600" />
                      Rejected
                    </div>
                  </DropdownMenuCheckboxItem>
                  <DropdownMenuCheckboxItem
                    checked={selectedStatus === "CANCELLED"}
                    onCheckedChange={() => handleStatusChange("CANCELLED")}
                  >
                    <div className="flex items-center">
                      <XCircle className="mr-2 h-4 w-4 text-gray-600" />
                      Cancelled
                    </div>
                  </DropdownMenuCheckboxItem>
                </DropdownMenuContent>
              </DropdownMenu>

              <Button
                variant="outline"
                size="sm"
                className="h-8 border-teal-200 text-teal-700 hover:bg-teal-50"
                onClick={handleViewAll}
                disabled={loadingAll}
              >
                {loadingAll ? (
                  <>
                    <Loader2 className="mr-2 h-3 w-3 animate-spin" />
                    Loading...
                  </>
                ) : showAllRequests ? (
                  <>
                    Show Recent
                    <ChevronUp className="ml-1 h-3 w-3" />
                  </>
                ) : (
                  <>
                    View All
                    <ChevronDown className="ml-1 h-3 w-3" />
                  </>
                )}
              </Button>
            </div>
          </div>
        </StyledCardHeader>
        <StyledCardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left border-b border-teal-100">
                  <th className="py-3 px-2 font-medium text-teal-800">Type</th>
                  <th className="py-3 px-2 font-medium text-teal-800">Start Date</th>
                  <th className="py-3 px-2 font-medium text-teal-800">End Date</th>
                  <th className="py-3 px-2 font-medium text-teal-800">Days</th>
                  <th className="py-3 px-2 font-medium text-teal-800">Status</th>
                </tr>
              </thead>
              <tbody>
                {requests.length > 0 ? (
                  requests.map((request) => (
                    <tr
                      key={request.id}
                      className="border-b border-teal-50 last:border-0 hover:bg-teal-100/40 cursor-pointer transition-colors duration-150 group"
                      onClick={() => handleRowClick(request)}
                    >
                      <td className="py-3 px-2 font-medium group-hover:text-teal-700">{request.type}</td>
                      <td className="py-3 px-2 group-hover:text-teal-700">{new Date(request.startDate).toLocaleDateString()}</td>
                      <td className="py-3 px-2 group-hover:text-teal-700">{new Date(request.endDate).toLocaleDateString()}</td>
                      <td className="py-3 px-2 group-hover:text-teal-700">{request.days}</td>
                      <td className="py-3 px-2">
                        <span className={`px-2 py-1 rounded-full text-xs ${getBadgeColor(request.status)}`}>
                          {request.status}
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-gray-500">
                      {selectedStatus === "ALL"
                        ? "No leave requests found"
                        : `No ${selectedStatus.toLowerCase()} leave requests found`}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </StyledCardContent>
      </StyledCard>

      {/* Detail Dialog */}
      <LeaveRequestDetailDialog
        isOpen={isDetailDialogOpen}
        onClose={handleCloseDialog}
        leaveRequest={selectedRequest}
        onLeaveRequestUpdated={showAllRequests ? handleViewAll : loadRecentRequests}
      />
    </>
  );
};

export default RecentLeaveRequests;
