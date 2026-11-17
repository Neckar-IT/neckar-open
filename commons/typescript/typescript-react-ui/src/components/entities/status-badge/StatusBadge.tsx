import { cva } from "class-variance-authority";
import { cn } from "cn";
import { LoaderCircleIcon } from "lucide-react";

const statusBadgeVariants = cva("[&_svg]:size-4", {
  variants: {
    tone: {
      muted: "border-muted-foreground/40 text-muted-foreground",
      info: "border-primary/60 text-primary",
      success: "border-state-success/60 text-state-success",
      warning: "border-state-warning/60 text-state-warning",
      danger: "border-state-error/60 text-state-error",
    },
    variant: {
      icon: "",
      "with-text": "inline-flex shrink-0 items-center gap-1 rounded-full border py-0.75 pr-2.5 pl-2 text-xs whitespace-nowrap",
    },
    loading: {
      true: "opacity-60",
      false: "",
    },
  },
  defaultVariants: {
    variant: "icon",
    loading: false,
  },
});

export type StatusBadgeTone = "muted" | "info" | "success" | "warning" | "danger";

export type StatusBadgeVariant = "icon" | "with-text";

interface StatusBadgeProps {
  tone: StatusBadgeTone;
  icon: React.ReactNode;
  label: string;
  variant?: StatusBadgeVariant;
  loading?: boolean;
  className?: string;
  "data-testid"?: string;
}

/**
 * Badge showing one status of an entity as a toned icon, with the label as text in the `with-text` variant and as tooltip in both;
 * `loading` swaps the icon for a spinner while the status changes.
 */
export function StatusBadge({
  tone,
  icon,
  label,
  variant = "icon",
  loading = false,
  className,
  "data-testid": dataTestId,
}: StatusBadgeProps): React.ReactElement {
  return (
    <div className={cn(statusBadgeVariants({ tone, variant, loading }), className)} title={label} data-testid={dataTestId}>
      {loading ? <LoaderCircleIcon className="animate-spin" /> : icon}
      {variant === "with-text" ? <span>{label}</span> : null}
    </div>
  );
}
