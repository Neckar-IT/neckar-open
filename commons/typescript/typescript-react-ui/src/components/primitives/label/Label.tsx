/**
 * Accessible form label that automatically links to its peer input and dims when the input is disabled.
 */

import { Label as ShadcnLabel } from "@neckar-hub/typescript-react-ui-primitives/ui/label";
import { cn } from "cn";

// Label with `leading-snug` in place of `leading-none`, on which the lines of a wrapped label touch each other.
export function Label({ className, ...props }: React.ComponentProps<typeof ShadcnLabel>): React.ReactElement {
  return <ShadcnLabel className={cn("leading-snug", className)} {...props} />;
}
