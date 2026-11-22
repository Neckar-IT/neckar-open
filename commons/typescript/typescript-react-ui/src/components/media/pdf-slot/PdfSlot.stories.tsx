import type { UploadProgress } from "@neckar-hub/typescript-react-query";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { PdfSlot, type PdfSlotDocument } from "./PdfSlot";

const meta = {
  title: "Media / PdfSlot",
  component: PdfSlot,
} satisfies Meta;

export default meta;

/**
 * Args
 */
interface Args {
  disabled: boolean;
  removeDisabled: boolean;
  maximumSize: number;
}

export const PdfSlotStory: StoryObj<Args> = {
  name: "Pdf Slot",
  args: {
    disabled: false,
    removeDisabled: false,
    maximumSize: 10 * 1024 * 1024,
  },
  render: (args: Args) => {
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
  },
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

export const PdfSlotTransferStory: StoryObj = {
  name: "Pdf Slot Transfer (62 %)",
  render: () => <HeldUpload uploadProgress={{ kind: "uploading", fraction: 0.62 }} />,
};

export const PdfSlotProcessingStory: StoryObj = {
  name: "Pdf Slot Processing",
  render: () => <HeldUpload uploadProgress={{ kind: "processing" }} />,
};
