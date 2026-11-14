import { type LinkPropsPrimitive, type NoParams, useLocation } from "@neckar-hub/typescript-react-router";
import type { ComponentType } from "react";

export interface SidebarNavItemDeps<AppRoutes extends Record<string, Record<string, string> | NoParams>> {
  Link: ComponentType<LinkPropsPrimitive<AppRoutes>>;
  generateUrl: (link: LinkPropsPrimitive<AppRoutes>) => string;
}

/**
 * Builds the active-state hook for a sidebar nav item bound to `AppRoutes`.
 *
 * The item is active when the path starts with the resolved target URL
 * (NavLink-style); pass `end={true}` to require an exact match instead.
 */
export function createUseIsActive<AppRoutes extends Record<string, Record<string, string> | NoParams>>(
  generateUrl: (link: LinkPropsPrimitive<AppRoutes>) => string,
) {
  return function useIsActive(link: LinkPropsPrimitive<AppRoutes>, end: boolean): boolean {
    const { pathname } = useLocation();
    const target = generateUrl(link);

    if (end || target === "/") {
      return pathname === target;
    }

    //Match the target itself or a sub-path, but require a "/" boundary so a sibling that merely
    //shares a string prefix (e.g. "/settings-archive" vs "/settings") is not treated as active.
    return pathname === target || pathname.startsWith(`${target}/`);
  };
}
