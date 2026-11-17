import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { TimeInput, type TimeInputProps } from "./Input";

const meta = {
  title: "Inputs / Time Input",
  args: {
    disabled: false,
    isInvalid: false,
  },
} satisfies Meta<TimeInputProps>;

export default meta;

export const Default: StoryObj<TimeInputProps> = {
  render: (args: TimeInputProps) => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <TimeInput {...args} value={value} onChange={setValue} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const IncludeSeconds: StoryObj<TimeInputProps> = {
  render: (args: TimeInputProps) => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <TimeInput {...args} value={value} onChange={setValue} includeSeconds={true} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
