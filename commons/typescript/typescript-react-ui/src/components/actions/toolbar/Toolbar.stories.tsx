/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { MousePointerIcon, TrashIcon, ZoomInIcon, ZoomOutIcon } from "lucide-react";
import { Toolbar, ToolbarButton, ToolbarGroup } from "./Toolbar";

const meta = {
  title: "Actions / Toolbar",
  component: Toolbar,
} satisfies Meta;

export default meta;

export const ToolbarStory: StoryObj = {
  name: "Toolbar",
  render: () => {
    return (
      <Toolbar className="mx-auto">
        <ToolbarGroup>
          <ToolbarButton active={true} disabled={false} label="Select">
            <MousePointerIcon />
          </ToolbarButton>
        </ToolbarGroup>
        <ToolbarGroup>
          <ToolbarButton label="Zoom in">
            <ZoomInIcon />
          </ToolbarButton>
          <ToolbarButton disabled={true} label="Zoom out">
            <ZoomOutIcon />
          </ToolbarButton>
        </ToolbarGroup>
        <ToolbarGroup>
          <ToolbarButton disabled={false} label="Delete" variant="destructive">
            <TrashIcon />
          </ToolbarButton>
        </ToolbarGroup>
      </Toolbar>
    );
  },
};
