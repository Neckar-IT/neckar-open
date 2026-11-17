import type { UploadProgress } from "@neckar-hub/typescript-react-query";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../../primitives/button";
import { FileUploadSlot } from "./FileUploadSlot";

/**
 * Meta Information
 */
const meta = {
  title: "Media / FileUploadSlot",
} satisfies Meta;

export default meta;

/**
 * Args
 */
interface Args {
  disabled: boolean;
  maximumSize: number;
}

/**
 * Stories
 */
export const FileUploadSlotStory: StoryObj<Args> = {
  name: "File Upload Slot",
  args: {
    disabled: false,
    maximumSize: 10 * 1024 * 1024,
  },
  render: (args: Args) => {
    const [fileName, setFileName] = useState<string | null>(null);

    return (
      <FileUploadSlot
        accept="image/png,image/jpeg"
        kindLabel="BILD"
        emptyTitle="Noch kein Datenblatt hochgeladen"
        acceptHint="Nur PNG oder JPEG"
        onUpload={(file) => setFileName(file.name)}
        {...args}
      >
        {fileName == null
          ? null
          : ({ openPicker, locked }) => (
              <div className="flex items-center gap-3">
                <span>{fileName}</span>
                <Button variant="outline" disabled={locked} onClick={openPicker}>
                  Ersetzen
                </Button>
              </div>
            )}
      </FileUploadSlot>
    );
  },
};

/**
 * The progress card shows the name and size of the picked file, so each story waits for a pick and then holds its phase.
 */
function HeldUpload({ uploadProgress }: { uploadProgress: UploadProgress }): React.ReactElement {
  const [uploading, setUploading] = useState(false);

  return (
    <FileUploadSlot
      accept="image/png,image/jpeg"
      kindLabel="BILD"
      emptyTitle="Noch kein Datenblatt hochgeladen"
      acceptHint="Nur PNG oder JPEG"
      onUpload={() => setUploading(true)}
      isUploading={uploading}
      uploadProgress={uploadProgress}
      onCancelUpload={() => setUploading(false)}
    />
  );
}

export const FileUploadSlotTransferStory: StoryObj<typeof meta> = {
  name: "File Upload Slot Transfer (62 %)",
  render: () => <HeldUpload uploadProgress={{ kind: "uploading", fraction: 0.62 }} />,
};

export const FileUploadSlotProcessingStory: StoryObj<typeof meta> = {
  name: "File Upload Slot Processing",
  render: () => <HeldUpload uploadProgress={{ kind: "processing" }} />,
};
