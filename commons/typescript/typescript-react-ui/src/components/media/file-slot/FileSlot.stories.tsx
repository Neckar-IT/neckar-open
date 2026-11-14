import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { FileSlot } from "./FileSlot";

/**
 * Meta Information
 */
export default {
  title: "Media / FileSlot",
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
export const FileSlotStory: Story<Args> = (args) => {
  const [uploadedAt, setUploadedAt] = useState<string | null>(null);
  const [lastPicked, setLastPicked] = useState<string>("none");

  return (
    <div className="flex flex-col gap-8">
      <FileSlot
        url={uploadedAt === null ? null : "https://example.org/data-sheet.pdf"}
        uploadedAt={uploadedAt}
        fileName="data-sheet.pdf"
        accept="application/pdf"
        onUpload={(file) => {
          setLastPicked(file.name);
          setUploadedAt(new Date().toISOString());
        }}
        onRemove={() => {
          setUploadedAt(null);
        }}
        {...args}
      />
      <CodeBlock>{`Last picked file: ${lastPicked}`}</CodeBlock>
    </div>
  );
};
FileSlotStory.storyName = "File Slot";
FileSlotStory.args = {
  disabled: false,
};
