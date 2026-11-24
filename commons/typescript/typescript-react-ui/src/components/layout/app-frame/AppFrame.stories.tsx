/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { BookOpenIcon } from "lucide-react";
import { Breadcrumbs } from "../../navigation/breadcrumbs";
import { Sidebar, SidebarContent, SidebarProvider } from "../../primitives/sidebar";
import { AppContentColumn, AppMain } from "./AppFrame";

const meta = {
  title: "Layout / AppMain",
  component: AppMain,
  parameters: { layout: "fullscreen" },
} satisfies Meta;

export default meta;

/**
 * Page frame next to an open sidebar, as in the apps: the header ends at the right edge of the viewport, and a trail wider
 * than the header ends with an ellipsis.
 */
export const WithSidebar: StoryObj = {
  render: () => {
    return (
      <SidebarProvider>
        <Sidebar>
          <SidebarContent />
        </Sidebar>
        <AppContentColumn>
          <AppMain
            appName="Patterns"
            brandIcon={BookOpenIcon}
            breadcrumbs={
              <Breadcrumbs>
                <a data-text-content href="#books">
                  Bücher
                </a>
                <a data-text-content href="#book">
                  <span>
                    A Curious and Frequently Surprising Account of the Many Failed Expeditions to Locate the Reputed Inland Sea of the
                    Southern Continent, Compiled from Surviving Diaries, Maps and Letters
                  </span>
                </a>
              </Breadcrumbs>
            }
          >
            <h1>Auflagen</h1>
          </AppMain>
        </AppContentColumn>
      </SidebarProvider>
    );
  },
};
