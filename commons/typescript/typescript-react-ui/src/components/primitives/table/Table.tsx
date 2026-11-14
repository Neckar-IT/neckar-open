/**
 * Scrollable HTML table wrapper; compose with TableHeader, TableBody, TableRow, and TableCell.
 */

import {
  TableCell as ShadcnTableCell,
  TableRow as ShadcnTableRow,
  TableHead as ShadcnTableHead,
} from "@neckar-hub/typescript-react-ui-primitives/ui/table";
import { cn } from "cn";

export { Table, TableBody, TableCaption, TableFooter, TableHeader } from "@neckar-hub/typescript-react-ui-primitives/ui/table";

/**
 * Table Head with muted text
 */
export function TableHead({ className, ...props }: React.ComponentProps<typeof ShadcnTableHead>): React.ReactElement {
  return <ShadcnTableHead className={cn("text-muted-foreground", className)} {...props} />;
}

/**
 * Row with a full `bg-muted` hover: the light `muted` token (`hsl(210deg 40% 96.1%)`) at 50 % on white leaves the hover almost invisible.
 */
export function TableRow({ className, ...props }: React.ComponentProps<typeof ShadcnTableRow>): React.ReactElement {
  return <ShadcnTableRow className={cn("hover:bg-muted", className)} {...props} />;
}

/**
 * Cell with a fixed height of `h-14`, so rows keep one height whether a cell holds text, a badge, or a button.
 */
export function TableCell({ className, ...props }: React.ComponentProps<typeof ShadcnTableCell>): React.ReactElement {
  return <ShadcnTableCell className={cn("h-14 px-4", className)} {...props} />;
}
