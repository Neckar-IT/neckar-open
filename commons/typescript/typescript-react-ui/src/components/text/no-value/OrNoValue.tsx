import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { PropsWithChildren, ReactNode } from "react";
import { NoValue } from "./NoValue";

interface OrNoValueProps extends PropsWithChildren {
  className?: string;

  /**
   * The fallback value that is rendered, if the children are not defined.
   */
  fallback?: ReactNode;
}

/**
 * Shows children if they are defined, otherwise shows the fallback or a dash ("-")
 *
 * @example
 * <OrNoValue fallback="Arrgh">{children}</OrNoValue>
 * // is equivalent to
 * {isDefined(children) ? <>{children}</> : <NoValue fallback="Arrgh" />}
 */
export function OrNoValue(props: OrNoValueProps): ReactNode {
  if (isDefined(props.children)) {
    return props.children;
  } else {
    return <NoValue className={props.className}>{props.fallback}</NoValue>;
  }
}

type OrNoValueIfUndefinedProps = PropsWithChildren<{
  className?: string;
  /**
   * If defined {@link children} are rendered else {@link fallback} is rendered.
   */
  value: unknown;

  /**
   * Is rendered if {@link value} is undefined; leave undefined to get the default "-"
   */
  fallback?: ReactNode;
}>;

/**
 * Shows children if `value` is defined, otherwise shows the fallback or a dash ("-")
 *
 * @example
 * <OrNoValueIfUndefined value={value} fallback="Arrgh">{children}</OrNoValueIfUndefined>
 * // is equivalent to
 * {isDefined(value) ? <>{children}</> : <NoValue fallback="Arrgh" />}
 */
export function OrNoValueIfUndefined(props: OrNoValueIfUndefinedProps): ReactNode {
  return isDefined(props.value) ? props.children : <NoValue className={props.className}>{props.fallback}</NoValue>;
}
