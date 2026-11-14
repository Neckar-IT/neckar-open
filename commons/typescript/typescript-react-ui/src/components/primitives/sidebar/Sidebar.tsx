/**
 * Collapsible navigation panel that adapts to a slide-in Sheet on mobile; wrap in SidebarProvider.
 */

import { SidebarProvider as ShadcnSidebarProvider } from "@neckar-hub/typescript-react-ui-primitives/ui/sidebar";
import { TooltipProvider } from "@neckar-hub/typescript-react-ui-primitives/ui/tooltip";

export {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupAction,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInput,
  SidebarInset,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSkeleton,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarRail,
  SidebarSeparator,
  SidebarTrigger,
  useSidebar,
} from "@neckar-hub/typescript-react-ui-primitives/ui/sidebar";

/**
 * Provider that also provides the tooltips: `SidebarMenuButton` shows its `tooltip` in the collapsed sidebar, and a Radix tooltip
 * throws outside a `TooltipProvider`.
 */
export function SidebarProvider(props: React.ComponentProps<typeof ShadcnSidebarProvider>): React.ReactElement {
  return (
    <TooltipProvider delayDuration={0}>
      <ShadcnSidebarProvider {...props} />
    </TooltipProvider>
  );
}
