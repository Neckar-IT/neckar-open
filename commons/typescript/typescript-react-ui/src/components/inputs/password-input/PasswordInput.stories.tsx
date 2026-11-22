/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { PasswordInput } from "./PasswordInput";

const meta = {
  title: "Inputs / PasswordInput",
  component: PasswordInput,
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  disabled: boolean;
  isInvalid: boolean;
}

export const PasswordInputStory: StoryObj<Args> = {
  name: "PasswordInput",
  args: {
    disabled: false,
    isInvalid: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <PasswordInput {...args} value={value} onChange={setValue} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
