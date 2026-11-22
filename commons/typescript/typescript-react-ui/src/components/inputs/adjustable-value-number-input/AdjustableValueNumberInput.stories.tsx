import {
  AdjustableValueAutoDoubleWithTypeType,
  type AdjustableValueDouble,
  AdjustableValueManualDoubleWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { AdjustableValueNumberInput, type AdjustableValueNumberNullable } from "./AdjustableValueNumberInput";

const meta = {
  title: "Inputs / AdjustableValueNumberInput",
  component: AdjustableValueNumberInput,
} satisfies Meta;

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

export const AdjustableValueNumberInputStory: StoryObj<Args> = {
  name: "Default",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<AdjustableValueNumberNullable | null>(initial);

    return (
      <div className="flex w-80 flex-col gap-8">
        <AdjustableValueNumberInput {...args} value={value} onChange={setValue} unit="ct/kWh" fractionDigits={2} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

const initialManual: AdjustableValueDouble = {
  type: AdjustableValueManualDoubleWithTypeType.Manual,
  value: 42,
};

export const AdjustableValueNumberInputManualStory: StoryObj<Args> = {
  name: "Manual",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<AdjustableValueNumberNullable | null>(initialManual);

    return (
      <div className="flex w-80 flex-col gap-8">
        <AdjustableValueNumberInput {...args} value={value} onChange={setValue} unit="ct/kWh" fractionDigits={2} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const AdjustableValueNumberInputNoValueStory: StoryObj<Args> = {
  name: "AdjustableValueNumberInput (No Value)",
  args: {
    isInvalid: false,
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<AdjustableValueNumberNullable | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <AdjustableValueNumberInput {...args} value={value} onChange={setValue} unit="ct/kWh" fractionDigits={2} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
