/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { MousePointerIcon, TrashIcon, ZoomInIcon, ZoomOutIcon } from "lucide-react";
import { Toolbar, ToolbarButton, ToolbarGroup } from "./Toolbar";

/**
 * Meta Information
 */

const meta = {
  title: "Actions / Toolbar",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const ToolbarStory: StoryObj<typeof meta> = {
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
