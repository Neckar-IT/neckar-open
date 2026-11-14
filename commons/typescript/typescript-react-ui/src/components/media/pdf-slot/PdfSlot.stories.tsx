import type { Story, StoryDefault } from "@ladle/react";
import type { UploadProgress } from "@neckar-hub/typescript-react-query";
import { useState } from "react";
import { PdfSlot, type PdfSlotDocument } from "./PdfSlot";

/**
 * Meta Information
 */
export default {
  title: "Media / PdfSlot",
} satisfies StoryDefault;

/**
 * Args
 */
interface Args {
  disabled: boolean;
  removeDisabled: boolean;
  maximumSize: number;
}

/**
 * Stories
 */
export const PdfSlotStory: Story<Args> = (args) => {
  const [document, setDocument] = useState<PdfSlotDocument | null>(null);

  return (
    <PdfSlot
      document={document}
      emptyTitle="Noch kein Datenblatt hochgeladen"
      onUpload={(file) =>
        setDocument({
          fileName: file.name,
          size: file.size,
          uploadedAt: new Date().toISOString(),
          url: "https://example.org/data-sheet.pdf",
          downloadUrl: "https://example.org/data-sheet.pdf",
          thumbnailUrl: null,
          pageCount: 2,
        })
      }
      onRemove={() => setDocument(null)}
      {...args}
    />
  );
};
PdfSlotStory.storyName = "Pdf Slot";
PdfSlotStory.args = {
  disabled: false,
  removeDisabled: false,
  maximumSize: 10 * 1024 * 1024,
};

/**
 * The upload progress card shows the name and size of the picked file, so each story waits for a pick and then holds its phase.
 */
function HeldUpload({ uploadProgress }: { uploadProgress: UploadProgress }): React.ReactElement {
  const [uploading, setUploading] = useState(false);

  return (
    <PdfSlot
      document={null}
      emptyTitle="Noch kein Datenblatt hochgeladen"
      onUpload={() => setUploading(true)}
      onRemove={() => undefined}
      isUploading={uploading}
      uploadProgress={uploadProgress}
      onCancelUpload={() => setUploading(false)}
    />
  );
}

export const PdfSlotTransferStory: Story = () => <HeldUpload uploadProgress={{ kind: "uploading", fraction: 0.62 }} />;
PdfSlotTransferStory.storyName = "Pdf Slot Transfer (62 %)";

export const PdfSlotProcessingStory: Story = () => <HeldUpload uploadProgress={{ kind: "processing" }} />;
PdfSlotProcessingStory.storyName = "Pdf Slot Processing";
