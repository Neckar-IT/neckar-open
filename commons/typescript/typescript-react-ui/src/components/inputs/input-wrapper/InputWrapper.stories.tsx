/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { TextInput } from "../text-input";

/**
 * meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / InputWrapper",
};

export default meta;

/**
 * Stories
 */

export const InputWrapperStory: Story = () => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <div className="flex flex-col gap-1">
        <p className="font-mono text-sm font-semibold">default:</p>
        <TextInput value={value} onChange={setValue} />
      </div>
      <div className="flex flex-col gap-1">
        <p className="font-mono text-sm font-semibold">disabled:</p>
        <TextInput value={value} onChange={setValue} disabled={true} />
      </div>
      <div className="flex flex-col gap-1">
        <p className="font-mono text-sm font-semibold">isInvalid:</p>
        <TextInput value={value} onChange={setValue} isInvalid={true} />
      </div>
    </div>
  );
};
