import type { LinkPropsPrimitive, NoParams } from "@neckar-hub/typescript-react-router";
import { cn } from "cn";
import type { ComponentType, ReactElement } from "react";
import type { CustomIconProps } from "../../media/custom-icons";
import { SidebarMenuButton, SidebarMenuItem } from "../../primitives/sidebar";
import { createUseIsActive, type NavItemDeps } from "../createUseIsActive";
import { sidebarNavItemAriaCurrent } from "./sidebarNavItemAriaCurrent";
import { SidebarNavMenuButtonClasses, SidebarNavMenuDisabledClasses, SidebarNavMenuIconClasses } from "./sidebarNavMenuItemClasses";

export type SidebarNavMenuItemProps<AppRoutes extends Record<string, Record<string, string> | NoParams>> = LinkPropsPrimitive<AppRoutes> & {
  title: string;
  icon?: ComponentType<CustomIconProps>;
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
 * Factory for typed sidebar nav-menu items bound to a project's `AppRoutes`.
 *
 * Returns `SidebarNavMenuItem`, whose active state is computed from the current
 * location. The shared component is i18n-agnostic — callers pass a
 * pre-translated `title` string.
 *
 * Sub-items live in their own factory: see `createSidebarNavMenuSubItem`.
 */
export function createSidebarNavMenuItem<AppRoutes extends Record<string, Record<string, string> | NoParams>>({
  Link,
  generateUrl,
}: NavItemDeps<AppRoutes>) {
  const useIsActive = createUseIsActive(generateUrl);

  function SidebarNavMenuItem({
    title,
    icon: Icon,
    end = false,
    disabled = false,
    isActive: isActiveOverride,
    ...rest
  }: SidebarNavMenuItemProps<AppRoutes>): ReactElement {
    const link = rest as LinkPropsPrimitive<AppRoutes>;
    const locationIsActive = useIsActive(link, end);
    const isActive = isActiveOverride ?? locationIsActive;

    if (disabled) {
      return (
        <SidebarMenuItem>
          <SidebarMenuButton className={cn(SidebarNavMenuButtonClasses, SidebarNavMenuDisabledClasses)} isActive={isActive}>
            {Icon !== undefined && <Icon className={SidebarNavMenuIconClasses} />}
            <span>{title}</span>
          </SidebarMenuButton>
        </SidebarMenuItem>
      );
    }

    return (
      <SidebarMenuItem>
        <SidebarMenuButton asChild className={SidebarNavMenuButtonClasses} isActive={isActive}>
          <Link {...link} aria-current={sidebarNavItemAriaCurrent(locationIsActive, isActive)}>
            {Icon !== undefined && <Icon className={SidebarNavMenuIconClasses} />}
            <span>{title}</span>
          </Link>
        </SidebarMenuButton>
      </SidebarMenuItem>
    );
  }

  return SidebarNavMenuItem;
}
