/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { PasswordInput } from "./PasswordInput";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / PasswordInput",
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

export const PasswordInputStory: Story<Args> = (args) => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <PasswordInput {...args} value={value} onChange={setValue} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
PasswordInputStory.storyName = "PasswordInput";
PasswordInputStory.args = {
  disabled: false,
  isInvalid: false,
};
