import api from './api';

export interface LeaveRequest {
  id: string;
  type: string;
  startDate: string;
  endDate: string;
  days: number;
  status: "APPROVED" | "PENDING" | "REJECTED" | "CANCELLED";
  employeeId: string;
  employeeName: string;
  reason: string;
  requireReason: boolean;
  requireDocument: boolean;
  createdAt: string;
  updatedAt: string;
  approvedBy?: string;
  approverName?: string;
  approvedAt?: string;
  comments?: string;
  document?: string;
  documents?: Array<{
    name?: string;
    blobUrl: string;
    visible?: boolean;
    uploadedAt?: string;
  }>;
}

export interface PaginatedResponse {
  leaveRequests: LeaveRequest[];
  pagination: {
    total: number;
    page: number;
    pageSize: number;
    totalPages: number;
  };
}

/**
 * Fetches recent leave requests with pagination
 * @param page Page number (0-indexed)
 * @param size Number of items per page
 * @param status Optional status filter
 * @returns Promise with paginated leave requests
 */
export const fetchLeaveRequests = async (page = 0, size = 5, status?: string): Promise<PaginatedResponse> => {
  try {
    const params: Record<string, any> = { page, size };
    if (status) {
      params.status = status;
    }
    
    const response = await api.get<PaginatedResponse>('/leaveRequests', {
      params
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching leave requests:', error);
    throw error;
  }
};

/**
 * Fetches all leave requests (with large page size)
 * @param status Optional status filter
 * @returns Promise with leave requests
 */
export const fetchAllLeaveRequests = async (status?: string): Promise<PaginatedResponse> => {
  return fetchLeaveRequests(0, 100, status);
};

/**
 * Fetches a single leave request by ID
 * @param id Leave request ID
 * @returns Promise with leave request details
 */
export const fetchLeaveRequestById = async (id: string): Promise<LeaveRequest> => {
  try {
    const response = await api.get<LeaveRequest>(`/leaveRequests/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching leave request with ID ${id}:`, error);
    throw error;
  }
};

/**
 * Cancels a leave request
 * @param id Leave request ID
 * @returns Promise with the result of cancellation
 */
export const cancelLeaveRequest = async (id: string): Promise<any> => {
  try {
    const response = await api.patch(`/leaveRequests/${id}/cancel`);
    return response.data;
  } catch (error) {
    throw error;
  }
}; 