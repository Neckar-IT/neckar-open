/**
 * Short contextual label that appears near a trigger element on hover or focus.
 */

import { TooltipTrigger as ShadcnTooltipTrigger } from "@neckar-hub/typescript-react-ui-primitives/ui/tooltip";

export { Tooltip, TooltipContent, TooltipProvider } from "@neckar-hub/typescript-react-ui-primitives/ui/tooltip";

/**
 * Trigger outside the tab order: a dialog focuses its first focusable element on open, and a focused trigger opens its tooltip
 * over the dialog. With `asChild` the child element leaves the tab order as well; a caller passes `tabIndex={0}` to keep it.
 */
export function TooltipTrigger({ tabIndex = -1, ...props }: React.ComponentProps<typeof ShadcnTooltipTrigger>): React.ReactElement {
  return <ShadcnTooltipTrigger tabIndex={tabIndex} {...props} />;
}
