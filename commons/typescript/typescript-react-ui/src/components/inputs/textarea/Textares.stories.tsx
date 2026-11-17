/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { Textarea } from "./Textarea";

/**
 * Meta Information
 */

const meta = {
  title: "Inputs / Textares",
} satisfies Meta;

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

export const TextareaStory: StoryObj<Args> = {
  name: "Textarea",
  args: {
    disabled: false,
    isInvalid: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <Textarea {...args} value={value} onChange={setValue} rows={5} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
