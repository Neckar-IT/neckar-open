/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { TextInput } from "./TextInput";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / TextInput",
};

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

export const TextInputStory: Story<Args> = (args) => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="w-80">
      <TextInput value={value} onChange={setValue} placeholder="John Doe" {...args} />

      <CodeBlock className="mt-8">{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
TextInputStory.storyName = "TextInput";
TextInputStory.args = {
  disabled: false,
  isInvalid: false,
};
