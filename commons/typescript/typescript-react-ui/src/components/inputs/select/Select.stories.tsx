/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { SelectGroup, SelectItem, SelectLabel, SelectSeparator } from "../../primitives/select";
import { Select } from "./Select";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / Select",
};

export default meta;

/**
 * Args
 */

interface Args {
  disabled: boolean;
  isInvalid: boolean;
}

/**
 * Stories
 */

export const SelectStory: Story<Args> = (args) => {
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
};
SelectStory.storyName = "Select";
SelectStory.args = {
  disabled: false,
  isInvalid: false,
};
