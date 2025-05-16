import React, { useState, useEffect } from "react";
import {
  addMonths,
  format,
  getDay,
  getDaysInMonth,
  isSameDay,
  startOfMonth,
  subMonths,
} from "date-fns";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ChevronLeft,
  ChevronRight,
  Filter,
  Loader2,
  Info,
  Calendar as CalendarIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { StyledCard, StyledCardHeader, StyledCardContent } from "@/components/ui/styled-card";
import {
  fetchCompanyCalendarData,
  extractDepartments,
  getLeaveForDate,
  isPublicHoliday,
  PublicHoliday,
  EmployeeLeave,
  CalendarData,
  Department
} from "@/services/calendarService";

interface Filters {
  department: string;
}

// Get up to 5 upcoming leaves
const getUpcomingLeaves = (filters: Filters, leaveData: EmployeeLeave[]) => {
  const today = new Date();
  const todayStr = format(today, "yyyy-MM-dd");

  return leaveData
    .filter((leave) => {
      return (
        (filters.department === "All Departments" ||
          leave.departmentName === filters.department) &&
        leave.startDate >= todayStr
      );
    })
    .sort((a, b) => (a.startDate > b.startDate ? 1 : -1))
    .slice(0, 5);
};

interface DayProps {
  date: Date;
  currentMonth: Date;
  today: Date;
  filters: Filters;
  leaveData: EmployeeLeave[];
  holidays: PublicHoliday[];
}

