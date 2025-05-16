import React, { useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/components/ui/dialog";
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
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { LeaveRequest, cancelLeaveRequest } from "@/services/leaveRequestService";
import { format, parseISO } from "date-fns";
import {
    CalendarDays,
    Clock,
    FileText,
    AlertCircle,
    UserCircle,
    MessageSquare,
    Calendar,
    CheckCircle2,
    Loader2,
    Info,
    XCircle,
    CheckCircle
} from "lucide-react";
import { toast } from "@/components/ui/sonner";
import { cn } from "@/lib/utils";
import { Separator } from "@/components/ui/separator";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import api from '@/services/api';

interface LeaveRequestDetailDialogProps {
    isOpen: boolean;
    onClose: () => void;
    leaveRequest: LeaveRequest | null;
    onLeaveRequestUpdated: () => void;
}

const LeaveRequestDetailDialog: React.FC<LeaveRequestDetailDialogProps> = ({
    isOpen,
    onClose,
    leaveRequest,
    onLeaveRequestUpdated,
}) => {
    const [isConfirmDialogOpen, setIsConfirmDialogOpen] = useState(false);
    const [isCancelling, setIsCancelling] = useState(false);
    const [loadingDocIdx, setLoadingDocIdx] = useState<number | null>(null);

    if (!leaveRequest) return null;

    const formatDate = (dateStr: string) => {
        return format(new Date(dateStr), "MMM dd, yyyy");
    };

    const formatDateTime = (dateTimeStr: string) => {
        try {
            const date = parseISO(dateTimeStr);
            return format(date, "MMM dd, yyyy 'at' h:mm a");
        } catch (e) {
            return dateTimeStr;
        }
    };

    const getStatusBadgeColor = (status: string) => {
        switch (status) {
            case "APPROVED":
                return "bg-green-100 text-green-800 border-green-200";
            case "PENDING":
                return "bg-yellow-100 text-yellow-800 border-yellow-200";
            case "REJECTED":
                return "bg-red-100 text-red-800 border-red-200";
            case "CANCELLED":
                return "bg-gray-100 text-gray-600 border-gray-200";
            default:
                return "bg-gray-100 text-gray-800 border-gray-200";
        }
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case "APPROVED":
                return <CheckCircle className="h-4 w-4 text-green-600 mr-1" />;
            case "PENDING":
                return <Clock className="h-4 w-4 text-yellow-600 mr-1" />;
            case "REJECTED":
                return <XCircle className="h-4 w-4 text-red-600 mr-1" />;
            case "CANCELLED":
                return <XCircle className="h-4 w-4 text-gray-500 mr-1" />;
            default:
                return null;
        }
    };

    const getLeaveTypeBadgeColor = (type: string) => {
        if (type.includes("Annual") || type.includes("PTO")) {
            return "bg-teal-100 text-teal-800 border-teal-200";
        } else if (type.includes("Sick")) {
            return "bg-red-100 text-red-800 border-red-200";
        } else if (type.includes("Personal")) {
            return "bg-purple-100 text-purple-800 border-purple-200";
        } else {
            return "bg-gray-100 text-gray-800 border-gray-200";
        }
    };

    const handleCancelRequest = async () => {
        try {
            setIsCancelling(true);
            await cancelLeaveRequest(leaveRequest.id);
            setIsConfirmDialogOpen(false);
            toast.success("Your leave request has been successfully cancelled.");
            onLeaveRequestUpdated();
            onClose();
        } catch (error) {
            toast.error("Failed to cancel leave request. Please try again.");
        } finally {
            setIsCancelling(false);
        }
    };

    const handleViewDocument = async (blobUrl: string, idx: number) => {
        setLoadingDocIdx(idx);
        try {
            const response = await api.get<{ downloadUrl: string }>(`/documents/download-url`, {
                params: { blobUrl },
            });
            window.open(response.data.downloadUrl, '_blank', 'noopener,noreferrer');
        } catch (error) {
            toast.error('Failed to fetch document download link.');
        } finally {
            setLoadingDocIdx(null);
        }
    };

    return (
        <>
            <Dialog open={isOpen} onOpenChange={onClose}>
                <DialogContent className="sm:max-w-[550px] p-0 overflow-hidden">
                    {/* Header with status banner */}
                    <div className={cn(
                        "w-full py-6 px-7",
                        leaveRequest.status === "APPROVED" ? "bg-green-50" :
                            leaveRequest.status === "PENDING" ? "bg-yellow-50" :
                                leaveRequest.status === "REJECTED" ? "bg-red-50" : "bg-gray-50"
                    )}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle className="text-xl font-semibold">Leave Request</DialogTitle>
                                <Badge
                                    variant="outline"
                                    className={cn("text-sm px-3 py-1 flex items-center", getStatusBadgeColor(leaveRequest.status))}
                                >
                                    {getStatusIcon(leaveRequest.status)}
                                    {leaveRequest.status}
                                </Badge>
                            </div>
                            <DialogDescription className="mt-1">
                                Request #{leaveRequest.id}
                            </DialogDescription>
                        </DialogHeader>
                    </div>

                    <div className="px-7 py-8 space-y-6">
                        {/* Employee Card */}
                        <div className="flex items-center p-4 rounded-lg border bg-gray-50/50">
                            <Avatar className="h-14 w-14 border-2 border-white shadow-sm">
                                <AvatarImage
                                    src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${leaveRequest.employeeId}`}
                                    alt={leaveRequest.employeeName}
                                />
                                <AvatarFallback className="bg-teal-100 text-teal-800">{leaveRequest.employeeName.charAt(0)}</AvatarFallback>
                            </Avatar>
                            <div className="ml-4 flex-1">
                                <h3 className="font-medium text-lg">{leaveRequest.employeeName}</h3>
                            </div>
                            <Badge variant="outline" className={cn("ml-auto", getLeaveTypeBadgeColor(leaveRequest.type))}>
                                {leaveRequest.type}
                            </Badge>
                        </div>

                        {/* Leave Details */}
                        <div className="grid grid-cols-2 gap-4">
                            <div className="flex flex-col p-4 rounded-lg border bg-teal-50/30">
                                <span className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5">Start Date</span>
                                <div className="flex items-center">
                                    <Calendar className="mr-2 h-4 w-4 text-teal-600" />
                                    <span className="font-medium">{formatDate(leaveRequest.startDate)}</span>
                                </div>
                            </div>
                            <div className="flex flex-col p-4 rounded-lg border bg-teal-50/30">
                                <span className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5">End Date</span>
                                <div className="flex items-center">
                                    <Calendar className="mr-2 h-4 w-4 text-teal-600" />
                                    <span className="font-medium">{formatDate(leaveRequest.endDate)}</span>
                                </div>
                            </div>
                        </div>

                        <div className="flex items-center px-4 py-3 rounded-lg border bg-green-50/30">
                            <Clock className="h-5 w-5 text-green-600 mr-3 flex-shrink-0" />
                            <div>
                                <span className="text-sm font-medium">Duration</span>
                                <p className="text-base font-semibold">{leaveRequest.days} {leaveRequest.days === 1 ? "day" : "days"}</p>
                            </div>
                        </div>

                        {/* Reason */}
                        <div>
                            <div className="flex items-center mb-2">
                                <MessageSquare className="h-4 w-4 mr-2 text-gray-500" />
                                <h4 className="text-sm font-medium">Reason for Leave</h4>
                                {leaveRequest.requireReason && (
                                    <TooltipProvider>
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <div className="ml-2">
                                                    <Info className="h-3.5 w-3.5 text-teal-500" />
                                                </div>
                                            </TooltipTrigger>
                                            <TooltipContent side="top">
                                                <p className="text-xs">Reason is required for this leave type</p>
                                            </TooltipContent>
                                        </Tooltip>
                                    </TooltipProvider>
                                )}
                            </div>
                            <div className="p-4 rounded-lg border bg-gray-50">
                                <p className="text-sm text-gray-700 leading-relaxed">
                                    {leaveRequest.reason || "No reason provided"}
                                </p>
                            </div>
                        </div>

                        {/* Document(s) */}
                        {leaveRequest.requireDocument && (
                            <div>
                                <div className="flex items-center mb-2">
                                    <FileText className="h-4 w-4 mr-2 text-gray-500" />
                                    <h4 className="text-sm font-medium">Supporting Document(s)</h4>
                                    <TooltipProvider>
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <div className="ml-2">
                                                    <Info className="h-3.5 w-3.5 text-teal-500" />
                                                </div>
                                            </TooltipTrigger>
                                            <TooltipContent side="top">
                                                <p className="text-xs">Document is required for this leave type</p>
                                            </TooltipContent>
                                        </Tooltip>
                                    </TooltipProvider>
                                </div>
                                {leaveRequest.documents && leaveRequest.documents.length > 0 ? (
                                    <div className="flex flex-col gap-2">
                                        {leaveRequest.documents.map((doc, idx) => (
                                            <Button
                                                key={idx}
                                                variant="outline"
                                                size="sm"
                                                className="flex items-center w-fit justify-center"
                                                onClick={() => handleViewDocument(doc.blobUrl, idx)}
                                                disabled={loadingDocIdx === idx}
                                            >
                                                <FileText className="mr-2 h-4 w-4" />
                                                {loadingDocIdx === idx ? 'Loading...' : `View ${doc.name ? doc.name : `Document ${idx + 1}`}`}
                                            </Button>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="p-3 rounded-lg border bg-amber-50 text-amber-800 text-sm flex items-center">
                                        <AlertCircle className="h-4 w-4 mr-2 text-amber-600" />
                                        No documents provided
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Approver Info (if approved) */}
                        {(leaveRequest.status === "APPROVED" || leaveRequest.status === "REJECTED") && leaveRequest.approverName && (
                            <div>
                                <Separator className="my-3" />
                                <div className={cn(
                                    "flex items-center p-4 rounded-lg mt-4",
                                    leaveRequest.status === "APPROVED" ? "bg-green-50" : "bg-red-50"
                                )}>
                                    {leaveRequest.status === "APPROVED" ? (
                                        <CheckCircle2 className="h-5 w-5 text-green-600 mr-3 flex-shrink-0" />
                                    ) : (
                                        <XCircle className="h-5 w-5 text-red-600 mr-3 flex-shrink-0" />
                                    )}
                                    <div>
                                        <div className={cn(
                                            "font-medium",
                                            leaveRequest.status === "APPROVED" ? "text-green-800" : "text-red-800"
                                        )}>
                                            {leaveRequest.status === "APPROVED" ? "Approved" : "Rejected"} by {leaveRequest.approverName}
                                        </div>
                                        {leaveRequest.approvedAt && (
                                            <div className={cn(
                                                "text-xs mt-0.5",
                                                leaveRequest.status === "APPROVED" ? "text-green-700" : "text-red-700"
                                            )}>
                                                on {formatDateTime(leaveRequest.approvedAt)}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Comments */}
                        {leaveRequest.comments && (
                            <div>
                                <div className="flex items-center mb-2">
                                    <MessageSquare className="h-4 w-4 mr-2 text-gray-500" />
                                    <h4 className="text-sm font-medium">Comments</h4>
                                </div>
                                <div className="p-4 rounded-lg border bg-gray-50">
                                    <p className="text-sm italic text-gray-600 leading-relaxed">"{leaveRequest.comments}"</p>
                                </div>
                            </div>
                        )}

                        {/* Date Info */}
                        <div className="pt-2 space-y-1 text-xs text-gray-500">
                            <div className="flex items-center">
                                <span>Created: {formatDateTime(leaveRequest.createdAt)}</span>
                            </div>
                        </div>
                    </div>

                    {/* Footer */}
                    <div className="px-7 py-4 bg-gray-50 border-t flex items-center justify-end">
                        <div className="flex gap-2">
                            {leaveRequest.status === "PENDING" && (
                                <Button
                                    variant="destructive"
                                    onClick={() => setIsConfirmDialogOpen(true)}
                                    className="px-4"
                                >
                                    Cancel Request
                                </Button>
                            )}
                            <Button variant="outline" onClick={onClose}>
                                Close
                            </Button>
                        </div>
                    </div>
                </DialogContent>
            </Dialog>

            {/* Confirmation Dialog */}
            <AlertDialog open={isConfirmDialogOpen} onOpenChange={setIsConfirmDialogOpen}>
                <AlertDialogContent className="max-w-[400px]">
                    <AlertDialogHeader>
                        <AlertDialogTitle className="text-red-600">Cancel Leave Request</AlertDialogTitle>
                        <AlertDialogDescription>
                            <span className="flex items-start mt-2 space-x-3 p-4 bg-amber-50 rounded-lg border border-amber-100">
                                <AlertCircle className="h-5 w-5 text-amber-500 flex-shrink-0 mt-0.5" />
                                <span className="text-amber-800">
                                    Are you sure you want to cancel this leave request? This action cannot be undone.
                                </span>
                            </span>
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter className="gap-2">
                        <AlertDialogCancel disabled={isCancelling} className="mt-0">Keep Request</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={(e) => {
                                e.preventDefault();
                                handleCancelRequest();
                            }}
                            disabled={isCancelling}
                            className="bg-red-600 hover:bg-red-700 focus:ring-red-600"
                        >
                            {isCancelling ? (
                                <>
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                    Cancelling...
                                </>
                            ) : (
                                "Yes, cancel request"
                            )}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
};

export default LeaveRequestDetailDialog; 