/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { SelectContent, SelectGroup, SelectItem, SelectLabel, Select as SelectRoot, SelectTrigger, SelectValue } from "./Select";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Select",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Select: StoryObj<typeof meta> = {
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
