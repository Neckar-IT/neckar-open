/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { SelectContent, SelectGroup, SelectItem, SelectLabel, Select as SelectRoot, SelectTrigger, SelectValue } from "./Select";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Select",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Select: Story = () => {
  return (
    <SelectRoot>
      <SelectTrigger className="w-[180px]">
        <SelectValue placeholder="Select a fruit" />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel>Fruits</SelectLabel>
          <SelectItem value="apple">Apple</SelectItem>
          <SelectItem value="banana">Banana</SelectItem>
          <SelectItem value="blueberry">Blueberry</SelectItem>
          <SelectItem value="grapes">Grapes</SelectItem>
          <SelectItem value="pineapple">Pineapple</SelectItem>
        </SelectGroup>
      </SelectContent>
    </SelectRoot>
  );
};
