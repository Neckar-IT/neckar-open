import { cn } from "cn";
import { Slot } from "radix-ui";

interface ListEntryCardProps extends React.ComponentProps<"div"> {
  /** Renders the card as its single child — pass a link to make the whole entry clickable. */
  asChild?: boolean;
}

/**
 * One entry of a bordered list: a framed, padded block stacking its content vertically.
 *
 * The single place that decides how an entry in a scannable list is framed — used for read-only rows
 * (a team member, a transcript) as well as for entries carrying actions (a running meeting). Entries
 * sit inside a [ListEntryStack], which owns the distance between them.
 */
export function ListEntryCard({ className, asChild = false, ...props }: ListEntryCardProps): React.ReactElement {
  const Component = asChild ? Slot.Root : "div";

  return <Component className={cn("flex flex-col gap-1 rounded-md border p-4", className)} {...props} />;
}
