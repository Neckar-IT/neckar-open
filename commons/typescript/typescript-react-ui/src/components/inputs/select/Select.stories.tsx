/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { SelectGroup, SelectItem, SelectLabel, SelectSeparator } from "../../primitives/select";
import { Select } from "./Select";

const meta = {
  title: "Inputs / Select",
  component: Select,
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  disabled: boolean;
  isInvalid: boolean;
}

export const SelectStory: StoryObj<Args> = {
  name: "Select",
  args: {
    disabled: false,
    isInvalid: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="w-80">
        <Select {...args} value={value} onChange={setValue}>
          <SelectGroup>
            <SelectLabel>Vegetables</SelectLabel>
            <SelectItem value="potato">Potato</SelectItem>
            <SelectItem value="tomato">Tomato</SelectItem>
            <SelectItem value="cucumber">Cucumber</SelectItem>
          </SelectGroup>
          <SelectSeparator />
          <SelectGroup>
            <SelectLabel>Fruits</SelectLabel>
            <SelectItem value="apple">Apple</SelectItem>
            <SelectItem value="banana">Banana</SelectItem>
            <SelectItem value="orange">Orange</SelectItem>
          </SelectGroup>
        </Select>

        <CodeBlock className="mt-8">{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
