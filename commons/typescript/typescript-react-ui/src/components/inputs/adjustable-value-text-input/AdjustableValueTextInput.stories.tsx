/**
 * Imports
 */

import { AdjustableValueAutoStringWithTypeType, AdjustableValueManualStringWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { type AdjustableValueTextExtended, AdjustableValueTextInput } from "./AdjustableValueTextInput";

const meta = {
  title: "Inputs / AdjustableValueTextInput",
  component: AdjustableValueTextInput,
} satisfies Meta;

export default meta;

/**
 * Story Args
 */
interface Args {
  isInvalid: boolean;
  disabled: boolean;
}

export const AdjustableValueTextInputStory: StoryObj<Args> = {
  name: "AdjustableValueTextInput",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<AdjustableValueTextExtended | null>({
      type: AdjustableValueAutoStringWithTypeType.Auto,
      value: "john.doe",
    });

    return (
      <div className="flex w-80 flex-col gap-8">
        <AdjustableValueTextInput {...args} value={value} onChange={setValue} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const AdjustableValueTextInputManualStory: StoryObj<Args> = {
  name: "AdjustableValueTextInput (Manual)",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<AdjustableValueTextExtended | null>({
      type: AdjustableValueManualStringWithTypeType.Manual,
      value: "john.doe",
    });

    return (
      <div className="flex w-80 flex-col gap-8">
        <AdjustableValueTextInput {...args} value={value} onChange={setValue} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const AdjustableValueTextInputNoValueStory: StoryObj<Args> = {
  name: "AdjustableValueTextInput (No Value)",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<AdjustableValueTextExtended | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <AdjustableValueTextInput {...args} value={value} onChange={setValue} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
