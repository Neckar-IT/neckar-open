/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { Textarea } from "./Textarea";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / Textares",
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

export const TextareaStory: Story<Args> = (args) => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <Textarea {...args} value={value} onChange={setValue} rows={5} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
TextareaStory.storyName = "Textarea";
TextareaStory.args = {
  disabled: false,
  isInvalid: false,
};
