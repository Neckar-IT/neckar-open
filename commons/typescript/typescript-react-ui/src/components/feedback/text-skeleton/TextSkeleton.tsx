import { cn } from "cn";

interface TextSkeletonProps {
  className?: string;
}

/**
 * Loading-state placeholder for text-content slots (entity titles, names, fields).
 *
 * Renders an inline `<span data-text-content>` so it composes inside the same parents
 * as the real text (breadcrumbs, table cells, headlines) and participates in the
 * `TruncateTextContent` CSS contract just like the loaded value would.
 */
export function TextSkeleton({ className }: TextSkeletonProps): React.ReactElement {
  return <span data-text-content className={cn("bg-muted inline-block h-4 w-24 animate-pulse rounded-md align-middle", className)} />;
}
