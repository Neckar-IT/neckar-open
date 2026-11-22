import { ChevronRightIcon } from "lucide-react";
import { type ComponentType, type ReactElement, type ReactNode, useState } from "react";
import type { CustomIconProps } from "../../media/custom-icons";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "../../primitives/collapsible";
import { SidebarMenuButton, SidebarMenuItem, SidebarMenuSub } from "../../primitives/sidebar";
import { SidebarNavMenuButtonClasses, SidebarNavMenuIconClasses } from "./sidebarNavMenuItemClasses";

export type SidebarNavCategoryProps = {
  title: string;
  icon: ComponentType<CustomIconProps>;
  containsCurrentPage: boolean;
  /** The sub-items, each a `SidebarMenuSubItem`, usually the `SidebarNavMenuSubItem` of `createSidebarNavMenuSubItem`. */
  children: ReactNode;
};

/**
 * A sidebar nav item that opens and closes a list of sub-items on click.
 *
 * The sidebar stays mounted across navigation, so the category opens on every change of `containsCurrentPage` to
 * `true`, not only on mount. A change to `false` leaves the category as it is. Callers pass a pre-translated `title`.
 */
export function SidebarNavCategory({ title, icon: Icon, containsCurrentPage, children }: SidebarNavCategoryProps): ReactElement {
  const [open, setOpen] = useState(containsCurrentPage);
  const [previousContainsCurrentPage, setPreviousContainsCurrentPage] = useState(containsCurrentPage);
  // Adjusts the state during render, so the first paint after the navigation already shows the open category.
  if (containsCurrentPage !== previousContainsCurrentPage) {
    setPreviousContainsCurrentPage(containsCurrentPage);
    if (containsCurrentPage) {
      setOpen(true);
    }
  }

  return (
    <Collapsible asChild={true} open={open} onOpenChange={setOpen} className="group/collapsible">
      <SidebarMenuItem>
        <CollapsibleTrigger asChild={true}>
          <SidebarMenuButton className={SidebarNavMenuButtonClasses}>
            <Icon className={SidebarNavMenuIconClasses} />
            <span>{title}</span>
            <ChevronRightIcon className="ml-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90" />
          </SidebarMenuButton>
        </CollapsibleTrigger>
        <CollapsibleContent>
          <SidebarMenuSub>{children}</SidebarMenuSub>
        </CollapsibleContent>
      </SidebarMenuItem>
    </Collapsible>
  );
}
