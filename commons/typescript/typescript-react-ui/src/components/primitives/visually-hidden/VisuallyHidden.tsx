/**
 * Imports
 */

import { VisuallyHidden as VisuallyHiddenPrimitive } from "radix-ui";
import type { PropsWithChildren, ReactElement } from "react";

/**
 * Children will only be visible to screen readers
 */

type VisuallyHiddenProps = PropsWithChildren;

/**
 * Hides content visually while keeping it available to screen readers.
 */
export function VisuallyHidden(props: VisuallyHiddenProps): ReactElement {
  return <VisuallyHiddenPrimitive.Root>{props.children}</VisuallyHiddenPrimitive.Root>;
}
