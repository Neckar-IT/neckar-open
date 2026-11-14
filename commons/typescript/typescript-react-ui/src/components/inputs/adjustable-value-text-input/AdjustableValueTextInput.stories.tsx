/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { AdjustableValueAutoStringWithTypeType, AdjustableValueManualStringWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { type AdjustableValueTextExtended, AdjustableValueTextInput } from "./AdjustableValueTextInput";

/**
 * Meta Information
 */
const meta: StoryDefault = {
  title: "Inputs / AdjustableValueTextInput",
};

export default meta;

/**
 * Story Args
 */
interface Args {
  isInvalid: boolean;
  disabled: boolean;
}

/**
 * Stories
 */
export const AdjustableValueTextInputStory: Story<Args> = (args) => {
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
};
AdjustableValueTextInputStory.storyName = "AdjustableValueTextInput";
AdjustableValueTextInputStory.args = {
  isInvalid: false,
  disabled: false,
};

export const AdjustableValueTextInputManualStory: Story<Args> = (args) => {
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
};
AdjustableValueTextInputManualStory.storyName = "AdjustableValueTextInput (Manual)";
AdjustableValueTextInputManualStory.args = {
  isInvalid: false,
  disabled: false,
};

export const AdjustableValueTextInputNoValueStory: Story<Args> = (args) => {
  const [value, setValue] = useState<AdjustableValueTextExtended | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <AdjustableValueTextInput {...args} value={value} onChange={setValue} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
AdjustableValueTextInputNoValueStory.storyName = "AdjustableValueTextInput (No Value)";
AdjustableValueTextInputNoValueStory.args = {
  isInvalid: false,
  disabled: false,
};
