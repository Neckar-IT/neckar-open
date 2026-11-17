import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { FileSlot } from "./FileSlot";

/**
 * Meta Information
 */
const meta = {
  title: "Media / FileSlot",
} satisfies Meta;

export default meta;

/**
 * Args
 */
interface Args {
  disabled: boolean;
}

/**
 * Stories
 */
export const FileSlotStory: StoryObj<Args> = {
  name: "File Slot",
  args: {
    disabled: false,
  },
  render: (args: Args) => {
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
  },
};
