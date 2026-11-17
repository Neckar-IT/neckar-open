import { type LinkPropsPrimitive, type NoParams, useLocation } from "@neckar-hub/typescript-react-router";
import type { ComponentType } from "react";

export interface NavItemDeps<AppRoutes extends Record<string, Record<string, string> | NoParams>> {
  Link: ComponentType<LinkPropsPrimitive<AppRoutes>>;
  generateUrl: (link: LinkPropsPrimitive<AppRoutes>) => string;
}

/**
 * Builds the active-state hook for a nav item bound to `AppRoutes`; sidebar and header nav items share it.
 *
 * The item is active on its target path and on every sub-path that continues it after a "/"; the root target `/` and
 * `end = true` require the exact path. Search params of the target take no part.
 */
export function createUseIsActive<AppRoutes extends Record<string, Record<string, string> | NoParams>>(
  generateUrl: (link: LinkPropsPrimitive<AppRoutes>) => string,
) {
  return function useIsActive(link: LinkPropsPrimitive<AppRoutes>, end: boolean): boolean {
    const { pathname } = useLocation();
    const target = generateUrl({ ...link, searchParams: undefined });

    if (end || target === "/") {
      return pathname === target;
    }

    // The "/" boundary keeps a sibling that shares a string prefix ("/settings-archive" vs "/settings") inactive.
    return pathname === target || pathname.startsWith(`${target}/`);
  };
}
