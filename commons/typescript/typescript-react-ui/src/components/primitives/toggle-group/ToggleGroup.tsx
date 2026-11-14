/**
 * Group of connected toggle buttons where one or more items can be pressed simultaneously.
 */

import { ToggleGroup as ShadcnToggleGroup } from "@neckar-hub/typescript-react-ui-primitives/ui/toggle-group";

export { ToggleGroupItem } from "@neckar-hub/typescript-react-ui-primitives/ui/toggle-group";

// Group with `spacing={0}` as default: the items join into one control with a rounded first and last item, the primitive's
// default `2` spaces them apart.
export function ToggleGroup({ spacing = 0, ...props }: React.ComponentProps<typeof ShadcnToggleGroup>): React.ReactElement {
  return <ShadcnToggleGroup spacing={spacing} {...props} />;
}
