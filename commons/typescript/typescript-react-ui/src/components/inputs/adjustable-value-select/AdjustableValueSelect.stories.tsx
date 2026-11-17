/**
 * Imports
 */

import { AdjustableValueAutoStringWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { AdjustableValueSelect, type AdjustableValueSelectValue } from "./AdjustableValueSelect";

/**
 * Meta Information
 */
const meta = {
  title: "Inputs / AdjustableValueSelect",
} satisfies Meta;

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
export const AdjustableValueSelectStory: StoryObj<Args> = {
  name: "AdjustableValueSelect",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
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
  },
};

export const AdjustableValueSelectInheritedStory: StoryObj<Args> = {
  name: "AdjustableValueSelect (inherited, no source)",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<AdjustableValueSelectValue<string> | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <AdjustableValueSelect {...args} options={options} itemRenderer={(item) => item} value={value} onChange={setValue} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
