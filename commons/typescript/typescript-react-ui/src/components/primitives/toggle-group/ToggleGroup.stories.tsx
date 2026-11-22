/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Bold, Italic, Underline } from "lucide-react";
import { ToggleGroupItem, ToggleGroup as ToggleGroupRoot } from "./ToggleGroup";

const meta = {
  title: "Primitives / ToggleGroup",
  component: ToggleGroupRoot,
} satisfies Meta;

export default meta;

export const ToggleGroup: StoryObj = {
  render: () => {
    return (
      <ToggleGroupRoot type="multiple" variant="outline">
        <ToggleGroupItem aria-label="Toggle bold" value="bold">
          <Bold className="h-4 w-4" />
        </ToggleGroupItem>
        <ToggleGroupItem aria-label="Toggle italic" value="italic">
          <Italic className="h-4 w-4" />
        </ToggleGroupItem>
        <ToggleGroupItem aria-label="Toggle underline" value="underline">
          <Underline className="h-4 w-4" />
        </ToggleGroupItem>
      </ToggleGroupRoot>
    );
  },
};
