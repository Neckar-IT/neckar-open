/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { PhoneNumberInput, type PhoneNumberInputProps } from "./PhoneNumberInput";

/**
 * Meta Information
 */

const meta: StoryDefault<PhoneNumberInputProps> = {
  title: "Inputs / PhoneNumberInput",
};

export default meta;

/**
 * Stories
 */

export const PhoneNumberInputStory: Story = () => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <div>
        <PhoneNumberInput value={value} onChange={setValue} placeholder="07473 9594960" />
      </div>

      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
PhoneNumberInputStory.storyName = "Phone Number Input";
