import type { Story, StoryDefault } from "@ladle/react";
import {
  AdjustableValueAutoDoubleWithTypeType,
  type AdjustableValueDouble,
  AdjustableValueManualDoubleWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { AdjustableValueNumberInput, type AdjustableValueNumberNullable } from "./AdjustableValueNumberInput";

/**
 * Meta Information
 */
const meta: StoryDefault = {
  title: "Inputs / AdjustableValueNumberInput",
};

export default meta;

/**
 * Story Args
 */
interface Args {
  isInvalid: boolean;
  disabled: boolean;
}

const initial: AdjustableValueDouble = {
  type: AdjustableValueAutoDoubleWithTypeType.Auto,
  value: 42,
};

/**
 * Stories
 */
export const AdjustableValueNumberInputStory: Story<Args> = (args) => {
  const [value, setValue] = useState<AdjustableValueNumberNullable | null>(initial);

  return (
    <div className="flex w-80 flex-col gap-8">
      <AdjustableValueNumberInput {...args} value={value} onChange={setValue} unit="ct/kWh" fractionDigits={2} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
AdjustableValueNumberInputStory.storyName = "Default";
AdjustableValueNumberInputStory.args = {
  isInvalid: false,
  disabled: false,
};

const initialManual: AdjustableValueDouble = {
  type: AdjustableValueManualDoubleWithTypeType.Manual,
  value: 42,
};

export const AdjustableValueNumberInputManualStory: Story<Args> = (args) => {
  const [value, setValue] = useState<AdjustableValueNumberNullable | null>(initialManual);

  return (
    <div className="flex w-80 flex-col gap-8">
      <AdjustableValueNumberInput {...args} value={value} onChange={setValue} unit="ct/kWh" fractionDigits={2} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
AdjustableValueNumberInputManualStory.storyName = "Manual";
AdjustableValueNumberInputManualStory.args = {
  isInvalid: false,
  disabled: false,
};

export const AdjustableValueNumberInputNoValueStory: Story<Args> = (args) => {
  const [value, setValue] = useState<AdjustableValueNumberNullable | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <AdjustableValueNumberInput {...args} value={value} onChange={setValue} unit="ct/kWh" fractionDigits={2} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
AdjustableValueNumberInputNoValueStory.storyName = "AdjustableValueNumberInput (No Value)";
AdjustableValueNumberInputNoValueStory.args = {
  isInvalid: false,
  disabled: false,
};
