import api from './api';

export interface PublicHoliday {
  id: number;
  name: string;
  date: string;
  description: string;
}

export interface EmployeeLeave {
  employeeId: string;
  employeeName: string;
  departmentName: string;
  startDate: string;
  endDate: string;
  leaveType: string;
  status: string;
  days: number;
}

export interface Department {
  id: string;
  name: string;
  description: string;
}

export interface CalendarData {
  employeeLeaves: EmployeeLeave[];
  publicHolidays: PublicHoliday[];
  departments: Department[];
}

/**
 * Fetches company calendar data including employee leaves and public holidays
 * @returns Promise with calendar data
 */
export const fetchCompanyCalendarData = async (): Promise<CalendarData> => {
  try {
    const response = await api.get('/leaveRequests/company-calendar');
    return response.data;
  } catch (error) {
    console.error('Error fetching calendar data:', error);
    throw error;
  }
};

/**
 * Extracts departments from the API response data
 */
export const extractDepartments = (departments: Department[]): string[] => {
  return ["All Departments", ...departments.map(dept => dept.name)];
};

/**
 * Gets all leave data for a specific date
 * @param date The date to check
 * @param leaves Array of employee leaves
 * @param filters Active filters
 * @returns Filtered leave data for the specified date
 */
export const getLeaveForDate = (
  date: Date, 
  leaves: EmployeeLeave[],
  filters: {
    department: string;
    leaveType: string;
    member: string;
  }
): EmployeeLeave[] => {
  const dateStr = date.toISOString().split('T')[0]; // Format: YYYY-MM-DD
  
  return leaves.filter((leave) => {
    return (
      (filters.department === "All Departments" ||
        leave.departmentName === filters.department) &&
      (filters.member === "All Members" || leave.employeeName === filters.member) &&
      (filters.leaveType === "All Types" || leave.leaveType.includes(filters.leaveType.split(' ')[0])) &&
      leave.startDate <= dateStr &&
      leave.endDate >= dateStr
    );
  });
};

/**
 * Checks if a date is a public holiday
 * @param date The date to check
 * @param holidays Array of public holidays
 * @returns The holiday if it exists, undefined otherwise
 */
export const isPublicHoliday = (date: Date, holidays: PublicHoliday[]): PublicHoliday | undefined => {
  const dateStr = date.toISOString().split('T')[0]; // Format: YYYY-MM-DD
  return holidays.find((holiday) => holiday.date === dateStr);
};

/**
 * Finds a department by its name
 * @param name Department name
 * @param departments Array of departments
 * @returns The department or undefined if not found
 */
export const getDepartmentByName = (name: string, departments: Department[]): Department | undefined => {
  return departments.find(dept => dept.name === name);
}; 