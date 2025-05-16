import React from "react";
import { Card, CardContent, CardHeader, CardProps } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export type CardVariant =
    | "default"
    | "primary"    // Blue - for primary information
    | "secondary"  // Purple - for secondary information
    | "accent"     // Teal - for highlighted information
    | "muted"      // Gray - for less important information
    | "info";      // Light blue - for informational content

interface StyledCardProps extends React.ComponentPropsWithoutRef<typeof Card> {
    variant?: CardVariant;
}

export const StyledCard = React.forwardRef<
    HTMLDivElement,
    StyledCardProps
>(({ className, variant = "default", children, ...props }, ref) => {
    // Define styling based on variant
    const getVariantStyles = () => {
        switch (variant) {
            case "primary":
                return "bg-blue-50 border-blue-200";
            case "secondary":
                return "bg-purple-50 border-purple-200";
            case "accent":
                return "bg-teal-50 border-teal-200";
            case "muted":
                return "bg-gray-50 border-gray-200";
            case "info":
                return "bg-sky-50 border-sky-200";
            default:
                return "bg-white border-border";
        }
    };

    return (
        <Card
            ref={ref}
            className={cn(
                "border shadow-sm",
                getVariantStyles(),
                className
            )}
            {...props}
        >
            {children}
        </Card>
    );
});

StyledCard.displayName = "StyledCard";

// Styled variants of CardHeader and CardContent
export const StyledCardHeader = React.forwardRef<
    HTMLDivElement,
    React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
    <CardHeader
        ref={ref}
        className={cn("pb-3", className)}
        {...props}
    />
));

StyledCardHeader.displayName = "StyledCardHeader";

export const StyledCardContent = React.forwardRef<
    HTMLDivElement,
    React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
    <CardContent
        ref={ref}
        className={cn("space-y-4", className)}
        {...props}
    />
));

StyledCardContent.displayName = "StyledCardContent"; 