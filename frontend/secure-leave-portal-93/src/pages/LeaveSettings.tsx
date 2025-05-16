import { useState, useEffect } from "react";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
    Pencil,
    Trash,
    Plus,
    Calendar,
    FilePlus,
    AlertCircle,
    Info,
    Eye,
    Loader2
} from "lucide-react";
import { format } from "date-fns";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { Switch } from "@/components/ui/switch";
import api from "@/services/api";

// Types
type LeaveType = {
    id: number;
    name: string;
    description: string;
    isDefault: boolean;
    isEnabled: boolean;
    maxDuration: number | null;
    paid: boolean;
    accrualRate: number | null;
    accrualBased: boolean;
    isCarryForwardEnabled: boolean;
    carryForwardCap: number | null;
    requireReason: boolean;
    requireDocument: boolean;
};

type PublicHoliday = {
    id: string;
    name: string;
    date: string;
    description?: string;
};

type LeaveCarryForward = {
    id: number;
    employeeId: number;
    employeeName: string;
    leaveTypeId: number;
    leaveTypeName: string;
    fromYear: number;
    toYear: number;
    amountCarried: number;
    status: string;
    createdAt: string;
};

type AccrualHistorySummary = {
    id: number;
    accrualPeriod: string;
    processedDate: string;
    employeeCount: number;
    totalAccruals: number;
    totalDaysAccrued: number;
    status: 'COMPLETED' | 'PARTIAL' | 'FAILED';
};

type AccrualHistoryDetail = {
    id: number;
    employeeId: number;
    employeeName: string;
    leaveTypeId: number;
    leaveTypeName: string;
    accrualPeriod: string;
    amount: number;
    isProrated: boolean;
    processedDate: string;
};

// Sample public holidays
const initialPublicHolidays: PublicHoliday[] = [
    {
        id: "ph-1",
        name: "New Year's Day",
        date: "2025-01-01",
        description: "The first day of the year"
    },
    {
        id: "ph-2",
        name: "Martin Luther King Jr. Day",
        date: "2025-01-20"
    },
    {
        id: "ph-3",
        name: "Presidents' Day",
        date: "2025-02-17"
    },
    {
        id: "ph-4",
        name: "Memorial Day",
        date: "2025-05-26"
    },
    {
        id: "ph-5",
        name: "Independence Day",
        date: "2025-07-04",
        description: "Independence Day celebration"
    },
    {
        id: "ph-6",
        name: "Labor Day",
        date: "2025-09-01"
    },
    {
        id: "ph-7",
        name: "Veterans Day",
        date: "2025-11-11"
    },
    {
        id: "ph-8",
        name: "Thanksgiving Day",
        date: "2025-11-27"
    },
    {
        id: "ph-9",
        name: "Christmas Day",
        date: "2025-12-25",
        description: "Christmas holiday"
    }
];

