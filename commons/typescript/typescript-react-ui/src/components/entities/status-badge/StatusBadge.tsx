import { cva } from "class-variance-authority";
import { cn } from "cn";
import { LoaderCircleIcon } from "lucide-react";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";

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
  /** What the status means and the facts behind it; shown on hover and keyboard focus in place of the label. */
  hint?: React.ReactNode;
  /** Opens this address in a new tab; without it the badge links nothing and stays usable inside a link. */
  href?: string | null | undefined;
  variant?: StatusBadgeVariant;
  loading?: boolean;
  className?: string | undefined;
  "data-testid"?: string;
}

/**
 * Badge showing one status of an entity as a toned icon, with the label as text in the `with-text` variant and as tooltip in both;
 * `loading` swaps the icon for a spinner while the status changes.
 *
 * With `hint` or `href` the tooltip is a focusable Radix tooltip, which needs a `TooltipProvider`; without both it is the native `title`.
 */
export function StatusBadge({
  tone,
  icon,
  label,
  hint,
  href,
  variant = "icon",
  loading = false,
  className,
  "data-testid": dataTestId,
}: StatusBadgeProps): React.ReactElement {
  const explained = hint != null || href != null;
  const badge = (
    <div
      data-slot="badge"
      className={cn(statusBadgeVariants({ tone, variant, loading }), className)}
      title={explained ? undefined : label}
      data-testid={dataTestId}
    >
      {loading ? <LoaderCircleIcon className="animate-spin" /> : icon}
      {variant === "with-text" ? <span>{label}</span> : null}
    </div>
  );

  if (!explained) {
    return badge;
  }

  return (
    <Tooltip>
      {/* The trigger is an element of its own: as the trigger, the badge would lose its data-slot, which the truncation zone reads. */}
      <TooltipTrigger asChild tabIndex={0}>
        {href == null ? (
          <span className="inline-flex shrink-0">{badge}</span>
        ) : (
          <a href={href} target="_blank" rel="noreferrer" aria-label={label} className="inline-flex shrink-0">
            {badge}
          </a>
        )}
      </TooltipTrigger>
      <TooltipContent className="max-w-[calc(100vw-2rem)]">{hint ?? label}</TooltipContent>
    </Tooltip>
  );
}
