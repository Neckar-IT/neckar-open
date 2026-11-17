import type { ComponentType, ReactElement } from "react";
import type { CustomIconProps } from "../../media/custom-icons";
import { SidebarMenuButton, SidebarMenuItem } from "../../primitives/sidebar";
import { SidebarNavMenuButtonClasses, SidebarNavMenuIconClasses } from "./sidebarNavMenuItemClasses";

export type SidebarNavExternalLinkItemProps = {
  title: string;
  /** Absolute URL of the target, e.g. `https://admin.patterns.neckar.it/`. */
  href: string;
  icon?: ComponentType<CustomIconProps>;
};

/**
 * A sidebar nav item pointing at another application.
 *
 * The router-typed items from `createSidebarNavMenuItem` cover routes inside the app and cannot
 * express a target outside it. This one takes a plain URL and therefore has no active state: the
 * current location can never match a different origin.
 *
 * Opens in a new tab, so the user does not lose the state of the app they came from. Like the
 * router-typed items it is i18n-agnostic — callers pass a pre-translated `title`.
 */
export function SidebarNavExternalLinkItem({ title, href, icon: Icon }: SidebarNavExternalLinkItemProps): ReactElement {
  return (
    <SidebarMenuItem>
      <SidebarMenuButton asChild className={SidebarNavMenuButtonClasses}>
        <a href={href} rel="noopener noreferrer" target="_blank">
          {Icon !== undefined && <Icon className={SidebarNavMenuIconClasses} />}
          <span>{title}</span>
        </a>
      </SidebarMenuButton>
    </SidebarMenuItem>
  );
}
