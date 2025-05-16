import React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { format, isWeekend } from "date-fns";
import { CalendarIcon, Upload, File, Loader2, Clock, Link as LinkIcon, AlertCircle } from "lucide-react";
import { toast } from "@/components/ui/sonner";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
  FormDescription,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import api from "@/services/api";
import s3Service from "@/services/s3Service";
import { validateFile } from "@/config/s3Config";
import { Checkbox } from "@/components/ui/checkbox";
import azureBlobService from '@/services/azureBlobService';

// Define the leave type interface
interface LeaveType {
  id: number;
  value: string;
  label: string;
  requiresReason: boolean;
  requiresDocument: boolean;
}

const formSchema = z.object({
  leaveType: z.string({
    required_error: "Please select a leave type",
  }),
  startDate: z.date({
    required_error: "Start date is required",
  }),
  endDate: z.date({
    required_error: "End date is required",
  }),
  halfDayStart: z.boolean().default(false),
  halfDayEnd: z.boolean().default(false),
  reason: z.string().optional(),
  documentUpload: z.any().optional(),
  documentUrl: z.string().optional(),
});

type LeaveRequestFormValues = z.infer<typeof formSchema>;

type LeaveRequestDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

const LeaveRequestDialog = ({ open, onOpenChange }: LeaveRequestDialogProps) => {
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [currentLeaveType, setCurrentLeaveType] = React.useState<string | null>(null);
  const [uploadedFile, setUploadedFile] = React.useState<File | null>(null);
  const [fileUrl, setFileUrl] = React.useState<string | null>(null);
  const [isUploading, setIsUploading] = React.useState(false);
  const [uploadProgress, setUploadProgress] = React.useState(0);
  const [leaveTypes, setLeaveTypes] = React.useState<LeaveType[]>([]);
  const [documents, setDocuments] = React.useState<{ name: string; blobUrl: string }[]>([]);

  // Fetch leave types from API
  React.useEffect(() => {
    const fetchLeaveTypes = async () => {
      try {
        const response = await api.get("/leaveTypes");

        const mappedTypes = response.data
          .filter((type: any) => type.name !== 'Public Holidays')
          .map((type: any) => ({
            id: type.id,
            value: type.name.toLowerCase().replace(/\s+/g, ''),
            label: type.name,
            requiresReason: type.requireReason,
            requiresDocument: type.requireDocument
          }));
        setLeaveTypes(mappedTypes);
      } catch (error) {
        console.error("Failed to fetch leave types:", error);
        // Fallback to default leave types if API call fails
        setLeaveTypes([
          { id: 1, value: "annual", label: "Fall Back Annual Leave", requiresReason: false, requiresDocument: false },
          { id: 2, value: "sick", label: "Fall Back Sick Leave", requiresReason: true, requiresDocument: true },
          { id: 3, value: "personal", label: "Fall Back Personal Leave", requiresReason: true, requiresDocument: false },
        ]);
      }
    };

    fetchLeaveTypes();
  }, []);

  const form = useForm<LeaveRequestFormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      reason: "",
      halfDayStart: false,
      halfDayEnd: false,
    },
  });

  const watchLeaveType = form.watch("leaveType");

  React.useEffect(() => {
    if (watchLeaveType) {
      setCurrentLeaveType(watchLeaveType);
    }
  }, [watchLeaveType]);

  const isReasonRequired = React.useMemo(() => {
    const selectedType = leaveTypes.find(type => type.value === currentLeaveType);
    return selectedType?.requiresReason || false;
  }, [currentLeaveType, leaveTypes]);

  const isDocumentRequired = React.useMemo(() => {
    const selectedType = leaveTypes.find(type => type.value === currentLeaveType);
    return selectedType?.requiresDocument || false;
  }, [currentLeaveType, leaveTypes]);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate the file before uploading
    const validationResult = validateFile(file);
    if (!validationResult.valid) {
      toast.error(validationResult.message);
      return;
    }

    setUploadedFile(file);
    form.setValue("documentUpload", file);

    // Upload file to Azure Blob Storage
    await uploadFileToAzure(file);
  };

  const uploadFileToAzure = async (file: File) => {
    try {
      setIsUploading(true);
      setUploadProgress(0);

      toast.info("Please wait while we upload your document...");

      // 1. Get presigned upload URL from backend
      const { uploadUrl, blobUrl } = await azureBlobService.getPresignedUploadUrl(file.name, file.type);

      // 2. Upload file to Azure Blob Storage
      await azureBlobService.uploadFileToAzure(uploadUrl, file, (progress) => setUploadProgress(progress));

      // 3. Store document metadata in documents array (only one document for now)
      setDocuments([{ name: file.name, blobUrl }]);
      setFileUrl(blobUrl);
      form.setValue("documentUrl", blobUrl);

      toast.success("Your document has been uploaded successfully.");
    } catch (error) {
      console.error("Failed to upload document:", error);
      toast.error(
        typeof error === 'object' && error !== null && 'message' in error
          ? `${error.message}`
          : "Failed to upload document. Please try again."
      );

      // Reset file state on error
      setUploadedFile(null);
      setFileUrl(null);
      setDocuments([]);
      form.setValue("documentUpload", null);
    } finally {
      setIsUploading(false);
    }
  };

  const onSubmit = async (data: LeaveRequestFormValues) => {
    // Validate that end date is not before start date
    if (data.endDate < data.startDate) {
      form.setError("endDate", {
        message: "End date cannot be before start date"
      });
      return;
    }

    // Validate that selected dates aren't weekends
    if (isWeekend(data.startDate)) {
      form.setError("startDate", {
        message: "Weekends cannot be selected for leave"
      });
      return;
    }

    if (isWeekend(data.endDate)) {
      form.setError("endDate", {
        message: "Weekends cannot be selected for leave"
      });
      return;
    }

    // Get the leave type from the selected leave type value
    const selectedLeaveType = leaveTypes.find(type => type.value === data.leaveType);
    if (!selectedLeaveType) {
      toast.error("Invalid leave type selected.");
      return;
    }

    // Check if document is required for this leave type
    const documentRequired = selectedLeaveType.requiresDocument;

    // Check if the document is still uploading
    if (isUploading) {
      toast.error("Please wait for the document to finish uploading before submitting.");
      return;
    }

    // Check if a document is required but not uploaded
    if (documentRequired && !uploadedFile) {
      toast.error("Please upload a supporting document for this leave type.");
      return;
    }

    // Validate that required document was uploaded successfully
    if (documentRequired && (!fileUrl || !documents.length)) {
      toast.error("The document upload failed or is incomplete. Please try uploading again.");
      return;
    }

    // If a file was uploaded, verify both fileUrl and document exist
    if (uploadedFile && (!fileUrl || !documents.length)) {
      toast.error("Your document upload is incomplete. Please try uploading again.");
      return;
    }

    setIsSubmitting(true);

    // Calculate the number of days (including weekends for now)
    const differenceInTime = data.endDate.getTime() - data.startDate.getTime();
    const differenceInDays = Math.ceil(differenceInTime / (1000 * 3600 * 24)) + 1;

    // Format dates for API
    const startDate = format(data.startDate, "yyyy-MM-dd");
    const endDate = format(data.endDate, "yyyy-MM-dd");

    // Prepare the request payload
    const leaveRequest = {
      leaveTypeId: selectedLeaveType.id,
      startDate,
      endDate,
      halfDayStart: data.halfDayStart,
      halfDayEnd: data.halfDayEnd,
      leaveRequestReason: data.reason || null,
      documents,
    };

    // Send the request to the API
    try {
      const response = await api.post("/leaveRequests", leaveRequest);
      const data = response.data;

      if (data.error) {
        console.log(data.error);
        toast.error(data.error);
        setIsSubmitting(false);
        return;
      }

      toast.success(`Your ${selectedLeaveType.label} leave request for ${differenceInDays} days has been submitted successfully.`);

      // Reset form and close dialog
      form.reset();
      setUploadedFile(null);
      setFileUrl(null);
      setDocuments([]);
      onOpenChange(false);
    } catch (error) {
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px] border-teal-200 shadow-lg">
        <DialogHeader className="bg-teal-50 -mx-6 -mt-6 px-6 py-4 rounded-t-lg border-b border-teal-100">
          <div className="flex items-center gap-2 mb-1">
            <Clock className="h-5 w-5 text-teal-600" />
            <DialogTitle className="text-xl">Request Leave</DialogTitle>
          </div>
          <DialogDescription className="text-gray-600">
            Fill in the details below to submit your leave request.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">
            <FormField
              control={form.control}
              name="leaveType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Leave Type</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger className="border-teal-200 focus:ring-teal-500">
                        <SelectValue placeholder="Select leave type" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {leaveTypes.map((type) => (
                        <SelectItem key={type.value} value={type.value}>
                          {type.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="startDate"
                render={({ field }) => (
                  <FormItem className="flex flex-col">
                    <FormLabel>Start Date</FormLabel>
                    <Popover>
                      <PopoverTrigger asChild>
                        <FormControl>
                          <Button
                            variant="outline"
                            className={cn(
                              "w-full pl-3 text-left font-normal border-teal-200",
                              !field.value && "text-muted-foreground"
                            )}
                          >
                            {field.value ? (
                              format(field.value, "PPP")
                            ) : (
                              <span>Pick a date</span>
                            )}
                            <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                          </Button>
                        </FormControl>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0" align="start">
                        <Calendar
                          mode="single"
                          selected={field.value}
                          onSelect={field.onChange}
                          disabled={(date) => {
                            // Disable dates in the past and weekends
                            const isPastDate = date < new Date();
                            const isWeekendDate = isWeekend(date);
                            return isPastDate || isWeekendDate;
                          }}
                          initialFocus
                          className={cn("p-3 pointer-events-auto")}
                        />
                      </PopoverContent>
                    </Popover>
                    <FormDescription className="text-xs text-muted-foreground">
                      Weekends cannot be selected
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="endDate"
                render={({ field }) => (
                  <FormItem className="flex flex-col">
                    <FormLabel>End Date</FormLabel>
                    <Popover>
                      <PopoverTrigger asChild>
                        <FormControl>
                          <Button
                            variant="outline"
                            className={cn(
                              "w-full pl-3 text-left font-normal border-teal-200",
                              !field.value && "text-muted-foreground"
                            )}
                          >
                            {field.value ? (
                              format(field.value, "PPP")
                            ) : (
                              <span>Pick a date</span>
                            )}
                            <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                          </Button>
                        </FormControl>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0" align="start">
                        <Calendar
                          mode="single"
                          selected={field.value}
                          onSelect={field.onChange}
                          disabled={(date) => {
                            // Disable dates in the past, before start date, and weekends
                            const isPastDate = date < new Date();
                            const isBeforeStart = form.watch("startDate") ? date < form.watch("startDate") : false;
                            const isWeekendDate = isWeekend(date);
                            return isPastDate || isBeforeStart || isWeekendDate;
                          }}
                          initialFocus
                          className={cn("p-3 pointer-events-auto")}
                        />
                      </PopoverContent>
                    </Popover>
                    <FormDescription className="text-xs text-muted-foreground">
                      Weekends cannot be selected
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="halfDayStart"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md border p-4 border-teal-100">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                    <div className="space-y-1 leading-none">
                      <FormLabel>Half Day Start</FormLabel>
                      <FormDescription>
                        Take only half day at the start
                      </FormDescription>
                    </div>
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="halfDayEnd"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md border p-4 border-teal-100">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                    <div className="space-y-1 leading-none">
                      <FormLabel>Half Day End</FormLabel>
                      <FormDescription>
                        Take only half day at the end
                      </FormDescription>
                    </div>
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="reason"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{isReasonRequired ? "Reason (Required)" : "Reason (Optional)"}</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Please provide details for your leave request"
                      className="resize-none border-teal-200"
                      {...field}
                      required={isReasonRequired}
                    />
                  </FormControl>
                  <FormDescription>
                    {currentLeaveType === 'sick' && "Medical certificate may be required for sick leave of 3 or more days."}
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormItem className="space-y-2">
              <FormLabel>
                {isDocumentRequired
                  ? "Supporting Documents (Required)"
                  : "Supporting Documents (Optional)"}
              </FormLabel>
              <div className="flex items-center gap-2">
                <FormControl>
                  <Input
                    type="file"
                    className="hidden"
                    id="file-upload"
                    onChange={handleFileChange}
                    required={isDocumentRequired}
                    disabled={isUploading}
                    accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
                  />
                </FormControl>
                <label
                  htmlFor="file-upload"
                  className={cn(
                    "cursor-pointer flex items-center gap-2 px-4 py-2 border rounded-md",
                    isDocumentRequired && !uploadedFile
                      ? "bg-red-50 text-red-600 hover:bg-red-100 border-red-200"
                      : isUploading
                        ? "bg-gray-100 text-gray-500 border-gray-200 cursor-not-allowed"
                        : "bg-teal-50 text-teal-600 hover:bg-teal-100 border-teal-200"
                  )}
                >
                  {isUploading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      <span>Uploading... {uploadProgress}%</span>
                    </>
                  ) : (
                    <>
                      <Upload className="h-4 w-4" />
                      <span>{isDocumentRequired ? "Upload Document (Required)" : "Upload Document"}</span>
                    </>
                  )}
                </label>
                {uploadedFile && !isUploading && (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <File className="h-4 w-4" />
                    <span className="truncate max-w-[150px]">{uploadedFile.name}</span>
                  </div>
                )}
              </div>

              {isUploading && (
                <div className="w-full bg-gray-200 rounded-full h-2.5 mt-2">
                  <div
                    className="bg-teal-600 h-2.5 rounded-full"
                    style={{ width: `${uploadProgress}%` }}
                  ></div>
                </div>
              )}

              {fileUrl && (
                <div className="flex items-center gap-2 mt-2 p-2 bg-teal-50 rounded border border-teal-100">
                  <LinkIcon className="h-4 w-4 text-teal-600" />
                  <a
                    href={fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm text-teal-600 hover:text-teal-700 hover:underline truncate"
                  >
                    View uploaded document
                  </a>
                </div>
              )}

              <FormDescription>
                {isDocumentRequired
                  ? "A supporting document is required for this leave type (PDF, JPEG, PNG files)."
                  : "Upload supporting documentation if applicable (PDF, JPEG, PNG files)."}
              </FormDescription>
              {isDocumentRequired && !uploadedFile && (
                <p className="text-sm font-medium text-red-500">Supporting document is required</p>
              )}
            </FormItem>

            <DialogFooter className="mt-6 pt-4 border-t border-gray-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isSubmitting || isUploading}
                className="border-gray-200"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || isUploading}
                className="bg-teal-600 hover:bg-teal-700"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Submitting
                  </>
                ) : (
                  "Submit Request"
                )}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};

export default LeaveRequestDialog;
