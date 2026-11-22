import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { FileInput } from "./FileInput";

const meta = {
  title: "Inputs / FileInput",
  component: FileInput,
} satisfies Meta;

export default meta;

/**
 * Args
 */
interface Args {
  disabled: boolean;
}

export const FileInputStory: StoryObj<Args> = {
  name: "File Input",
  args: {
    disabled: false,
  },
  render: (args: Args) => {
    const [value, setValue] = useState<File | null>(null);

    return (
      <div className="flex flex-col gap-8">
        <FileInput className="w-80" value={value} onChange={setValue} {...args} />
        <CodeBlock>{`Selected File: ${value ? value.name : "None"}`}</CodeBlock>
      </div>
    );
  },
};
