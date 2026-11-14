/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { AdjustableValueAutoStringWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { AdjustableValueSelect, type AdjustableValueSelectValue } from "./AdjustableValueSelect";

/**
 * Meta Information
 */
const meta: StoryDefault = {
  title: "Inputs / AdjustableValueSelect",
};

export default meta;

/**
 * Story Args
 */
interface Args {
  isInvalid: boolean;
  disabled: boolean;
}

const options = ["silver", "black"];

/**
 * Stories
 */
export const AdjustableValueSelectStory: Story<Args> = (args) => {
  const [value, setValue] = useState<AdjustableValueSelectValue<string> | null>({
    type: AdjustableValueAutoStringWithTypeType.Auto,
    value: "silver",
  });

  return (
    <div className="flex w-80 flex-col gap-8">
      <AdjustableValueSelect {...args} options={options} itemRenderer={(item) => item} value={value} onChange={setValue} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
AdjustableValueSelectStory.storyName = "AdjustableValueSelect";
AdjustableValueSelectStory.args = {
  isInvalid: false,
  disabled: false,
};

export const AdjustableValueSelectInheritedStory: Story<Args> = (args) => {
  const [value, setValue] = useState<AdjustableValueSelectValue<string> | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <AdjustableValueSelect {...args} options={options} itemRenderer={(item) => item} value={value} onChange={setValue} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
AdjustableValueSelectInheritedStory.storyName = "AdjustableValueSelect (inherited, no source)";
AdjustableValueSelectInheritedStory.args = {
  isInvalid: false,
  disabled: false,
};
