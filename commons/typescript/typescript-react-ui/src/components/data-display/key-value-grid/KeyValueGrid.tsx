import { cn } from "cn";

/**
 * Compact two-column key/value grid (`<dl>`) — labels sized to their content, values fill the rest.
 *
 * The muted, small-print style is for secondary summaries, e.g. recapping the affected object
 * inside a confirmation dialog. Children are plain `<dt>`/`<dd>` pairs. For the full-width
 * label/value list on detail pages use [DescriptionList] instead.
 */
export function KeyValueGrid({ className, ...props }: React.ComponentProps<"dl">): React.ReactElement {
  return <dl className={cn("text-muted-foreground grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1 text-sm", className)} {...props} />;
}
