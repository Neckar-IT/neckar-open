import { cn } from "cn";

/**
 * Vertical stack of list entries — one place decides how far apart [ListEntryCard]s sit.
 */
export function ListEntryStack({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  return <div className={cn("flex flex-col gap-2", className)} {...props} />;
}
