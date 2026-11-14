/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Bold, Italic, Underline } from "lucide-react";
import { ToggleGroupItem, ToggleGroup as ToggleGroupRoot } from "./ToggleGroup";

/**
 * Meta Information
 */

export default {
  title: "Primitives / ToggleGroup",
} satisfies StoryDefault;

/**
 * Stories
 */

export const ToggleGroup: Story = () => {
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
};
