/**
 * Imports
 */

import { cn } from "cn";
import { Link as RouterLink } from "react-router";
import type { LinkPropsPrimitive, NoParams } from "./types";
import { buildUrl } from "./utils";

/**
 * Factory function to create a typed link component (requires AppRoutes generic type)
 *
 * Example Usage:
 * ```
 * interface AppRoutes {
 *   "/": NoParams;
 *   "/chickens": NoParams;
 *   "/chickens/:chickenId": { chickenId: string };
 * }
 *
 * const Link = createLink<AppRoutes>()
 * ```
 */

export function createLink<AppRoutes extends Record<string, Record<string, string> | NoParams>>() {
  return function Link(props: LinkPropsPrimitive<AppRoutes>) {
    const url = buildUrl(props);

    // Note: we pull `params` and `searchParams` out, so they won't be forwarded accidentally
    const { params: _params, searchParams: _searchParams, className, ...rest } = props;
    return <RouterLink {...rest} to={url} className={cn("group/link min-w-0 **:group-hover/link:underline hover:underline", className)} />;
  };
}

/**
 * Factory that returns a function to build strongly typed LinkProps objects.
 *
 * Usage:
 * interface AppRoutes {
 *   "/": NoParams;
 *   "/chickens": NoParams;
 *   "/chickens/:chickenId": { chickenId: string };
 * }
 *
 * const generateLinkProps = createLinkPropsFactory<AppRoutes>();
 */
export function createLinkPropsGenerator<AppRoutes extends Record<string, Record<string, string> | NoParams>>() {
  return function build<T extends keyof AppRoutes>(
    to: T,
    ...paramsArg: AppRoutes[T] extends NoParams ? [] : [AppRoutes[T]]
  ): LinkPropsPrimitive<AppRoutes, T> {
    if (paramsArg.length === 0) {
      return { to } as LinkPropsPrimitive<AppRoutes, T>;
    }
    return { to, params: paramsArg[0] } as LinkPropsPrimitive<AppRoutes, T>;
  };
}
