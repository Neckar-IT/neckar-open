import { cn } from "cn";
import React from "react";
import { Button } from "../../primitives/button";

/**
 * Horizontal flex container for grouped action buttons, with ToolbarGroup and ToolbarButton sub-components.
 */

export function Toolbar({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex max-w-min items-center gap-2", className)} {...props} />;
}

export function ToolbarGroup({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex max-w-min divide-x overflow-hidden rounded-md border bg-white shadow-md", className)} {...props} />;
}

interface ToolbarButtonProps extends React.ComponentProps<"button"> {
  /** Accessible name and tooltip. Required because a toolbar button carries an icon and no visible text. */
  label: string;
  active?: boolean;
  variant?: "default" | "destructive";
}

export function ToolbarButton({ className, label, active, variant = "default", ...props }: ToolbarButtonProps) {
  return (
    <Button
      aria-label={label}
      className={cn(
        "bg-background text-muted-foreground disabled:hover:bg-background disabled:hover:text-muted-foreground flex h-12 w-12 items-center justify-center rounded-none p-0 disabled:opacity-50 [&_svg]:size-6",
        {
          "bg-primary/75 hover:bg-primary/75 disabled:hover:bg-primary/75 text-white hover:cursor-auto hover:text-white disabled:hover:text-white":
            active,
          "text-destructive hover:text-destructive disabled:hover:text-destructive": variant === "destructive",
        },
        className,
      )}
      customIconSize={true}
      title={label}
      variant="ghost"
      {...props}
    />
  );
}
