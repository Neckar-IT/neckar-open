/**
 * Navigational landmark showing the current page's position within the site hierarchy.
 */

import {
  BreadcrumbItem as ShadcnBreadcrumbItem,
  BreadcrumbLink as ShadcnBreadcrumbLink,
  BreadcrumbList as ShadcnBreadcrumbList,
  BreadcrumbPage as ShadcnBreadcrumbPage,
  BreadcrumbSeparator as ShadcnBreadcrumbSeparator,
} from "@neckar-hub/typescript-react-ui-primitives/ui/breadcrumb";
import { cn } from "cn";

export { Breadcrumb, BreadcrumbEllipsis } from "@neckar-hub/typescript-react-ui-primitives/ui/breadcrumb";

/**
 * List on a single line: the breadcrumb sits in a fixed-height header, where a second line would overflow.
 * The items share one text baseline, so an item set in another font, such as `font-mono` for a commit, stays on the line.
 */
export function BreadcrumbList({ className, ...props }: React.ComponentProps<typeof ShadcnBreadcrumbList>): React.ReactElement {
  return <ShadcnBreadcrumbList className={cn("flex-nowrap items-baseline", className)} {...props} />;
}

/**
 * Item on a single line, for the same reason as `BreadcrumbList`, aligned on the text baseline like the list.
 */
export function BreadcrumbItem({ className, ...props }: React.ComponentProps<typeof ShadcnBreadcrumbItem>): React.ReactElement {
  return <ShadcnBreadcrumbItem className={cn("items-baseline text-nowrap", className)} {...props} />;
}

/**
 * Separator icon centred on the line: an icon has no text baseline.
 */
export function BreadcrumbSeparator({ className, ...props }: React.ComponentProps<typeof ShadcnBreadcrumbSeparator>): React.ReactElement {
  return <ShadcnBreadcrumbSeparator className={cn("self-center", className)} {...props} />;
}

/**
 * Link truncated at `max-w-48`, so a long entity name keeps the rest of the trail visible.
 */
export function BreadcrumbLink({ className, ...props }: React.ComponentProps<typeof ShadcnBreadcrumbLink>): React.ReactElement {
  return <ShadcnBreadcrumbLink className={cn("max-w-48 truncate", className)} {...props} />;
}

/**
 * Current page truncated at `max-w-48`, like `BreadcrumbLink`.
 */
export function BreadcrumbPage({ className, ...props }: React.ComponentProps<typeof ShadcnBreadcrumbPage>): React.ReactElement {
  return <ShadcnBreadcrumbPage className={cn("max-w-48 truncate", className)} {...props} />;
}