const Day: React.FC<DayProps> = ({
  date,
  currentMonth,
  today,
  filters,
  leaveData,
  holidays
}) => {
  const holiday = isPublicHoliday(date, holidays);
  const leaves = getLeaveForDate(date, leaveData, {
    ...filters,
    leaveType: "All Types",
    member: "All Members"
  });
  const isCurrentMonth = date.getMonth() === currentMonth.getMonth();
  const isToday = isSameDay(date, today);
  const isWeekend = date.getDay() === 0 || date.getDay() === 6;

  // Group leaves by type to count them
  const leaveTypeCounts: Record<string, number> = {};
  leaves.forEach((leave) => {
    const type = leave.leaveType.split(' ')[0]; // Extract base type (Annual, Sick, etc.)
    leaveTypeCounts[type] = (leaveTypeCounts[type] || 0) + 1;
  });

  // Get leave type indicator colors
  const getLeaveTypeColor = (type: string) => {
    switch (type) {
      case "Annual":
        return "bg-blue-500";
      case "Sick":
        return "bg-red-500";
      case "Personal":
        return "bg-purple-500";
      default:
        return "bg-gray-500";
    }
  };

  return (
    <HoverCard openDelay={200} closeDelay={100}>
      <HoverCardTrigger asChild>
        <div
          className={cn(
            "h-24 border border-blue-100 p-1 transition-colors",
            isCurrentMonth
              ? isWeekend ? "bg-blue-50/30" : "bg-white hover:bg-blue-50/20"
              : "bg-gray-50/50 text-gray-400",
            isToday && "ring-2 ring-teal-500 ring-inset"
          )}
        >
          <div className="flex justify-between">
            <span
              className={cn(
                "inline-flex h-6 w-6 items-center justify-center rounded-full text-xs",
                isToday ? "bg-teal-500 text-white" : isWeekend ? "text-blue-700" : "text-gray-500"
              )}
            >
              {date.getDate()}
            </span>
            {holiday && (
              <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200 text-xs">
                Holiday
              </Badge>
            )}
          </div>
          {/* Leave indicators */}
          {Object.keys(leaveTypeCounts).length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1">
              {Object.entries(leaveTypeCounts).map(([type, count]) => (
                <div
                  key={type}
                  className="flex items-center space-x-1"
                >
                  <div
                    className={cn("h-2 w-2 rounded-full", getLeaveTypeColor(type))}
                  />
                  <span className="text-xs text-gray-600">
                    {count} {type.charAt(0)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </HoverCardTrigger>
      <HoverCardContent side="right" className="w-80 p-0 border-teal-200 shadow-lg shadow-teal-100/20">
        <div className="p-4 border-b border-teal-100 bg-teal-50/50">
          <p className="font-semibold text-teal-800">
            {format(date, "EEEE, MMMM d, yyyy")}
          </p>
          {holiday && (
            <div className="mt-2 px-3 py-1 bg-red-50 text-red-700 rounded-md text-sm">
              🎉 {holiday.name}
            </div>
          )}
        </div>
        {leaves.length > 0 ? (
          <div className="p-2 max-h-64 overflow-y-auto">
            <p className="px-2 mb-2 text-teal-600 text-xs uppercase font-semibold tracking-wide">
              Team members on leave
            </p>
            {leaves.map((leave, i) => (
              <div
                key={i}
                className="px-2 py-2 hover:bg-teal-50/50 rounded-md transition-colors"
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium text-sm">{leave.employeeName}</span>
                  <Badge
                    className={cn(
                      leave.leaveType.includes("Annual")
                        ? "bg-blue-100 text-blue-700 hover:bg-blue-100"
                        : leave.leaveType.includes("Sick")
                          ? "bg-red-100 text-red-700 hover:bg-red-100"
                          : "bg-purple-100 text-purple-700 hover:bg-purple-100"
                    )}
                  >
                    {leave.leaveType.split(' ')[0]}
                  </Badge>
                </div>
                <div className="mt-1 text-xs text-gray-500">
                  {leave.startDate === leave.endDate
                    ? format(new Date(leave.startDate), "MMMM d, yyyy")
                    : `${format(new Date(leave.startDate), "MMM d")} - ${format(
                      new Date(leave.endDate),
                      "MMM d, yyyy"
                    )}`}
                </div>
                <div className="mt-1 text-xs text-gray-400">
                  {leave.departmentName} • {leave.status}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-6 text-center text-gray-500 text-sm">
            No leave records for this day
          </div>
        )}
      </HoverCardContent>
    </HoverCard>
  );
};

// Department selector with description tooltips
const DepartmentSelect = ({
  departments,
  value,
  onChange,
  departmentsData
}: {
  departments: string[],
  value: string,
  onChange: (value: string) => void,
  departmentsData: Department[]
}) => {

  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-[250px] border-teal-200 bg-teal-50/30 text-teal-700 focus:ring-teal-500">
        <SelectValue placeholder="Department" />
      </SelectTrigger>
      <SelectContent>
        {departments.map((dept) => (
          <TooltipProvider key={dept}>
            <Tooltip>
              <TooltipTrigger asChild>
                <SelectItem value={dept} className="flex items-center justify-between pr-3">
                  {dept}
                </SelectItem>
              </TooltipTrigger>
            </Tooltip>
          </TooltipProvider>
        ))}
      </SelectContent>
    </Select>
  );
};

const Calendar = () => {
  const today = new Date();
  const [currentMonth, setCurrentMonth] = useState(today);
  const [calendarData, setCalendarData] = useState<CalendarData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [departments, setDepartments] = useState<string[]>([]);
  const [filters, setFilters] = useState<Filters>({
    department: "All Departments",
  });

  useEffect(() => {
    const getCalendarData = async () => {
      setLoading(true);
      try {
        const data = await fetchCompanyCalendarData();
        setCalendarData(data);

        // Extract department options from the API response
        setDepartments(extractDepartments(data.departments));

      } catch (err: any) {
        console.error("Failed to fetch calendar data:", err);

        // Handle specific error types
        if (err.message?.includes('Network Error') || err.message?.includes('CORS')) {
          setError("Cross-origin request blocked. Please make sure the server allows requests from this application.");
        } else if (err.response?.status === 401 || err.message?.includes('Authentication') || err.message?.includes('session')) {
          setError('Your session has expired. Please log in again.');
        } else {
          setError("Failed to load calendar data. Please try again later.");
        }
      } finally {
        setLoading(false);
      }
    };

    getCalendarData();
  }, []);

  // Get days in current month view
  const daysInMonth = () => {
    const start = startOfMonth(currentMonth);
    const totalDays = getDaysInMonth(currentMonth);
    const firstDayOfWeek = getDay(start);

    const days: Date[] = [];

    // Add days from previous month
    for (let i = firstDayOfWeek - 1; i >= 0; i--) {
      const d = new Date(start);
      d.setDate(d.getDate() - i);
      days.push(d);
    }

    // Add days from current month
    for (let i = 1; i <= totalDays; i++) {
      const d = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), i);
      days.push(d);
    }

    // Add days from next month to complete the grid
    const totalCells = Math.ceil(days.length / 7) * 7;
    const daysToAdd = totalCells - days.length;

    for (let i = 1; i <= daysToAdd; i++) {
      const lastDay = new Date(days[days.length - 1]);
      const nextDay = new Date(lastDay);
      nextDay.setDate(lastDay.getDate() + 1);
      days.push(nextDay);
    }

    return days;
  };

  const renderDays = () => {
    if (!calendarData) return null;

    const days = daysInMonth();
    const rows = [];

    for (let i = 0; i < days.length; i += 7) {
      const week = days.slice(i, i + 7);
      rows.push(
        <div className="grid grid-cols-7 gap-px" key={`week-${i}`}>
          {week.map((day, index) => (
            <Day
              key={`day-${index}`}
              date={day}
              currentMonth={currentMonth}
              today={today}
              filters={filters}
              leaveData={calendarData.employeeLeaves}
              holidays={calendarData.publicHolidays}
            />
          ))}
        </div>
      );
    }

    return rows;
  };

  const previousMonth = () => {
    setCurrentMonth(subMonths(currentMonth, 1));
  };

  const nextMonth = () => {
    setCurrentMonth(addMonths(currentMonth, 1));
  };

  const goToToday = () => {
    setCurrentMonth(new Date());
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-blue-50/30 flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <Loader2 className="h-8 w-8 animate-spin text-teal-500" />
          <p className="text-teal-700">Loading calendar data...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-blue-50/30 flex items-center justify-center">
        <div className="bg-white p-6 rounded-lg shadow-sm max-w-md border border-red-200">
          <h2 className="text-xl font-bold text-red-600 mb-2">Error</h2>
          <p className="text-gray-700">{error}</p>
          <div className="flex gap-2 mt-4">
            <Button
              onClick={() => window.location.reload()}
              className="bg-teal-600 hover:bg-teal-700"
            >
              Try Again
            </Button>
            {(error.includes('session') || error.includes('expired') || error.includes('log in')) && (
              <Button
                variant="outline"
                onClick={() => window.location.href = '/login'}
                className="border-teal-200 text-teal-700 hover:bg-teal-50"
              >
                Log In
              </Button>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-blue-50/30">
      <div className="w-full flex justify-center">
        <div className="w-full max-w-[1440px] p-4 sm:p-6 md:px-8">
          <StyledCard variant="accent" className="mb-6">
            <StyledCardHeader className="p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="bg-teal-100 p-2 rounded-full">
                  <CalendarIcon className="h-5 w-5 text-teal-600" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-gray-800">Team Calendar</h2>
                  <p className="text-sm text-muted-foreground">
                    View team leave schedule and public holidays
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="bg-teal-100 text-teal-800 border-teal-200">
                  {format(currentMonth, "MMMM yyyy")}
                </Badge>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={goToToday}
                  className="border-teal-200 text-teal-700 hover:bg-teal-50"
                >
                  Today
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={previousMonth}
                  className="border-teal-200 text-teal-700 hover:bg-teal-50"
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={nextMonth}
                  className="border-teal-200 text-teal-700 hover:bg-teal-50"
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </StyledCardHeader>

            <StyledCardContent className="p-6 pt-0">
              {/* Department Filter */}
              <div className="mb-6 flex items-center">
                <div className="flex items-center">
                  <Filter className="h-4 w-4 mr-1 text-teal-500" />
                  <span className="text-sm text-teal-600 mr-2">Filter by:</span>
                </div>

                {calendarData && (
                  <DepartmentSelect
                    departments={departments}
                    value={filters.department}
                    onChange={(val) => setFilters({ ...filters, department: val })}
                    departmentsData={calendarData.departments}
                  />
                )}
              </div>

              {/* Day Headers */}
              <div className="grid grid-cols-7 gap-px mb-px">
                {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day, index) => (
                  <div
                    key={day}
                    className={cn(
                      "bg-teal-100/50 p-2 text-center text-sm font-semibold",
                      index === 0 || index === 6 ? "text-teal-700" : "text-teal-600"
                    )}
                  >
                    {day}
                  </div>
                ))}
              </div>

              {/* Calendar Grid */}
              <div className="border border-teal-200 rounded-md overflow-hidden mb-6">
                {renderDays()}
              </div>

              {/* Legend */}
              <div className="pt-4 border-t border-teal-100">
                <h3 className="text-sm font-semibold mb-2 text-teal-700">Legend</h3>
                <div className="flex flex-wrap gap-4">
                  <div className="flex items-center space-x-2">
                    <div className="h-3 w-3 rounded-full bg-blue-500" />
                    <span className="text-xs text-gray-600">Annual Leave</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="h-3 w-3 rounded-full bg-red-500" />
                    <span className="text-xs text-gray-600">Sick Leave</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="h-3 w-3 rounded-full bg-purple-500" />
                    <span className="text-xs text-gray-600">Personal Leave</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Badge
                      variant="outline"
                      className="bg-red-50 text-red-700 border-red-200 text-xs h-5"
                    >
                      Holiday
                    </Badge>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="h-5 w-5 rounded-full bg-teal-500 flex items-center justify-center text-white text-xs">
                      {today.getDate()}
                    </div>
                    <span className="text-xs text-gray-600">Today</span>
                  </div>
                </div>
              </div>
            </StyledCardContent>
          </StyledCard>

          {/* Upcoming Leaves */}
          <StyledCard variant="primary" className="mb-6">
            <StyledCardHeader className="pb-2">
              <h2 className="text-lg font-semibold">Upcoming Leave</h2>
            </StyledCardHeader>
            <StyledCardContent>
              <div className="space-y-3">
                {calendarData && getUpcomingLeaves(filters, calendarData.employeeLeaves).length > 0 ? (
                  getUpcomingLeaves(filters, calendarData.employeeLeaves).map((leave, i) => (
                    <div
                      key={i}
                      className="flex justify-between items-center p-3 bg-blue-50/50 rounded-md border border-blue-100"
                    >
                      <div>
                        <div className="font-medium">{leave.employeeName}</div>
                        <div className="text-sm text-gray-500 mt-1">
                          {leave.startDate === leave.endDate
                            ? format(new Date(leave.startDate), "MMMM d, yyyy")
                            : `${format(new Date(leave.startDate), "MMM d")} - ${format(
                              new Date(leave.endDate),
                              "MMM d, yyyy"
                            )}`}
                        </div>
                        <div className="text-xs text-gray-400 mt-0.5">
                          {leave.departmentName}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge
                          className={cn(
                            "px-3 py-1",
                            leave.leaveType.includes("Annual")
                              ? "bg-blue-100 text-blue-700 hover:bg-blue-100"
                              : leave.leaveType.includes("Sick")
                                ? "bg-red-100 text-red-700 hover:bg-red-100"
                                : "bg-purple-100 text-purple-700 hover:bg-purple-100"
                          )}
                        >
                          {leave.leaveType.split(' ')[0]}
                        </Badge>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="text-center py-8 text-gray-500 border-2 border-dashed border-blue-100 rounded-md bg-blue-50/30">
                    No upcoming leave scheduled
                  </div>
                )}
              </div>
            </StyledCardContent>
          </StyledCard>
        </div>
      </div>
    </div>
  );
};

export default Calendar;
