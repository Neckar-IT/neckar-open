import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { ReactNode } from "react";
import { NoValue } from "./NoValue";

interface RenderIfValueDefinedProps<T> {
  /**
   * Children are rendered only if this value is defined.
   */
  value: T;
  /**
   * Render prop receiving the defined value; should return the UI to display.
   */
  children: (value: NonNullable<T>) => ReactNode;
  /**
   * Optional fallback to render if the value is not defined; defaults to NoValue.
   */
  fallback?: ReactNode;
}

/**
 * Renders `children(value)` if and only if `value` passes the `isDefined` guard.
 */
export function RenderIfValueDefined<T>(props: RenderIfValueDefinedProps<T>): ReactNode {
  if (!isDefined(props.value)) {
    return props.fallback ?? <NoValue />;
  }

  return props.children(props.value);
}
