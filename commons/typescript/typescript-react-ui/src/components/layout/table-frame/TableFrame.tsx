import { cn } from "cn";

/**
 * The frame around a table's rows: the single place that decides how a table is set off from the
 * page.
 *
 * [DataSliceView] draws it around its content, so a table with filters and pagination gets it from
 * there. A plain table — an invoice, a summary with no controls above it — draws it itself and
 * comes out looking like the others.
 */
export function TableFrame({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  return <div className={cn("rounded-md border", className)} {...props} />;
}
