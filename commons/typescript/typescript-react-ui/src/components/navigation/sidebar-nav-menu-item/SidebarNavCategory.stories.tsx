/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { HardHatIcon, PencilRulerIcon } from "lucide-react";
import { SidebarMenu, SidebarMenuSubButton, SidebarMenuSubItem, SidebarProvider } from "../../primitives/sidebar";
import { SidebarNavCategory } from "./SidebarNavCategory";

const meta = {
  title: "Navigation / SidebarNavCategory",
  component: SidebarNavCategory,
} satisfies Meta;

export default meta;

function SubItem({ title }: { title: string }): React.ReactElement {
  return (
    <SidebarMenuSubItem>
      <SidebarMenuSubButton asChild={true}>
        <a href={`#${title}`}>
          <span>{title}</span>
        </a>
      </SidebarMenuSubButton>
    </SidebarMenuSubItem>
  );
}

/**
 * The category with the current page is open, the other one is closed; a click on either toggles it.
 */
export const Default: StoryObj = {
  render: () => (
    <SidebarProvider>
      <SidebarMenu className="w-64">
        <SidebarNavCategory title="Planung" icon={PencilRulerIcon} containsCurrentPage={true}>
          <SubItem title="Wechselrichter" />
          <SubItem title="Finanzierung" />
        </SidebarNavCategory>
        <SidebarNavCategory title="Umsetzung" icon={HardHatIcon} containsCurrentPage={false}>
          <SubItem title="Dokumentation" />
        </SidebarNavCategory>
      </SidebarMenu>
    </SidebarProvider>
  ),
};
