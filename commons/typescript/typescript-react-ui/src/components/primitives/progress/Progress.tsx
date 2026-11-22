import { Progress as ShadcnProgress } from "@neckar-hub/typescript-react-ui-primitives/ui/progress";
import { cn } from "cn";
import type { ComponentProps } from "react";

export interface ProgressProps extends Omit<ComponentProps<typeof ShadcnProgress>, "value"> {
  /** The filled part from 0 to 100, or `"indeterminate"` while the progress is unknown. */
  value: number | "indeterminate";
  /** Shown above the bar on the left and used as its accessible name; the percentage appears opposite. */
  label?: string | undefined;
}

/**
 * Horizontal progress bar. A number from 0 to 100 fills the shadcn progress from left to right; `"indeterminate"`
 * shows two bars sweeping across the track in the style of Material UI, at the size and in the colors of the shadcn
 * progress. An optional label stands above the bar on the left, the percentage on the right (determinate only).
 */
export function Progress({ value, label, className, "aria-label": ariaLabel, ...props }: ProgressProps): React.ReactElement {
  const accessibleName = ariaLabel ?? label;
  const percent = value === "indeterminate" ? null : Math.round(value);

  return (
    <div className={cn("flex w-full flex-col gap-1.5", className)}>
      {label != null && (
        <div className="flex items-baseline justify-between gap-3 text-sm">
          <span>{label}</span>
          {percent != null && <span className="font-mono">{percent} %</span>}
        </div>
      )}
      {value === "indeterminate" ? (
        <div
          role="progressbar"
          aria-label={accessibleName}
          aria-busy={true}
          data-slot="progress"
          data-state="indeterminate"
          {...props}
          className="bg-muted relative h-1.5 w-full overflow-hidden rounded-full"
        >
          <span className="bg-primary animate-progress-indeterminate-primary absolute inset-y-0" />
          <span className="bg-primary animate-progress-indeterminate-secondary absolute inset-y-0" />
        </div>
      ) : (
        // The shadcn progress does not pass `value` on to the Radix root, so `aria-valuenow` is set here.
        <ShadcnProgress value={value} aria-valuenow={percent ?? undefined} aria-label={accessibleName} {...props} />
      )}
    </div>
  );
}
