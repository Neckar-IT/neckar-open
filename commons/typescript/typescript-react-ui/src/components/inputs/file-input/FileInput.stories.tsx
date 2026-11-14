import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { FileInput } from "./FileInput";

/**
 * Meta Information
 */
export default {
  title: "Inputs / FileInput",
} satisfies StoryDefault;

/**
 * Args
 */
interface Args {
  disabled: boolean;
}

/**
 * Stories
 */
export const FileInputStory: Story<Args> = (args) => {
  const [value, setValue] = useState<File | null>(null);

  return (
    <div className="flex flex-col gap-8">
      <FileInput className="w-80" value={value} onChange={setValue} {...args} />
      <CodeBlock>{`Selected File: ${value ? value.name : "None"}`}</CodeBlock>
    </div>
  );
};
FileInputStory.storyName = "File Input";
FileInputStory.args = {
  disabled: false,
};
