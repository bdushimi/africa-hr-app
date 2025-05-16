import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Calendar, Search, Filter, FileText, Plus, User, Briefcase, Moon, Sun, ClipboardList } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { toast } from "@/components/ui/use-toast";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import { format, parseISO, isValid, addDays } from "date-fns";
import api from '@/services/api';
import { useTheme } from '@/contexts/ThemeContext';
import { StyledCard, StyledCardContent, StyledCardHeader } from "@/components/ui/styled-card";

// Types
type LeaveRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

type Department = {
  id: number;
  name: string;
};

type LeaveRequest = {
  id: number;
  employeeId: number;
  employeeName: string;
  leaveTypeId: number;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  status: LeaveRequestStatus;
  leaveRequestReason?: string;
  rejectionReason?: string;
  documentUrl?: string;
  createdAt: string;
  updatedAt: string;
  approverName?: string;
  halfDayStart: boolean;
  halfDayEnd: boolean;
  department?: Department;
  documents?: { name: string; blobUrl: string }[];
  primaryDocument?: { name: string; blobUrl: string };
};

type Pageable = {
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

type ApiResponse = {
  content: LeaveRequest[];
  pageable: Pageable;
  totalPages: number;
  totalElements: number;
};

// Sample departments
const departments = [
  "All Departments",
  "Sales",
  "Marketing",
  "Engineering",
  "Human Resources",
  "Finance",
  "Operations",
  "Customer Support",
];

// Sample leave types
const leaveTypes = [
  "All Types",
  "Annual Leave",
  "Sick Leave",
  "Personal Leave",
  "Maternity Leave",
  "Bereavement Leave",
  "Unpaid Leave",
  "Work from Home",
];

const LeaveRequests = () => {
  const { theme, toggleTheme } = useTheme();
  const [leaveRequests, setLeaveRequests] = useState<LeaveRequest[]>([]);
  const [filteredRequests, setFilteredRequests] = useState<LeaveRequest[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [departmentFilter, setDepartmentFilter] = useState<string>("All Departments");
  const [typeFilter, setTypeFilter] = useState<string>("All Types");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [selectedRequest, setSelectedRequest] = useState<LeaveRequest | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState<boolean>(false);
  const [isConfirmOpen, setIsConfirmOpen] = useState<boolean>(false);
  const [confirmAction, setConfirmAction] = useState<"approve" | "reject" | null>(null);
  const [rejectReason, setRejectReason] = useState<string>("");
  const [isRejectionReasonOpen, setIsRejectionReasonOpen] = useState<boolean>(false);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loadingDocIdx, setLoadingDocIdx] = useState<number | null>(null);

  // Fetch leave requests from API
  useEffect(() => {
    const fetchLeaveRequests = async () => {
      setIsLoading(true);
      try {
        const response = await api.get(`/leaveRequests/direct-reports?page=${currentPage}&size=100`);
        const data: ApiResponse = response.data;
        setLeaveRequests(data.content);
        setTotalPages(data.totalPages);
        setFilteredRequests(data.content);
      } catch (error) {
        console.error('Error fetching leave requests:', error);
        toast({
          title: "Error",
          description: "Failed to fetch leave requests",
          variant: "destructive"
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchLeaveRequests();
  }, [currentPage]);

  // Filter leave requests
  useEffect(() => {
    setIsLoading(true);

    // Apply filters
    let results = leaveRequests;

    // Status filter
    if (statusFilter !== "all") {
      results = results.filter(request =>
        request.status.toLowerCase() === statusFilter.toLowerCase()
      );
    }

    // Department filter
    if (departmentFilter !== "All Departments") {
      results = results.filter(request =>
        request.department?.name === departmentFilter
      );
    }

    // Type filter
    if (typeFilter !== "All Types") {
      results = results.filter(request =>
        request.leaveTypeName === typeFilter
      );
    }

    // Search filter (employee name or leave type)
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      results = results.filter(request =>
        request.employeeName.toLowerCase().includes(query) ||
        request.leaveTypeName.toLowerCase().includes(query)
      );
    }

    setFilteredRequests(results);
    setIsLoading(false);
  }, [leaveRequests, statusFilter, departmentFilter, typeFilter, searchQuery]);

  // Get badge color based on status
  const getBadgeColor = (status: LeaveRequestStatus) => {
    switch (status) {
      case "PENDING":
        return "bg-yellow-100 text-yellow-800 hover:bg-yellow-200";
      case "APPROVED":
        return "bg-green-100 text-green-800 hover:bg-green-200";
      case "REJECTED":
        return "bg-red-100 text-red-800 hover:bg-red-200";
      default:
        return "bg-gray-100 text-gray-800 hover:bg-gray-200";
    }
  };

  // Get leave type badge color
  const getLeaveTypeBadge = (type: string) => {
    switch (type.toLowerCase()) {
      case "annual":
        return "bg-teal-100 text-teal-800 hover:bg-teal-200";
      case "sick":
        return "bg-red-100 text-red-800 hover:bg-red-200";
      case "maternity":
        return "bg-purple-100 text-purple-800 hover:bg-purple-200";
      case "paternity":
        return "bg-indigo-100 text-indigo-800 hover:bg-indigo-200";
      case "unpaid":
        return "bg-gray-100 text-gray-800 hover:bg-gray-200";
      default:
        return "bg-gray-100 text-gray-800 hover:bg-gray-200";
    }
  };

  // Format date for display - with safety check
  const formatDate = (dateString: string) => {
    try {
      const parsedDate = parseISO(dateString);
      if (!isValid(parsedDate)) {
        return "Invalid date";
      }
      return format(parsedDate, "MMM dd, yyyy");
    } catch (error) {
      console.error("Date formatting error:", error);
      return "Invalid date";
    }
  };

  // Format date and time for display - with safety check
  const formatDateTime = (dateString: string) => {
    try {
      const parsedDate = parseISO(dateString);
      if (!isValid(parsedDate)) {
        return "Invalid date/time";
      }
      return format(parsedDate, "MMM dd, yyyy HH:mm");
    } catch (error) {
      console.error("Date formatting error:", error);
      return "Invalid date/time";
    }
  };

  // Calculate days between dates
  const calculateDays = (startDate: string, endDate: string, halfDayStart: boolean, halfDayEnd: boolean) => {
    const start = parseISO(startDate);
    const end = parseISO(endDate);
    if (!isValid(start) || !isValid(end)) return 0;

    let days = 0;
    let currentDate = start;
    while (currentDate <= end) {
      const dayOfWeek = currentDate.getDay();
      if (dayOfWeek !== 0 && dayOfWeek !== 6) { // Skip weekends (0 = Sunday, 6 = Saturday)
        days += 1.0;
      }
      currentDate = addDays(currentDate, 1);
    }

    // Adjust for half days
    const startDayOfWeek = start.getDay();
    const endDayOfWeek = end.getDay();
    if (halfDayStart && startDayOfWeek !== 0 && startDayOfWeek !== 6) {
      days -= 0.5;
    }
    if (halfDayEnd && endDayOfWeek !== 0 && endDayOfWeek !== 6) {
      days -= 0.5;
    }

    return days;
  };

  // Open leave request details
  const openLeaveDetails = (request: LeaveRequest) => {
    setSelectedRequest(request);
    setIsDetailOpen(true);
  };

  // Handle approve action
  const handleApprove = async (request: LeaveRequest) => {
    setSelectedRequest(request);
    setConfirmAction("approve");
    setIsConfirmOpen(true);
  };

  // Handle reject action
  const handleReject = (request: LeaveRequest) => {
    setSelectedRequest(request);
    setConfirmAction("reject");
    setRejectReason("");
    setIsRejectionReasonOpen(true);
  };

  // Confirm approval
  const confirmApprove = async () => {
    if (!selectedRequest) return;

    setIsLoading(true);
    try {
      const response = await api.put(`/leaveRequests/${selectedRequest.id}/approve`, {
        status: "APPROVED",
        rejectionReason: null
      });

      // Refresh the leave requests
      const updatedResponse = await api.get(`/leaveRequests/direct-reports?page=${currentPage}&size=100`);
      const data: ApiResponse = updatedResponse.data;
      setLeaveRequests(data.content);
      setFilteredRequests(data.content);

      setIsConfirmOpen(false);
      setIsDetailOpen(false);
      toast({
        title: "Leave Request Approved",
        description: `Successfully approved leave request for ${selectedRequest.employeeName}`,
        variant: "default",
        className: "bg-green-50 border-green-200 dark:bg-green-900/20 dark:border-green-800",
      });
    } catch (error) {
      console.error('Error approving leave request:', error);
      toast({
        title: "Error",
        description: "Failed to approve leave request. Please try again.",
        variant: "destructive"
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Confirm rejection
  const confirmReject = async () => {
    if (!selectedRequest) return;

    if (!rejectReason.trim()) {
      toast({
        title: "Error",
        description: "Please provide a reason for rejection",
        variant: "destructive"
      });
      return;
    }

    setIsLoading(true);
    try {
      const response = await api.put(`/leaveRequests/${selectedRequest.id}/approve`, {
        status: "REJECTED",
        rejectionReason: rejectReason
      });

      // Refresh the leave requests
      const updatedResponse = await api.get(`/leaveRequests/direct-reports?page=${currentPage}&size=100`);
      const data: ApiResponse = updatedResponse.data;
      setLeaveRequests(data.content);
      setFilteredRequests(data.content);

      setIsRejectionReasonOpen(false);
      setIsDetailOpen(false);
      setRejectReason("");
      toast({
        title: "Leave Request Rejected",
        description: `Successfully rejected leave request for ${selectedRequest.employeeName}`,
        variant: "default",
        className: "bg-red-50 border-red-200 dark:bg-red-900/20 dark:border-red-800",
      });
    } catch (error) {
      console.error('Error rejecting leave request:', error);
      toast({
        title: "Error",
        description: "Failed to reject leave request. Please try again.",
        variant: "destructive"
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Clear all filters
  const clearFilters = () => {
    setStatusFilter("all");
    setDepartmentFilter("All Departments");
    setTypeFilter("All Types");
    setSearchQuery("");
  };

  // Add handleViewDocument function
  const handleViewDocument = async (blobUrl: string, idx: number) => {
    setLoadingDocIdx(idx);
    try {
      const response = await api.get<{ downloadUrl: string }>(`/documents/download-url`, {
        params: { blobUrl },
      });
      window.open(response.data.downloadUrl, '_blank', 'noopener,noreferrer');
    } catch (error) {
      console.error('Error fetching document:', error);
      toast({
        title: "Error",
        description: "Failed to fetch document download link.",
        variant: "destructive"
      });
    } finally {
      setLoadingDocIdx(null);
    }
  };

  return (
    <div className="min-h-screen bg-blue-50/30">
      <div className="w-full flex justify-center">
        <main className="w-full max-w-[1440px] p-4 sm:p-6 md:px-8">
          <div className="flex flex-col space-y-2">
            <h1 className="text-3xl font-bold tracking-tight text-teal-700">Leave Requests</h1>
            <p className="text-muted-foreground text-lg">
              View and manage your direct reports' leave requests
            </p>
          </div>

          {/* Filters Card */}
          <StyledCard className="mb-6 mt-6">
            <StyledCardContent className="p-6">
              <div className="space-y-4">
                {/* Search and Filters */}
                <div className="flex flex-col lg:flex-row gap-4">
                  <div className="flex-1">
                    <div className="relative">
                      <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                      <Input
                        type="search"
                        placeholder="Search by employee name or leave type..."
                        className="pl-8"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                      />
                    </div>
                  </div>
                  <div className="flex flex-col sm:flex-row gap-2">
                    <Select value={departmentFilter} onValueChange={setDepartmentFilter}>
                      <SelectTrigger className="w-[180px]">
                        <Briefcase className="h-4 w-4 mr-2 text-muted-foreground" />
                        <SelectValue placeholder="Department" />
                      </SelectTrigger>
                      <SelectContent>
                        {departments.map((dept) => (
                          <SelectItem key={dept} value={dept}>
                            {dept}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>

                    <Select value={typeFilter} onValueChange={setTypeFilter}>
                      <SelectTrigger className="w-[180px]">
                        <FileText className="h-4 w-4 mr-2 text-muted-foreground" />
                        <SelectValue placeholder="Leave Type" />
                      </SelectTrigger>
                      <SelectContent>
                        {leaveTypes.map((type) => (
                          <SelectItem key={type} value={type}>
                            {type}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>

                    <Button variant="outline" size="icon" onClick={clearFilters} title="Clear filters" className="border-teal-200 text-teal-700 hover:bg-teal-50">
                      <Filter className="h-4 w-4" />
                    </Button>
                  </div>
                </div>

                <Tabs defaultValue="all" value={statusFilter} onValueChange={setStatusFilter}>
                  <TabsList className="grid grid-cols-4 w-full max-w-md bg-teal-50/50">
                    <TabsTrigger value="all" className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors">All</TabsTrigger>
                    <TabsTrigger value="pending" className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors">Pending</TabsTrigger>
                    <TabsTrigger value="approved" className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors">Approved</TabsTrigger>
                    <TabsTrigger value="rejected" className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors">Rejected</TabsTrigger>
                  </TabsList>

                  <TabsContent value="all" className="mt-4">
                    <LeaveRequestTable
                      requests={filteredRequests}
                      isLoading={isLoading}
                      onViewDetails={openLeaveDetails}
                      onApprove={handleApprove}
                      onReject={handleReject}
                      formatDate={formatDate}
                      getBadgeColor={getBadgeColor}
                      getLeaveTypeBadge={getLeaveTypeBadge}
                      calculateDays={calculateDays}
                    />
                  </TabsContent>

                  <TabsContent value="pending" className="mt-4">
                    <LeaveRequestTable
                      requests={filteredRequests}
                      isLoading={isLoading}
                      onViewDetails={openLeaveDetails}
                      onApprove={handleApprove}
                      onReject={handleReject}
                      formatDate={formatDate}
                      getBadgeColor={getBadgeColor}
                      getLeaveTypeBadge={getLeaveTypeBadge}
                      calculateDays={calculateDays}
                    />
                  </TabsContent>

                  <TabsContent value="approved" className="mt-4">
                    <LeaveRequestTable
                      requests={filteredRequests}
                      isLoading={isLoading}
                      onViewDetails={openLeaveDetails}
                      onApprove={handleApprove}
                      onReject={handleReject}
                      formatDate={formatDate}
                      getBadgeColor={getBadgeColor}
                      getLeaveTypeBadge={getLeaveTypeBadge}
                      calculateDays={calculateDays}
                    />
                  </TabsContent>

                  <TabsContent value="rejected" className="mt-4">
                    <LeaveRequestTable
                      requests={filteredRequests}
                      isLoading={isLoading}
                      onViewDetails={openLeaveDetails}
                      onApprove={handleApprove}
                      onReject={handleReject}
                      formatDate={formatDate}
                      getBadgeColor={getBadgeColor}
                      getLeaveTypeBadge={getLeaveTypeBadge}
                      calculateDays={calculateDays}
                    />
                  </TabsContent>
                </Tabs>
              </div>
            </StyledCardContent>
          </StyledCard>

          {/* Leave Request Details Dialog */}
          <Dialog open={isDetailOpen} onOpenChange={setIsDetailOpen}>
            <DialogContent className="sm:max-w-[600px] bg-white dark:bg-gray-900 border-gray-200 dark:border-gray-800 shadow-xl">
              {selectedRequest && (
                <>
                  <DialogHeader className="space-y-3 pb-4 border-b border-gray-200 dark:border-gray-800">
                    <DialogTitle className="text-xl font-bold text-gray-900 dark:text-gray-100">
                      Leave Request Details
                    </DialogTitle>
                    <DialogDescription className="text-gray-500 dark:text-gray-400">
                      View complete details of the leave request
                    </DialogDescription>
                  </DialogHeader>

                  <div className="space-y-6 py-6">
                    {/* Employee Information */}
                    <div className="flex items-center space-x-4 p-4 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                      <Avatar className="h-14 w-14 border-2 border-gray-200 dark:border-gray-700 shadow-sm">
                        <AvatarFallback className="bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 text-lg">
                          {selectedRequest.employeeName.charAt(0)}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                          {selectedRequest.employeeName}
                        </h3>
                        <div className="text-sm text-gray-600 dark:text-gray-400 font-medium">
                          {selectedRequest.department?.name || 'No Department'}
                        </div>
                      </div>
                    </div>

                    {/* Leave Details */}
                    <div className="grid gap-6 p-4 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Leave Type</p>
                          <Badge className={cn(getLeaveTypeBadge(selectedRequest.leaveTypeName), "text-sm font-medium")}>
                            {selectedRequest.leaveTypeName}
                          </Badge>
                        </div>
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Status</p>
                          <Badge className={cn(getBadgeColor(selectedRequest.status), "text-sm font-medium")}>
                            {selectedRequest.status}
                          </Badge>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Start Date</p>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {formatDate(selectedRequest.startDate)}
                          </p>
                        </div>
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">End Date</p>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {formatDate(selectedRequest.endDate)}
                          </p>
                        </div>
                      </div>

                      <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                        <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Duration</p>
                        <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                          {calculateDays(
                            selectedRequest.startDate,
                            selectedRequest.endDate,
                            selectedRequest.halfDayStart,
                            selectedRequest.halfDayEnd
                          )} days
                        </p>
                      </div>

                      {selectedRequest.leaveRequestReason && (
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Reason</p>
                          <p className="text-sm text-gray-900 dark:text-gray-100">
                            {selectedRequest.leaveRequestReason}
                          </p>
                        </div>
                      )}

                      {selectedRequest.documentUrl && (
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Document</p>
                          <Button
                            variant="outline"
                            size="sm"
                            className="gap-2 bg-white dark:bg-gray-900 hover:bg-gray-50 dark:hover:bg-gray-800 border-gray-200 dark:border-gray-700"
                            onClick={() => handleViewDocument(selectedRequest.documentUrl!, -1)}
                            disabled={loadingDocIdx === -1}
                          >
                            <FileText className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                            {loadingDocIdx === -1 ? 'Loading...' : 'View Document'}
                          </Button>
                        </div>
                      )}

                      {/* Supporting Documents */}
                      {selectedRequest.documents && selectedRequest.documents.length > 0 && (
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Supporting Documents</p>
                          <div className="space-y-2">
                            {selectedRequest.documents.map((doc, index) => (
                              <div key={index} className="flex items-center justify-between p-2 rounded-md bg-gray-50 dark:bg-gray-800">
                                <div className="flex items-center gap-2">
                                  <FileText className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                                  <span className="text-sm text-gray-900 dark:text-gray-100">{doc.name}</span>
                                </div>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300"
                                  onClick={() => handleViewDocument(doc.blobUrl, index)}
                                  disabled={loadingDocIdx === index}
                                >
                                  {loadingDocIdx === index ? 'Loading...' : 'View'}
                                </Button>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Primary Document */}
                      {selectedRequest.primaryDocument && (
                        <div className="p-3 rounded-lg bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Primary Document</p>
                          <div className="flex items-center justify-between p-2 rounded-md bg-gray-50 dark:bg-gray-800">
                            <div className="flex items-center gap-2">
                              <FileText className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                              <span className="text-sm text-gray-900 dark:text-gray-100">{selectedRequest.primaryDocument.name}</span>
                            </div>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300"
                              onClick={() => handleViewDocument(selectedRequest.primaryDocument.blobUrl, -2)}
                              disabled={loadingDocIdx === -2}
                            >
                              {loadingDocIdx === -2 ? 'Loading...' : 'View'}
                            </Button>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Request History */}
                    <div className="p-4 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                      <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">Request History</p>
                      <div className="space-y-3">
                        <div className="flex gap-3">
                          <div className={cn(
                            "rounded-full w-3 h-3 mt-1.5",
                            selectedRequest.status === "APPROVED" ? "bg-green-500" :
                              selectedRequest.status === "REJECTED" ? "bg-red-500" :
                                "bg-yellow-500"
                          )} />
                          <div className="flex-1">
                            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                              {selectedRequest.status === "PENDING" ? "Request submitted" :
                                `${selectedRequest.status} by ${selectedRequest.approverName || 'Unknown'}`}
                            </p>
                            <p className="text-xs text-gray-500 dark:text-gray-400">
                              {formatDateTime(selectedRequest.createdAt)}
                            </p>
                            {selectedRequest.rejectionReason && selectedRequest.status === "REJECTED" && (
                              <p className="text-sm mt-1 text-red-600 dark:text-red-400">
                                {selectedRequest.rejectionReason}
                              </p>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <DialogFooter className="flex-col sm:flex-row gap-2 pt-4 border-t border-gray-200 dark:border-gray-800">
                    {selectedRequest.status === "PENDING" && (
                      <>
                        <Button
                          variant="outline"
                          onClick={() => handleReject(selectedRequest)}
                          className="w-full sm:w-auto border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                        >
                          Reject
                        </Button>
                        <Button
                          onClick={() => handleApprove(selectedRequest)}
                          className="w-full sm:w-auto bg-gray-900 dark:bg-gray-100 text-white dark:text-gray-900 hover:bg-gray-800 dark:hover:bg-gray-200"
                        >
                          Approve
                        </Button>
                      </>
                    )}
                    <Button
                      variant="ghost"
                      onClick={() => setIsDetailOpen(false)}
                      className="w-full sm:w-auto text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800"
                    >
                      Close
                    </Button>
                  </DialogFooter>
                </>
              )}
            </DialogContent>
          </Dialog>

          {/* Approval Confirmation Dialog */}
          <Dialog open={isConfirmOpen} onOpenChange={setIsConfirmOpen}>
            <DialogContent className="sm:max-w-[425px] bg-background text-foreground">
              <DialogHeader>
                <DialogTitle>Approve Leave Request</DialogTitle>
                <DialogDescription className="text-muted-foreground">Are you sure you want to approve this leave request?</DialogDescription>
              </DialogHeader>
              <div className="py-4">
                <p>
                  {selectedRequest?.employeeName}'s {selectedRequest?.leaveTypeName} leave request
                  for {formatDate(selectedRequest?.startDate || "")} to {formatDate(selectedRequest?.endDate || "")}
                </p>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsConfirmOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={confirmApprove} disabled={isLoading}>
                  {isLoading ? "Processing..." : "Confirm Approval"}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          {/* Rejection Reason Dialog */}
          <Dialog open={isRejectionReasonOpen} onOpenChange={setIsRejectionReasonOpen}>
            <DialogContent className="sm:max-w-[425px] bg-background text-foreground">
              <DialogHeader>
                <DialogTitle>Reject Leave Request</DialogTitle>
                <DialogDescription className="text-muted-foreground">Please provide a reason for rejection</DialogDescription>
              </DialogHeader>
              <div className="py-4 space-y-4">
                <p>
                  {selectedRequest?.employeeName}'s {selectedRequest?.leaveTypeName} leave request
                  for {formatDate(selectedRequest?.startDate || "")} to {formatDate(selectedRequest?.endDate || "")}
                </p>
                <div className="grid gap-2">
                  <label htmlFor="rejection-reason" className="text-sm font-medium">
                    Rejection Reason <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    id="rejection-reason"
                    className="min-h-[80px] rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                    placeholder="Enter rejection reason"
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsRejectionReasonOpen(false)}>
                  Cancel
                </Button>
                <Button
                  variant="destructive"
                  onClick={confirmReject}
                  disabled={isLoading || !rejectReason.trim()}
                >
                  {isLoading ? "Processing..." : "Confirm Rejection"}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </main>
      </div>
    </div>
  );
};

// Separate component for the leave request table
type LeaveRequestTableProps = {
  requests: LeaveRequest[];
  isLoading: boolean;
  onViewDetails: (request: LeaveRequest) => void;
  onApprove: (request: LeaveRequest) => void;
  onReject: (request: LeaveRequest) => void;
  formatDate: (date: string) => string;
  getBadgeColor: (status: LeaveRequestStatus) => string;
  getLeaveTypeBadge: (type: string) => string;
  calculateDays: (startDate: string, endDate: string, halfDayStart: boolean, halfDayEnd: boolean) => number;
};

const LeaveRequestTable = ({
  requests,
  isLoading,
  onViewDetails,
  onApprove,
  onReject,
  formatDate,
  getBadgeColor,
  getLeaveTypeBadge,
  calculateDays
}: LeaveRequestTableProps) => {
  const truncateText = (text: string, maxLength: number) => {
    if (text.length <= maxLength) return text;
    return `${text.substring(0, maxLength)}...`;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="flex flex-col items-center">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          <p className="mt-2 text-sm text-muted-foreground">Loading leave requests...</p>
        </div>
      </div>
    );
  }

  if (requests.length === 0) {
    return (
      <div className="text-center py-8 border rounded-md bg-white/50 backdrop-blur-sm">
        <p className="text-muted-foreground">No leave requests found</p>
      </div>
    );
  }

  return (
    <div className="border rounded-lg overflow-hidden bg-white/50 backdrop-blur-sm shadow-sm">
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-gradient-to-r from-teal-50 to-teal-100/50 hover:bg-teal-50/50">
              <TableHead className="font-semibold text-teal-900">Employee</TableHead>
              <TableHead className="font-semibold text-teal-900">Leave Type</TableHead>
              <TableHead className="font-semibold text-teal-900">Dates</TableHead>
              <TableHead className="font-semibold text-teal-900">Reason</TableHead>
              <TableHead className="font-semibold text-teal-900">Status</TableHead>
              <TableHead className="font-semibold text-teal-900 w-[150px]">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {requests.map((request) => (
              <TableRow
                key={request.id}
                className="cursor-pointer hover:bg-teal-50/30 transition-colors border-b border-teal-100/50"
                onClick={() => onViewDetails(request)}
              >
                <TableCell className="font-medium">
                  <div className="flex items-center gap-3">
                    <Avatar className="h-10 w-10 border-2 border-teal-100 shadow-sm">
                      <AvatarFallback className="bg-gradient-to-br from-teal-100 to-teal-200 text-teal-700">
                        {request.employeeName.charAt(0)}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <div className="font-semibold text-teal-900">{request.employeeName}</div>
                      <div className="text-xs text-teal-600 font-medium">
                        {request.department?.name || 'No Department'}
                      </div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>
                  <Badge className={cn(
                    getLeaveTypeBadge(request.leaveTypeName),
                    "font-medium px-3 py-1 shadow-sm"
                  )}>
                    {request.leaveTypeName}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="text-sm font-medium text-teal-900">
                    {formatDate(request.startDate)} - {formatDate(request.endDate)}
                  </div>
                  <div className="text-xs text-teal-600 font-medium">
                    {calculateDays(request.startDate, request.endDate, request.halfDayStart, request.halfDayEnd)} day{calculateDays(request.startDate, request.endDate, request.halfDayStart, request.halfDayEnd) !== 1 ? "s" : ""}
                  </div>
                </TableCell>
                <TableCell>
                  <Popover>
                    <PopoverTrigger className="text-left">
                      <span className="text-sm text-teal-900 font-medium underline decoration-dotted underline-offset-4 hover:text-teal-700">
                        {truncateText(request.leaveRequestReason || "", 20)}
                      </span>
                    </PopoverTrigger>
                    <PopoverContent className="w-[350px] bg-white/95 backdrop-blur-sm shadow-lg border-teal-100">
                      <div className="text-sm font-semibold mb-2 text-teal-900">Leave Reason:</div>
                      <p className="text-sm text-teal-800">{request.leaveRequestReason}</p>
                    </PopoverContent>
                  </Popover>
                  {request.documentUrl && (
                    <div className="mt-1">
                      <span className="text-xs flex items-center text-teal-600 hover:text-teal-800 font-medium">
                        <FileText className="h-3 w-3 mr-1" /> Document attached
                      </span>
                    </div>
                  )}
                </TableCell>
                <TableCell>
                  <Badge className={cn(
                    getBadgeColor(request.status),
                    "font-medium px-3 py-1 shadow-sm"
                  )}>
                    {request.status}
                  </Badge>
                </TableCell>
                <TableCell className="space-x-2 whitespace-nowrap">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-teal-600 hover:text-teal-800 hover:bg-teal-50 font-medium"
                    onClick={(e) => {
                      e.stopPropagation();
                      onViewDetails(request);
                    }}
                  >
                    View
                  </Button>
                  {request.status === "PENDING" && (
                    <>
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-red-500 hover:text-red-600 hover:bg-red-50 font-medium border-red-200"
                        onClick={(e) => {
                          e.stopPropagation();
                          onReject(request);
                        }}
                      >
                        Reject
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-green-500 hover:text-green-600 hover:bg-green-50 font-medium border-green-200"
                        onClick={(e) => {
                          e.stopPropagation();
                          onApprove(request);
                        }}
                      >
                        Approve
                      </Button>
                    </>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

export default LeaveRequests;
