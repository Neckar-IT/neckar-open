/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { TextInput } from "./TextInput";

/**
 * Meta Information
 */

const meta = {
  title: "Inputs / TextInput",
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  isInvalid: boolean;
  disabled: boolean;
}

/**
 * Stories
 */

export const TextInputStory: StoryObj<Args> = {
  name: "TextInput",
  args: {
    disabled: false,
    isInvalid: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="w-80">
        <TextInput value={value} onChange={setValue} placeholder="John Doe" {...args} />

        <CodeBlock className="mt-8">{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
