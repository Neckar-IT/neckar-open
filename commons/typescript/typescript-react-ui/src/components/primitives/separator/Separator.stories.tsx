/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Separator as SeparatorComponent } from "./Separator";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Separator",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Separator: StoryObj<typeof meta> = {
  render: () => {
    return (
      <div className="w-80">
        <div className="space-y-1">
          <h4 className="text-sm leading-none font-medium">Radix Primitives</h4>
          <p className="text-muted-foreground text-sm">An open-source UI component library.</p>
        </div>
        <SeparatorComponent className="my-4" />
        <div className="flex h-5 items-center space-x-4 text-sm">
          <div>Blog</div>
          <SeparatorComponent orientation="vertical" />
          <div>Docs</div>
          <SeparatorComponent orientation="vertical" />
          <div>Source</div>
        </div>
      </div>
    );
  },
};
