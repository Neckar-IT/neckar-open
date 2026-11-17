import { cn } from "cn";

/**
 * Row of short context values belonging to one entity — status, type, timestamps, counts.
 *
 * Wraps onto the next line on narrow screens instead of overflowing, with a tighter gap between the
 * wrapped lines than between the values themselves.
 */
export function MetaRow({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  return <div className={cn("flex flex-wrap items-center gap-x-3 gap-y-1", className)} {...props} />;
}
