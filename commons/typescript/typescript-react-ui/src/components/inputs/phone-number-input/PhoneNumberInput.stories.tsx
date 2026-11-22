/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { PhoneNumberInput, type PhoneNumberInputProps } from "./PhoneNumberInput";

const meta = {
  title: "Inputs / PhoneNumberInput",
  component: PhoneNumberInput,
} satisfies Meta<PhoneNumberInputProps>;

export default meta;

export const PhoneNumberInputStory: StoryObj = {
  name: "Phone Number Input",
  render: () => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <div>
          <PhoneNumberInput value={value} onChange={setValue} placeholder="07473 9594960" />
        </div>

        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
