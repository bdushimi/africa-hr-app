import { useTheme } from "next-themes"
import { Toaster as Sonner, toast as sonnerToast } from "sonner"

type ToasterProps = React.ComponentProps<typeof Sonner>

const Toaster = ({ ...props }: ToasterProps) => {
  const { theme = "system" } = useTheme()

  return (
    <Sonner
      theme={theme as ToasterProps["theme"]}
      className="toaster group"
      toastOptions={{
        classNames: {
          toast:
            "group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg",
          description: "group-[.toast]:text-muted-foreground",
          actionButton:
            "group-[.toast]:bg-primary group-[.toast]:text-primary-foreground",
          cancelButton:
            "group-[.toast]:bg-muted group-[.toast]:text-muted-foreground",
          error:
            "group-[.toaster]:bg-red-50 group-[.toaster]:border-red-300 group-[.toaster]:text-red-800",
          success:
            "group-[.toaster]:bg-green-50 group-[.toaster]:border-green-200 group-[.toaster]:text-green-800",
          info:
            "group-[.toaster]:bg-blue-50 group-[.toaster]:border-blue-200 group-[.toaster]:text-blue-800",
        },
      }}
      duration={4000}
      position="top-center"
      expand={true}
      {...props}
    />
  )
}

const toast = {
  ...sonnerToast,
  error: (message: string, options?: any) => {
    return sonnerToast.error(message, {
      duration: 6000,
      className: "font-medium border-2",
      position: "top-center",
      ...options,
    });
  },
  success: (message: string, options?: any) => {
    return sonnerToast.success(message, {
      duration: 3000,
      className: "font-medium",
      position: "top-center",
      ...options,
    });
  },
  info: (message: string, options?: any) => {
    return sonnerToast.info(message, {
      duration: 4000,
      className: "font-medium border-blue-200",
      position: "top-center",
      ...options,
    });
  },
  warning: (message: string, options?: any) => {
    return sonnerToast.warning(message, {
      duration: 5000,
      className: "font-medium border-yellow-200 bg-yellow-50 text-yellow-800",
      position: "top-center",
      ...options,
    });
  },
  // Override the default toast method as well
  default: (message: string, options?: any) => {
    return sonnerToast(message, {
      duration: 4000,
      position: "top-center",
      ...options,
    });
  },
  // Make sure to override the normal function call
  __call: (message: string, options?: any) => {
    return sonnerToast(message, {
      duration: 4000,
      position: "top-center",
      ...options,
    });
  }
}

// Override the function call behavior
const enhancedToast = Object.assign(
  (message: string, options?: any) => toast.default(message, options),
  toast
);

export { Toaster, enhancedToast as toast }
