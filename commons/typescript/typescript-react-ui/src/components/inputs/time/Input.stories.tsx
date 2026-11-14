import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { TimeInput, type TimeInputProps } from "./Input";

export default {
  title: "Inputs / Time Input",
  args: {
    disabled: false,
    isInvalid: false,
  },
} satisfies StoryDefault<TimeInputProps>;

export const Default: Story<TimeInputProps> = (args) => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <TimeInput {...args} value={value} onChange={setValue} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};

export const IncludeSeconds: Story<TimeInputProps> = (args) => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <TimeInput {...args} value={value} onChange={setValue} includeSeconds={true} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
