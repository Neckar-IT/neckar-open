import type { LinkPropsPrimitive, NoParams } from "@neckar-hub/typescript-react-router";
import type { ReactElement } from "react";
import { SidebarMenuSubButton, SidebarMenuSubItem } from "../../primitives/sidebar";
import { createUseIsActive, type NavItemDeps } from "../createUseIsActive";
import { sidebarNavItemAriaCurrent } from "./sidebarNavItemAriaCurrent";
import { SidebarNavMenuDisabledClasses } from "./sidebarNavMenuItemClasses";

export type SidebarNavMenuSubItemProps<AppRoutes extends Record<string, Record<string, string> | NoParams>> =
  LinkPropsPrimitive<AppRoutes> & {
    title: string;
    end?: boolean;
    disabled?: boolean;
    /**
     * Overrides the location-derived active state. Use for items whose active
     * paths cannot be expressed as a single URL prefix (NavLink-style override).
     */
    isActive?: boolean;
    /** Derived from the location and the override, see `sidebarNavItemAriaCurrent`. */
    "aria-current"?: never;
  };

/**
 * Factory for typed sidebar nav-menu sub-items bound to a project's `AppRoutes`.
 *
 * Returns `SidebarNavMenuSubItem`, whose active state is computed from the
 * current location. Sub-items render no icon. The shared component is
 * i18n-agnostic — callers pass a pre-translated `title` string.
 *
 * Top-level items live in their own factory: see `createSidebarNavMenuItem`.
 */
export function createSidebarNavMenuSubItem<AppRoutes extends Record<string, Record<string, string> | NoParams>>({
  Link,
  generateUrl,
}: NavItemDeps<AppRoutes>) {
  const useIsActive = createUseIsActive(generateUrl);

  function SidebarNavMenuSubItem({
    title,
    end = false,
    disabled = false,
    isActive: isActiveOverride,
    ...rest
  }: SidebarNavMenuSubItemProps<AppRoutes>): ReactElement {
    const link = rest as LinkPropsPrimitive<AppRoutes>;
    const locationIsActive = useIsActive(link, end);
    const isActive = isActiveOverride ?? locationIsActive;

    if (disabled) {
      return (
        <SidebarMenuSubItem>
          <SidebarMenuSubButton className={SidebarNavMenuDisabledClasses} isActive={isActive}>
            <span>{title}</span>
          </SidebarMenuSubButton>
        </SidebarMenuSubItem>
      );
    }

    return (
      <SidebarMenuSubItem>
        <SidebarMenuSubButton asChild isActive={isActive}>
          <Link {...link} aria-current={sidebarNavItemAriaCurrent(locationIsActive, isActive)}>
            <span>{title}</span>
          </Link>
        </SidebarMenuSubButton>
      </SidebarMenuSubItem>
    );
  }

  return SidebarNavMenuSubItem;
}
