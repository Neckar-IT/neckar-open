/**
 * shadcn Toast Component (based on sonner) (https://ui.shadcn.com/docs/components/sonner)
 *
 * The base component was slightly modified to simplify the usage.
 */

/**
 * Imports
 */

import { AlertCircleIcon } from "lucide-react";
import { Toaster } from "sonner";

/**
 * Toast Provider (to be placed at the root of your app)
 */

type ToasterProps = React.ComponentProps<typeof Toaster>;

const ToastProvider = ({ ...props }: ToasterProps) => {
  return (
    <Toaster
      className="toaster group"
      theme={props.theme}
      icons={{
        error: <AlertCircleIcon className="size-5" />,
      }}
      toastOptions={{
        classNames: {
          toast:
            "group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg",
          // utilities need to be marked as important (!) to overwrite the default sonner styles
          description: "group-[.toast]:!text-muted-foreground group-data-[type=error]:!text-destructive-foreground/90",
          actionButton: "group-[.toast]:!bg-primary group-[.toast]:!text-primary-foreground",
          cancelButton: "group-[.toast]:!bg-muted group-[.toast]:!text-muted-foreground",
          error: "group-[.toaster]:!bg-destructive group-[.toaster]:!text-destructive-foreground",
        },
      }}
      {...props}
    />
  );
};

/**
 * Export
 */

export { ToastProvider };