// Add this helper function at the top of the file, after imports
const formatDate = (date: string | null | undefined) => {
    if (!date) return 'N/A';
    try {
        return new Date(date).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (error) {
        console.error('Error formatting date:', error);
        return 'Invalid Date';
    }
};

const LeaveSettings = () => {
    const [leaveTypes, setLeaveTypes] = useState<LeaveType[]>([]);
    const [publicHolidays, setPublicHolidays] = useState<PublicHoliday[]>(initialPublicHolidays);
    const [isLoading, setIsLoading] = useState(false);
    const [isInitialLoading, setIsInitialLoading] = useState(true);
    const [carryForwards, setCarryForwards] = useState<LeaveCarryForward[]>([]);
    const [isAccrualHistoryLoading, setIsAccrualHistoryLoading] = useState(false);
    const [selectedTab, setSelectedTab] = useState("carryForward");

    // Leave type form state
    const [isLeaveTypeDialogOpen, setIsLeaveTypeDialogOpen] = useState(false);
    const [isEditMode, setIsEditMode] = useState(false);
    const [currentLeaveType, setCurrentLeaveType] = useState<LeaveType | null>(null);

    // Holiday form state
    const [isHolidayDialogOpen, setIsHolidayDialogOpen] = useState(false);
    const [isHolidayEditMode, setIsHolidayEditMode] = useState(false);
    const [currentHoliday, setCurrentHoliday] = useState<PublicHoliday | null>(null);

    // Delete confirmation state
    const [deleteItemId, setDeleteItemId] = useState<string | null>(null);
    const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
    const [deleteItemType, setDeleteItemType] = useState<"leaveType" | "holiday" | null>(null);

    // Color options for leave types
    const colorOptions = [
        { name: "Blue", value: "blue", class: "bg-blue-200 text-blue-800" },
        { name: "Red", value: "red", class: "bg-red-200 text-red-800" },
        { name: "Green", value: "green", class: "bg-green-200 text-green-800" },
        { name: "Purple", value: "purple", class: "bg-purple-200 text-purple-800" },
        { name: "Pink", value: "pink", class: "bg-pink-200 text-pink-800" },
        { name: "Yellow", value: "yellow", class: "bg-yellow-200 text-yellow-800" },
        { name: "Gray", value: "gray", class: "bg-gray-200 text-gray-800" },
        { name: "Orange", value: "orange", class: "bg-orange-200 text-orange-800" },
    ];

    // Form for new/edit leave type
    const [leaveTypeForm, setLeaveTypeForm] = useState({
        name: "",
        description: "",
        isDefault: false,
        isEnabled: true,
        maxDuration: 0,
        paid: true,
        accrualRate: null as number | null,
        accrualBased: false,
        isCarryForwardEnabled: false,
        carryForwardCap: null as number | null,
        requireReason: false,
        requireDocument: false
    });

    // Form for new/edit holiday
    const [holidayForm, setHolidayForm] = useState({
        name: "",
        date: "",
        description: ""
    });

    const [accrualHistory, setAccrualHistory] = useState<AccrualHistorySummary[]>([]);
    const [selectedHistoryId, setSelectedHistoryId] = useState<number | null>(null);
    const [accrualDetails, setAccrualDetails] = useState<AccrualHistoryDetail[]>([]);
    const [isDetailsLoading, setIsDetailsLoading] = useState(false);

    // Fetch leave types from the backend
    useEffect(() => {
        const fetchLeaveTypes = async () => {
            try {
                const response = await api.get('/leaveTypes');
                setLeaveTypes(response.data);
            } catch (error) {
                console.error('Error fetching leave types:', error);
                // Error toast is handled by the API interceptor
            } finally {
                setIsInitialLoading(false);
            }
        };

        fetchLeaveTypes();
    }, []);

    // Handle opening the leave type form dialog for edit
    const handleEditLeaveType = (leaveType: LeaveType) => {
        setCurrentLeaveType(leaveType);
        setLeaveTypeForm({
            name: leaveType.name,
            description: leaveType.description,
            isDefault: leaveType.isDefault,
            isEnabled: leaveType.isEnabled,
            maxDuration: leaveType.maxDuration || 0,
            paid: leaveType.paid,
            accrualRate: leaveType.accrualRate,
            accrualBased: leaveType.accrualBased,
            isCarryForwardEnabled: leaveType.isCarryForwardEnabled,
            carryForwardCap: leaveType.carryForwardCap,
            requireReason: leaveType.requireReason,
            requireDocument: leaveType.requireDocument
        });
        setIsEditMode(true);
        setIsLeaveTypeDialogOpen(true);
    };

    // Handle opening the leave type form dialog for create
    const handleAddLeaveType = () => {
        setCurrentLeaveType(null);
        setLeaveTypeForm({
            name: "",
            description: "",
            isDefault: false,
            isEnabled: true,
            maxDuration: 0,
            paid: true,
            accrualRate: null,
            accrualBased: false,
            isCarryForwardEnabled: false,
            carryForwardCap: null,
            requireReason: false,
            requireDocument: false
        });
        setIsEditMode(false);
        setIsLeaveTypeDialogOpen(true);
    };

    // Handle save leave type
    const handleSaveLeaveType = async () => {
        if (!leaveTypeForm.name) {
            toast.error("Leave type name is required");
            return;
        }

        // Validate accrual configuration
        if (leaveTypeForm.accrualBased && (!leaveTypeForm.accrualRate || leaveTypeForm.accrualRate <= 0)) {
            toast.error("Accrual-based leave types must have a positive accrual rate");
            return;
        }

        // Validate carry-forward configuration
        if (leaveTypeForm.isCarryForwardEnabled && (!leaveTypeForm.carryForwardCap || leaveTypeForm.carryForwardCap <= 0)) {
            toast.error("Leave types with carry-forward enabled must have a positive carry-forward cap");
            return;
        }

        // Validate max duration
        if (leaveTypeForm.maxDuration && leaveTypeForm.maxDuration <= 0) {
            toast.error("Maximum duration must be greater than 0");
            return;
        }

        // Validate annual accrual against max duration
        if (leaveTypeForm.maxDuration && leaveTypeForm.accrualBased && leaveTypeForm.accrualRate) {
            const annualAccrual = leaveTypeForm.accrualRate * 12;
            if (annualAccrual > leaveTypeForm.maxDuration) {
                toast.error("Annual accrual cannot exceed maximum duration");
                return;
            }
        }

        setIsLoading(true);

        try {
            // Create the properly formatted data object
            const leaveTypeData = {
                name: leaveTypeForm.name,
                description: leaveTypeForm.description,
                isDefault: leaveTypeForm.isDefault,
                isEnabled: leaveTypeForm.isEnabled,
                maxDuration: leaveTypeForm.maxDuration || null,
                paid: leaveTypeForm.paid,
                accrualBased: leaveTypeForm.accrualBased,
                accrualRate: leaveTypeForm.accrualBased ? leaveTypeForm.accrualRate : null,
                isCarryForwardEnabled: leaveTypeForm.isCarryForwardEnabled,
                carryForwardCap: leaveTypeForm.isCarryForwardEnabled ? leaveTypeForm.carryForwardCap : null,
                requireReason: leaveTypeForm.requireReason,
                requireDocument: leaveTypeForm.requireDocument
            };

            if (isEditMode && currentLeaveType) {
                // Update existing leave type using PATCH
                const response = await api.patch(`/leaveTypes/${currentLeaveType.id}`, leaveTypeData);
                const updatedLeaveTypes = leaveTypes.map(lt =>
                    lt.id === currentLeaveType.id ? response.data : lt
                );
                setLeaveTypes(updatedLeaveTypes);
                toast.success(`Leave type "${leaveTypeForm.name}" updated successfully`);
            } else {
                // Create new leave type
                const response = await api.post('/leaveTypes', leaveTypeData);
                setLeaveTypes([...leaveTypes, response.data]);
                toast.success(`Leave type "${leaveTypeForm.name}" created successfully`);
            }
        } catch (error: any) {
            console.error('Error saving leave type:', error);
            const errorMessage = error.response?.data?.message || "Failed to save leave type";
            toast.error(errorMessage);
        } finally {
            setIsLoading(false);
            setIsLeaveTypeDialogOpen(false);
        }
    };

    // Handle opening the holiday form dialog for edit
    const handleEditHoliday = (holiday: PublicHoliday) => {
        setCurrentHoliday(holiday);
        setHolidayForm({
            name: holiday.name,
            date: holiday.date,
            description: holiday.description || ""
        });
        setIsHolidayEditMode(true);
        setIsHolidayDialogOpen(true);
    };

    // Handle opening the holiday form dialog for create
    const handleAddHoliday = () => {
        setCurrentHoliday(null);
        setHolidayForm({
            name: "",
            date: format(new Date(), "yyyy-MM-dd"),
            description: ""
        });
        setIsHolidayEditMode(false);
        setIsHolidayDialogOpen(true);
    };

    // Handle save holiday
    const handleSaveHoliday = () => {
        if (!holidayForm.name) {
            toast.error("Holiday name is required");
            return;
        }

        if (!holidayForm.date) {
            toast.error("Holiday date is required");
            return;
        }

        setIsLoading(true);

        setTimeout(() => {
            if (isHolidayEditMode && currentHoliday) {
                // Update existing holiday
                const updatedHolidays = publicHolidays.map(h =>
                    h.id === currentHoliday.id ? { ...h, ...holidayForm } : h
                );
                setPublicHolidays(updatedHolidays);
                toast.success(`Holiday "${holidayForm.name}" updated successfully`);
            } else {
                // Create new holiday
                const newHoliday: PublicHoliday = {
                    id: `ph-${Date.now()}`,
                    ...holidayForm
                };
                setPublicHolidays([...publicHolidays, newHoliday]);
                toast.success(`Holiday "${holidayForm.name}" created successfully`);
            }

            setIsLoading(false);
            setIsHolidayDialogOpen(false);
        }, 500);
    };

    // Handle delete confirmation
    const handleDeleteConfirm = async () => {
        if (!deleteItemId || !deleteItemType) return;

        setIsLoading(true);

        try {
            if (deleteItemType === "leaveType") {
                // Call the backend API to delete the leave type
                await api.delete(`/leaveTypes/${deleteItemId}`);

                // Update the local state only after successful API call
                const updatedLeaveTypes = leaveTypes.filter(lt => lt.id.toString() !== deleteItemId);
                setLeaveTypes(updatedLeaveTypes);
                toast.success("Leave type deleted successfully");
            } else {
                // Handle holiday deletion if needed
                const updatedHolidays = publicHolidays.filter(h => h.id !== deleteItemId);
                setPublicHolidays(updatedHolidays);
                toast.success("Holiday deleted successfully");
            }
        } catch (error: any) {
            console.error('Error deleting item:', error);
            const errorMessage = error.response?.data?.message || "Failed to delete item";
            toast.error(errorMessage);
        } finally {
            setIsLoading(false);
            setIsDeleteDialogOpen(false);
            setDeleteItemId(null);
            setDeleteItemType(null);
        }
    };

    // Handle delete button click
    const handleDeleteClick = (id: string, type: "leaveType" | "holiday") => {
        setDeleteItemId(id);
        setDeleteItemType(type);
        setIsDeleteDialogOpen(true);
    };

    const handleProcessCarryForward = async () => {
        try {
            setIsLoading(true);
            // Get all employees first
            const employeesResponse = await api.get('/users/employees');
            const employees = employeesResponse.data;

            // Process carry forward for each employee
            const allCarryForwards: LeaveCarryForward[] = [];
            for (const employee of employees) {
                try {
                    const response = await api.post(`/actions/leave-carry-forwards/employees/${employee.id}`);
                    allCarryForwards.push(...response.data);
                } catch (error) {
                    console.error(`Error processing carry forward for employee ${employee.id}:`, error);
                    // Continue with next employee even if one fails
                }
            }

            setCarryForwards(allCarryForwards);
            toast.success('Leave carry forward processed successfully');
        } catch (error) {
            toast.error(`Failed to process monthly accruals: ${error?.response?.data?.message || 'Unknown error'}`);
        } finally {
            setIsLoading(false);
        }
    };

    const handleProcessMonthlyAccrual = async () => {
        try {
            setIsLoading(true);
            await api.post('/actions/processMonthlyAccruals');
            // Fetch accrual history after processing
            const historyResponse = await api.get('/actions/leaveAccruals/history');
            toast.success('Monthly accruals processed successfully');
        } catch (error) {
            toast.error(`Failed to process monthly accruals: ${error?.response?.data?.message || 'Unknown error'}`);
        } finally {
            setIsLoading(false);
        }
    };

    const handleProcessYearlyAccrual = async () => {
        try {
            setIsLoading(true);
            await api.post('/actions/employees/processYearlyAccruals');
            // Fetch accrual history after processing
            const historyResponse = await api.get('/actions/leave-accruals/history');
            toast.success('Yearly accruals processed successfully');
        } catch (error) {
            console.error('Error processing yearly accruals:', error);
            toast.error('Failed to process yearly accruals');
        } finally {
            setIsLoading(false);
        }
    };

    // Update fetchAccrualHistory to get summary
    const fetchAccrualHistory = async () => {
        try {
            setIsAccrualHistoryLoading(true);
            const response = await api.get('/actions/leaveAccruals/history/summary');
            setAccrualHistory(response.data);
        } catch (error) {
            console.error('Error fetching accrual history:', error);
            toast.error('Failed to fetch accrual history');
        } finally {
            setIsAccrualHistoryLoading(false);
        }
    };

    // Add function to fetch details for a specific history entry
    const fetchAccrualDetails = async (historyId: number) => {
        try {
            setIsDetailsLoading(true);
            const response = await api.get(`/actions/leaveAccruals/history/${historyId}/details`);
            setAccrualDetails(response.data);
            setSelectedHistoryId(historyId);
        } catch (error) {
            console.error('Error fetching accrual details:', error);
            toast.error('Failed to fetch accrual details');
        } finally {
            setIsDetailsLoading(false);
        }
    };

    // Add useEffect to fetch accrual history when accrual tab is selected
    useEffect(() => {
        if (selectedTab === 'accrual') {
            fetchAccrualHistory();
        }
    }, [selectedTab]);

    return (
        <div className="container mx-auto py-6 space-y-8">
            <div className="flex flex-col space-y-2">
                <h1 className="text-3xl font-bold tracking-tight text-teal-700">Leave Settings</h1>
                <p className="text-muted-foreground text-lg">
                    Manage leave types and company holidays
                </p>
            </div>

            <Tabs
                defaultValue="carryForward"
                className="space-y-6"
                onValueChange={(value) => setSelectedTab(value)}
            >
                <TabsList className="grid w-full grid-cols-4 lg:w-[600px] bg-teal-50/50">
                    <TabsTrigger
                        value="carryForward"
                        className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors"
                    >
                        Carry Forward
                    </TabsTrigger>
                    <TabsTrigger
                        value="accrual"
                        className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors"
                    >
                        Accrual
                    </TabsTrigger>
                    <TabsTrigger
                        value="leaveTypes"
                        className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors"
                    >
                        Leave Types
                    </TabsTrigger>
                    <TabsTrigger
                        value="holidays"
                        className="data-[state=active]:bg-teal-500 data-[state=active]:text-white transition-colors"
                    >
                        Public Holidays
                    </TabsTrigger>
                </TabsList>

                {/* Carry Forward Tab */}
                <TabsContent value="carryForward" className="space-y-4">
                    <Card className="border-2 border-teal-100 bg-card shadow-sm">
                        <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0 bg-teal-50/30">
                            <div className="space-y-1">
                                <CardTitle className="text-2xl text-teal-700">Leave Carry Forward</CardTitle>
                                <CardDescription className="text-base text-muted-foreground">
                                    Process leave carry forward for employees
                                </CardDescription>
                            </div>
                            <Button
                                onClick={() => handleProcessCarryForward()}
                                className="bg-teal-500 hover:bg-teal-600 text-white"
                            >
                                <Calendar className="h-4 w-4 mr-2" />
                                Process Carry Forward
                            </Button>
                        </CardHeader>
                        <CardContent>
                            <div className="rounded-md border border-teal-100">
                                <Table>
                                    <TableHeader className="bg-teal-50/50">
                                        <TableRow>
                                            <TableHead className="font-semibold text-teal-700">Employee</TableHead>
                                            <TableHead className="font-semibold text-teal-700">Leave Type</TableHead>
                                            <TableHead className="font-semibold text-teal-700">From Year</TableHead>
                                            <TableHead className="font-semibold text-teal-700">To Year</TableHead>
                                            <TableHead className="font-semibold text-teal-700">Amount Carried</TableHead>
                                            <TableHead className="font-semibold text-teal-700">Status</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {carryForwards.map((carryForward) => (
                                            <TableRow key={carryForward.id} className="hover:bg-teal-50/40 transition-colors">
                                                <TableCell>{carryForward.employeeName}</TableCell>
                                                <TableCell>{carryForward.leaveTypeName}</TableCell>
                                                <TableCell>{carryForward.fromYear}</TableCell>
                                                <TableCell>{carryForward.toYear}</TableCell>
                                                <TableCell>{carryForward.amountCarried} days</TableCell>
                                                <TableCell>
                                                    <Badge variant="outline" className="bg-teal-50 text-teal-700 border-teal-200">
                                                        {carryForward.status}
                                                    </Badge>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                        {carryForwards.length === 0 && (
                                            <TableRow>
                                                <TableCell colSpan={6} className="text-center py-12">
                                                    <div className="flex flex-col items-center gap-3">
                                                        <Info className="h-10 w-10 text-teal-500/50" />
                                                        <p className="text-lg font-medium text-teal-700">No carry forwards processed yet</p>
                                                        <p className="text-muted-foreground">Process carry forwards to see the results</p>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>

                {/* Accrual Tab */}
                <TabsContent value="accrual" className="space-y-4">
                    <Card className="border-2 border-teal-100 bg-card shadow-sm">
                        <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0 bg-teal-50/30">
                            <div className="space-y-1">
                                <CardTitle className="text-2xl text-teal-700">Leave Accrual</CardTitle>
                                <CardDescription className="text-base text-muted-foreground">
                                    Process leave accruals for employees
                                </CardDescription>
                            </div>
                            <div className="flex gap-2">
                                <Button
                                    onClick={() => handleProcessMonthlyAccrual()}
                                    className="bg-teal-500 hover:bg-teal-600 text-white"
                                >
                                    <Calendar className="h-4 w-4 mr-2" />
                                    Process Monthly
                                </Button>
                                <Button
                                    onClick={() => handleProcessYearlyAccrual()}
                                    className="bg-teal-500 hover:bg-teal-600 text-white"
                                >
                                    <Calendar className="h-4 w-4 mr-2" />
                                    Process Yearly
                                </Button>
                            </div>
                        </CardHeader>
                        <CardContent>
                            <div className="rounded-md border-2 border-teal-200 overflow-hidden shadow-sm">
                                <Table>
                                    <TableHeader className="bg-gradient-to-r from-teal-100 to-teal-50">
                                        <TableRow className="border-b-2 border-teal-200">
                                            <TableHead className="font-semibold text-teal-800 py-4">Accrual Period</TableHead>
                                            <TableHead className="font-semibold text-teal-800 py-4">Processed Date</TableHead>
                                            <TableHead className="font-semibold text-teal-800 py-4">Employees</TableHead>
                                            <TableHead className="font-semibold text-teal-800 py-4">Total Accruals</TableHead>
                                            <TableHead className="font-semibold text-teal-800 py-4">Total Days</TableHead>
                                            <TableHead className="font-semibold text-teal-800 py-4">Status</TableHead>
                                            <TableHead className="font-semibold text-teal-800 py-4 text-right">Actions</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {isAccrualHistoryLoading ? (
                                            <TableRow>
                                                <TableCell colSpan={7} className="text-center py-16">
                                                    <div className="flex justify-center items-center">
                                                        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-500"></div>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        ) : accrualHistory.length > 0 ? (
                                            accrualHistory.map((history) => (
                                                <TableRow
                                                    key={history.id}
                                                    className="hover:bg-teal-50/50 transition-colors border-b border-teal-100"
                                                >
                                                    <TableCell className="font-medium text-teal-900">{history.accrualPeriod}</TableCell>
                                                    <TableCell className="text-teal-700">{formatDate(history.processedDate)}</TableCell>
                                                    <TableCell className="text-teal-700">{history.employeeCount}</TableCell>
                                                    <TableCell className="text-teal-700">{history.totalAccruals}</TableCell>
                                                    <TableCell className="text-teal-700 font-medium">{history.totalDaysAccrued.toFixed(2)}</TableCell>
                                                    <TableCell>
                                                        <Badge
                                                            className="px-3 py-1 rounded-full text-xs font-semibold"
                                                            variant={
                                                                history.status === 'COMPLETED' ? 'default' :
                                                                    history.status === 'PARTIAL' ? 'secondary' : 'destructive'
                                                            }
                                                        >
                                                            {history.status}
                                                        </Badge>
                                                    </TableCell>
                                                    <TableCell className="text-right">
                                                        <Button
                                                            variant="outline"
                                                            size="sm"
                                                            onClick={() => fetchAccrualDetails(history.id)}
                                                            disabled={isDetailsLoading}
                                                            className="hover:bg-teal-50 hover:text-teal-700 border-teal-200"
                                                        >
                                                            {isDetailsLoading && selectedHistoryId === history.id ? (
                                                                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                                                            ) : (
                                                                <Eye className="h-4 w-4 mr-2 text-teal-600" />
                                                            )}
                                                            View Details
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            ))
                                        ) : (
                                            <TableRow>
                                                <TableCell colSpan={7} className="text-center py-16">
                                                    <div className="flex flex-col items-center gap-4">
                                                        <Info className="h-12 w-12 text-teal-500/60" />
                                                        <p className="text-xl font-medium text-teal-800">No accruals processed yet</p>
                                                        <p className="text-muted-foreground text-teal-600">Process monthly or yearly accruals to see the results here</p>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                            </div>

                            {/* Accrual Details Dialog */}
                            <Dialog
                                open={selectedHistoryId !== null}
                                onOpenChange={(open) => !open && setSelectedHistoryId(null)}
                            >
                                <DialogContent className="max-w-4xl bg-gradient-to-b from-white to-teal-50/30 border-2 border-teal-200">
                                    <DialogHeader className="pb-4 border-b border-teal-100">
                                        <DialogTitle className="text-2xl font-bold text-teal-800">Accrual Details</DialogTitle>
                                        <DialogDescription className="text-teal-600 text-base">
                                            Detailed view of accruals for the selected period
                                        </DialogDescription>
                                    </DialogHeader>
                                    {isDetailsLoading ? (
                                        <div className="flex justify-center items-center py-16">
                                            <div className="flex flex-col items-center gap-4">
                                                <Loader2 className="h-10 w-10 animate-spin text-teal-600" />
                                                <p className="text-teal-600 animate-pulse">Loading accrual details...</p>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="max-h-[60vh] overflow-y-auto mt-4 rounded-md border-2 border-teal-100 shadow-inner bg-white">
                                            <Table>
                                                <TableHeader className="bg-gradient-to-r from-teal-100 to-teal-50 sticky top-0 z-10">
                                                    <TableRow className="border-b-2 border-teal-200">
                                                        <TableHead className="font-semibold text-teal-800 py-3 w-[20%]">Employee</TableHead>
                                                        <TableHead className="font-semibold text-teal-800 py-3 w-[15%]">Leave Type</TableHead>
                                                        <TableHead className="font-semibold text-teal-800 py-3 w-[15%]">Period</TableHead>
                                                        <TableHead className="font-semibold text-teal-800 py-3 w-[10%]">Amount</TableHead>
                                                        <TableHead className="font-semibold text-teal-800 py-3 w-[15%]">Prorated</TableHead>
                                                        <TableHead className="font-semibold text-teal-800 py-3 w-[25%]">Processed Date</TableHead>
                                                    </TableRow>
                                                </TableHeader>
                                                <TableBody>
                                                    {accrualDetails.length > 0 ? accrualDetails.map((detail) => (
                                                        <TableRow key={detail.id} className="hover:bg-teal-50/50 transition-colors border-b border-teal-100">
                                                            <TableCell className="font-medium text-teal-900">{detail.employeeName}</TableCell>
                                                            <TableCell className="text-teal-700">{detail.leaveTypeName}</TableCell>
                                                            <TableCell className="text-teal-700">{detail.accrualPeriod}</TableCell>
                                                            <TableCell className="text-teal-700 font-semibold">{detail.amount.toFixed(2)}</TableCell>
                                                            <TableCell>
                                                                <Badge
                                                                    className="px-2 py-1 rounded-full text-xs font-semibold"
                                                                    variant={detail.isProrated ? 'secondary' : 'default'}
                                                                >
                                                                    {detail.isProrated ? 'Yes' : 'No'}
                                                                </Badge>
                                                            </TableCell>
                                                            <TableCell className="text-teal-700">{formatDate(detail.processedDate)}</TableCell>
                                                        </TableRow>
                                                    )) : (
                                                        <TableRow>
                                                            <TableCell colSpan={6} className="text-center py-12">
                                                                <div className="flex flex-col items-center gap-3">
                                                                    <Info className="h-8 w-8 text-teal-500/50" />
                                                                    <p className="text-lg font-medium text-teal-700">No accrual details found</p>
                                                                </div>
                                                            </TableCell>
                                                        </TableRow>
                                                    )}
                                                </TableBody>
                                            </Table>
                                        </div>
                                    )}
                                    <DialogFooter className="mt-6 pt-4 border-t border-teal-100">
                                        <Button
                                            variant="outline"
                                            onClick={() => setSelectedHistoryId(null)}
                                            className="border-teal-200 hover:bg-teal-50 text-teal-700"
                                        >
                                            Close
                                        </Button>
                                    </DialogFooter>
                                </DialogContent>
                            </Dialog>
                        </CardContent>
                    </Card>
                </TabsContent>

                {/* Leave Types Tab */}
                <TabsContent value="leaveTypes" className="space-y-4">
                    <Card className="border-2 border-teal-100 bg-card shadow-sm">
                        <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0 bg-teal-50/30">
                            <div className="space-y-1">
                                <CardTitle className="text-2xl text-teal-700">Leave Types</CardTitle>
                                <CardDescription className="text-base text-muted-foreground">
                                    Configure available leave types for your organization
                                </CardDescription>
                            </div>
                            <Button onClick={handleAddLeaveType} className="bg-teal-500 hover:bg-teal-600 text-white">
                                <Plus className="h-4 w-4 mr-2" />
                                Add Leave Type
                            </Button>
                        </CardHeader>
                        <CardContent>
                            {isInitialLoading ? (
                                <div className="flex justify-center items-center py-12">
                                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-500"></div>
                                </div>
                            ) : (
                                <div className="rounded-md border border-teal-100">
                                    <Table>
                                        <TableHeader className="bg-teal-50/50">
                                            <TableRow className="bg-gradient-to-r from-teal-50 to-teal-100/50 hover:bg-teal-50/50">
                                                <TableHead className="font-semibold text-teal-900">Name</TableHead>
                                                <TableHead className="font-semibold text-teal-900">Category</TableHead>
                                                <TableHead className="font-semibold text-teal-900">Status</TableHead>
                                                <TableHead className="font-semibold text-teal-900">Max Duration</TableHead>
                                                <TableHead className="font-semibold text-teal-900">Accrual</TableHead>
                                                <TableHead className="font-semibold text-teal-900">Carry Forward</TableHead>
                                                <TableHead className="font-semibold text-teal-900">Requirements</TableHead>
                                                <TableHead className="font-semibold text-teal-900 w-[100px]">Actions</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {leaveTypes.map((leaveType) => (
                                                <TableRow key={leaveType.id} className="hover:bg-teal-50/40 transition-colors">
                                                    <TableCell>
                                                        <div className="flex items-center gap-2">
                                                            <Badge variant="outline" className="font-medium bg-teal-50 text-teal-700 border-teal-200">
                                                                {leaveType.name}
                                                            </Badge>
                                                        </div>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Badge
                                                            variant={leaveType.isDefault ? "default" : "secondary"}
                                                            className="font-medium"
                                                        >
                                                            {leaveType.isDefault ? "Default" : "Custom"}
                                                        </Badge>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Badge variant={leaveType.isEnabled ? "default" : "secondary"} className="bg-teal-100 text-teal-800 hover:bg-teal-200">
                                                            {leaveType.isEnabled ? "Enabled" : "Disabled"}
                                                        </Badge>
                                                    </TableCell>
                                                    <TableCell>
                                                        <span className="font-medium text-teal-700">
                                                            {leaveType.maxDuration ? `${leaveType.maxDuration} days` : 'N/A'}
                                                        </span>
                                                    </TableCell>
                                                    <TableCell>
                                                        {leaveType.accrualBased ? (
                                                            <span className="font-medium text-teal-700">
                                                                {leaveType.accrualRate} days/month
                                                            </span>
                                                        ) : (
                                                            <span className="text-muted-foreground">Not accrual based</span>
                                                        )}
                                                    </TableCell>
                                                    <TableCell>
                                                        {leaveType.isCarryForwardEnabled ? (
                                                            <span className="font-medium text-teal-700">
                                                                Up to {leaveType.carryForwardCap} days
                                                            </span>
                                                        ) : (
                                                            <span className="text-muted-foreground">Not allowed</span>
                                                        )}
                                                    </TableCell>
                                                    <TableCell>
                                                        <div className="flex flex-col gap-1">
                                                            {leaveType.requireReason && (
                                                                <Badge variant="outline" className="w-fit bg-teal-50 text-teal-700 border-teal-200">
                                                                    Reason Required
                                                                </Badge>
                                                            )}
                                                            {leaveType.requireDocument && (
                                                                <Badge variant="outline" className="w-fit bg-teal-50 text-teal-700 border-teal-200">
                                                                    Document Required
                                                                </Badge>
                                                            )}
                                                        </div>
                                                    </TableCell>
                                                    <TableCell className="text-right space-x-1">
                                                        <Button
                                                            variant="ghost"
                                                            size="sm"
                                                            onClick={() => handleEditLeaveType(leaveType)}
                                                            className="hover:bg-teal-50 hover:text-teal-700"
                                                        >
                                                            <Pencil className="h-4 w-4" />
                                                            <span className="sr-only">Edit</span>
                                                        </Button>
                                                        {!leaveType.isDefault && (
                                                            <Button
                                                                variant="ghost"
                                                                size="sm"
                                                                className="text-red-600 hover:text-red-700 hover:bg-red-50"
                                                                onClick={() => handleDeleteClick(leaveType.id.toString(), "leaveType")}
                                                            >
                                                                <Trash className="h-4 w-4" />
                                                                <span className="sr-only">Delete</span>
                                                            </Button>
                                                        )}
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                            {leaveTypes.length === 0 && !isInitialLoading && (
                                                <TableRow>
                                                    <TableCell colSpan={8} className="text-center py-12">
                                                        <div className="flex flex-col items-center gap-3">
                                                            <Info className="h-10 w-10 text-teal-500/50" />
                                                            <p className="text-lg font-medium text-teal-700">No leave types configured yet</p>
                                                            <p className="text-muted-foreground">Get started by adding your first leave type</p>
                                                            <Button
                                                                variant="outline"
                                                                size="lg"
                                                                className="mt-2 border-teal-200 hover:bg-teal-50 hover:text-teal-700"
                                                                onClick={handleAddLeaveType}
                                                            >
                                                                <Plus className="h-5 w-5 mr-2" />
                                                                Add Leave Type
                                                            </Button>
                                                        </div>
                                                    </TableCell>
                                                </TableRow>
                                            )}
                                        </TableBody>
                                    </Table>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </TabsContent>

                {/* Public Holidays Tab */}
                <TabsContent value="holidays" className="space-y-4">
                    <Card className="border-2 border-teal-100 bg-card shadow-sm">
                        <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0 bg-teal-50/30">
                            <div className="space-y-1">
                                <CardTitle className="text-2xl text-teal-700">Public Holidays</CardTitle>
                                <CardDescription className="text-base text-muted-foreground">
                                    Manage company holidays and special events
                                </CardDescription>
                            </div>
                            <Button onClick={handleAddHoliday} className="bg-teal-500 hover:bg-teal-600 text-white">
                                <Plus className="h-4 w-4 mr-2" />
                                Add Holiday
                            </Button>
                        </CardHeader>
                        <CardContent>
                            <div className="rounded-md border border-teal-100">
                                <Table>
                                    <TableHeader className="bg-teal-50/50">
                                        <TableRow>
                                            <TableHead className="font-semibold text-teal-700">Holiday Name</TableHead>
                                            <TableHead className="font-semibold text-teal-700">Date</TableHead>
                                            <TableHead className="font-semibold text-teal-700">Description</TableHead>
                                            <TableHead className="font-semibold text-teal-700 text-right">Actions</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {publicHolidays
                                            .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
                                            .map((holiday) => (
                                                <TableRow key={holiday.id} className="hover:bg-teal-50/40 transition-colors">
                                                    <TableCell className="font-medium text-teal-700">{holiday.name}</TableCell>
                                                    <TableCell>
                                                        <div className="flex items-center gap-2">
                                                            <Calendar className="h-4 w-4 text-teal-500/50" />
                                                            <span className="font-medium text-teal-700">
                                                                {format(new Date(holiday.date), "MMMM dd, yyyy")}
                                                            </span>
                                                        </div>
                                                    </TableCell>
                                                    <TableCell className="max-w-[300px] truncate" title={holiday.description}>
                                                        <span className="text-muted-foreground">
                                                            {holiday.description || "-"}
                                                        </span>
                                                    </TableCell>
                                                    <TableCell className="text-right space-x-1">
                                                        <Button
                                                            variant="ghost"
                                                            size="sm"
                                                            onClick={() => handleEditHoliday(holiday)}
                                                            className="hover:bg-teal-50 hover:text-teal-700"
                                                        >
                                                            <Pencil className="h-4 w-4" />
                                                            <span className="sr-only">Edit</span>
                                                        </Button>
                                                        <Button
                                                            variant="ghost"
                                                            size="sm"
                                                            className="text-red-600 hover:text-red-700 hover:bg-red-50"
                                                            onClick={() => handleDeleteClick(holiday.id, "holiday")}
                                                        >
                                                            <Trash className="h-4 w-4" />
                                                            <span className="sr-only">Delete</span>
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        {publicHolidays.length === 0 && (
                                            <TableRow>
                                                <TableCell colSpan={4} className="text-center py-12">
                                                    <div className="flex flex-col items-center gap-3">
                                                        <Info className="h-10 w-10 text-teal-500/50" />
                                                        <p className="text-lg font-medium text-teal-700">No public holidays configured yet</p>
                                                        <p className="text-muted-foreground">Get started by adding your first holiday</p>
                                                        <Button
                                                            variant="outline"
                                                            size="lg"
                                                            className="mt-2 border-teal-200 hover:bg-teal-50 hover:text-teal-700"
                                                            onClick={handleAddHoliday}
                                                        >
                                                            <Plus className="h-5 w-5 mr-2" />
                                                            Add Holiday
                                                        </Button>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>
            </Tabs>

            {/* Leave Type Dialog */}
            <Dialog open={isLeaveTypeDialogOpen} onOpenChange={setIsLeaveTypeDialogOpen}>
                <DialogContent className="sm:max-w-[550px]">
                    <DialogHeader>
                        <DialogTitle>{isEditMode ? "Edit Leave Type" : "Add Leave Type"}</DialogTitle>
                        <DialogDescription>
                            {isEditMode
                                ? "Update the leave type details below"
                                : "Create a new leave type for your organization"}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="name">Leave Type Name <span className="text-red-500">*</span></Label>
                                <Input
                                    id="name"
                                    value={leaveTypeForm.name}
                                    onChange={(e) => setLeaveTypeForm({ ...leaveTypeForm, name: e.target.value })}
                                    placeholder="e.g., Annual Leave"
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="is-enabled">Leave Type Status</Label>
                                <div className="flex items-center space-x-2">
                                    <Switch
                                        id="is-enabled"
                                        checked={leaveTypeForm.isEnabled}
                                        onCheckedChange={(checked) => setLeaveTypeForm({ ...leaveTypeForm, isEnabled: checked })}
                                    />
                                    <span>{leaveTypeForm.isEnabled ? "Enabled" : "Disabled"}</span>
                                </div>
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="description">Description</Label>
                            <Input
                                id="description"
                                value={leaveTypeForm.description}
                                onChange={(e) => setLeaveTypeForm({ ...leaveTypeForm, description: e.target.value })}
                                placeholder="Brief description of this leave type"
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="max-duration">Maximum Duration (days)</Label>
                                <Input
                                    type="number"
                                    id="max-duration"
                                    value={leaveTypeForm.maxDuration}
                                    onChange={(e) => setLeaveTypeForm({
                                        ...leaveTypeForm,
                                        maxDuration: parseInt(e.target.value) || 0
                                    })}
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="paid">Paid Leave</Label>
                                <div className="flex items-center space-x-2">
                                    <Switch
                                        id="paid"
                                        checked={leaveTypeForm.paid}
                                        onCheckedChange={(checked) => setLeaveTypeForm({ ...leaveTypeForm, paid: checked })}
                                    />
                                    <span>{leaveTypeForm.paid ? "Yes" : "No"}</span>
                                </div>
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="accrual-based">Accrual Based</Label>
                                <div className="flex items-center space-x-2">
                                    <Switch
                                        id="accrual-based"
                                        checked={leaveTypeForm.accrualBased}
                                        onCheckedChange={(checked) => setLeaveTypeForm({ ...leaveTypeForm, accrualBased: checked })}
                                    />
                                    <span>{leaveTypeForm.accrualBased ? "Yes" : "No"}</span>
                                </div>
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="accrual-rate">Accrual Rate (days/month)</Label>
                                <Input
                                    type="number"
                                    id="accrual-rate"
                                    value={leaveTypeForm.accrualRate || ""}
                                    onChange={(e) => setLeaveTypeForm({
                                        ...leaveTypeForm,
                                        accrualRate: e.target.value ? parseFloat(e.target.value) : null
                                    })}
                                    disabled={!leaveTypeForm.accrualBased}
                                />
                                {leaveTypeForm.accrualBased && leaveTypeForm.maxDuration && (
                                    <p className="text-sm text-muted-foreground">
                                        Maximum monthly accrual: {(leaveTypeForm.maxDuration / 12).toFixed(2)} days
                                        (based on max duration of {leaveTypeForm.maxDuration} days)
                                    </p>
                                )}
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="is-carry-forward-enabled">Carry Forward</Label>
                                <div className="flex items-center space-x-2">
                                    <Switch
                                        id="is-carry-forward-enabled"
                                        checked={leaveTypeForm.isCarryForwardEnabled}
                                        onCheckedChange={(checked) => setLeaveTypeForm({ ...leaveTypeForm, isCarryForwardEnabled: checked })}
                                    />
                                    <span>{leaveTypeForm.isCarryForwardEnabled ? "Yes" : "No"}</span>
                                </div>
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="carry-forward-cap">Carry Forward Cap (days)</Label>
                                <Input
                                    type="number"
                                    id="carry-forward-cap"
                                    value={leaveTypeForm.carryForwardCap || ""}
                                    onChange={(e) => setLeaveTypeForm({
                                        ...leaveTypeForm,
                                        carryForwardCap: e.target.value ? parseFloat(e.target.value) : null
                                    })}
                                    disabled={!leaveTypeForm.isCarryForwardEnabled}
                                />
                                {leaveTypeForm.isCarryForwardEnabled && leaveTypeForm.maxDuration && (
                                    <p className="text-sm text-muted-foreground">
                                        Maximum carry forward: {leaveTypeForm.maxDuration} days
                                        (based on max duration)
                                    </p>
                                )}
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <div className="flex items-center space-x-2">
                                    <Checkbox
                                        id="requires-reason"
                                        checked={leaveTypeForm.requireReason}
                                        onCheckedChange={(checked) =>
                                            setLeaveTypeForm({ ...leaveTypeForm, requireReason: checked === true })
                                        }
                                    />
                                    <Label htmlFor="requires-reason">Reason Required</Label>
                                </div>
                            </div>
                            <div className="space-y-2">
                                <div className="flex items-center space-x-2">
                                    <Checkbox
                                        id="requires-document"
                                        checked={leaveTypeForm.requireDocument}
                                        onCheckedChange={(checked) =>
                                            setLeaveTypeForm({ ...leaveTypeForm, requireDocument: checked === true })
                                        }
                                    />
                                    <Label htmlFor="requires-document">Document Required</Label>
                                </div>
                            </div>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsLeaveTypeDialogOpen(false)}>
                            Cancel
                        </Button>
                        <Button onClick={handleSaveLeaveType} disabled={isLoading}>
                            {isLoading ? "Saving..." : (isEditMode ? "Save Changes" : "Create")}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Holiday Dialog */}
            <Dialog open={isHolidayDialogOpen} onOpenChange={setIsHolidayDialogOpen}>
                <DialogContent className="sm:max-w-[550px]">
                    <DialogHeader>
                        <DialogTitle>{isHolidayEditMode ? "Edit Holiday" : "Add Holiday"}</DialogTitle>
                        <DialogDescription>
                            {isHolidayEditMode
                                ? "Update the holiday details below"
                                : "Add a new company holiday or special event"}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-4">
                        <div className="space-y-2">
                            <Label htmlFor="holiday-name">Holiday Name <span className="text-red-500">*</span></Label>
                            <Input
                                id="holiday-name"
                                value={holidayForm.name}
                                onChange={(e) => setHolidayForm({ ...holidayForm, name: e.target.value })}
                                placeholder="e.g., New Year's Day"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="holiday-date">Date <span className="text-red-500">*</span></Label>
                            <Input
                                id="holiday-date"
                                type="date"
                                value={holidayForm.date}
                                onChange={(e) => setHolidayForm({ ...holidayForm, date: e.target.value })}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="holiday-description">Description (optional)</Label>
                            <Input
                                id="holiday-description"
                                value={holidayForm.description}
                                onChange={(e) => setHolidayForm({ ...holidayForm, description: e.target.value })}
                                placeholder="Brief description of this holiday"
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsHolidayDialogOpen(false)}>
                            Cancel
                        </Button>
                        <Button onClick={handleSaveHoliday} disabled={isLoading}>
                            {isLoading ? "Saving..." : (isHolidayEditMode ? "Save Changes" : "Create")}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>
                            <div className="flex items-center gap-2">
                                <AlertCircle className="h-5 w-5 text-destructive" />
                                Confirm Deletion
                            </div>
                        </AlertDialogTitle>
                        <AlertDialogDescription>
                            {deleteItemType === "leaveType"
                                ? "Are you sure you want to delete this leave type? This action cannot be undone."
                                : "Are you sure you want to delete this holiday? This action cannot be undone."}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDeleteConfirm}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            {isLoading ? "Deleting..." : "Delete"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};

export default LeaveSettings;
