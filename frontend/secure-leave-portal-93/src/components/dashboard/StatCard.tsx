import { Card, CardContent } from "@/components/ui/card";
import { ReactNode } from "react";
import { cn } from "@/lib/utils";

export type StatCardVariant =
  | "default"
  | "primary"    // Blue - for balance information
  | "secondary"  // Purple - for secondary info
  | "warning"    // Amber - for pending/waiting
  | "success"    // Green - for approved/positive
  | "destructive" // Red - for rejected/negative
  | "muted";     // Gray - for cancelled/inactive

type StatCardProps = {
  icon: ReactNode;
  title: string;
  value: string | number;
  description: string;
  formatDecimals?: boolean;
  variant?: StatCardVariant;
};

const StatCard = ({
  icon,
  title,
  value,
  description,
  formatDecimals = false,
  variant = "default"
}: StatCardProps) => {

  // Format the value to handle decimals if needed
  const formattedValue = formatDecimals && typeof value === 'number'
    ? value.toFixed(1)
    : value;

  // Define color variations based on variant prop
  const getVariantStyles = () => {
    switch (variant) {
      case "primary":
        return {
          icon: "text-blue-500",
          background: "bg-blue-50",
          border: "border-blue-100"
        };
      case "secondary":
        return {
          icon: "text-purple-500",
          background: "bg-purple-50",
          border: "border-purple-100"
        };
      case "warning":
        return {
          icon: "text-amber-500",
          background: "bg-amber-50",
          border: "border-amber-100"
        };
      case "success":
        return {
          icon: "text-green-500",
          background: "bg-green-50",
          border: "border-green-100"
        };
      case "destructive":
        return {
          icon: "text-red-500",
          background: "bg-red-50",
          border: "border-red-100"
        };
      case "muted":
        return {
          icon: "text-gray-500",
          background: "bg-gray-50",
          border: "border-gray-100"
        };
      default:
        return {
          icon: "text-slate-500",
          background: "",
          border: ""
        };
    }
  };

  const variantStyles = getVariantStyles();

  return (
    <Card className={cn("border", variantStyles.border)}>
      <CardContent className={cn("p-6", variantStyles.background)}>
        <div className="flex flex-col space-y-2">
          <div className="flex items-center space-x-2">
            <div className={cn("", variantStyles.icon)}>{icon}</div>
            <span className="text-sm font-medium">{title}</span>
          </div>
          <div className="text-3xl font-bold">{formattedValue}</div>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
      </CardContent>
    </Card>
  );
};

export default StatCard;
