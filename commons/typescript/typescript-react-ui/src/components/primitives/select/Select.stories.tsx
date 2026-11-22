/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { SelectContent, SelectGroup, SelectItem, SelectLabel, Select as SelectRoot, SelectTrigger, SelectValue } from "./Select";

const meta = {
  title: "Primitives / Select",
  component: SelectRoot,
} satisfies Meta;

export default meta;

export const Select: StoryObj = {
  render: () => {
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
  },
};
