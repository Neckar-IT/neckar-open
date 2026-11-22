import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { ImageSlot } from "./ImageSlot";

const meta = {
  title: "Media / ImageSlot",
  component: ImageSlot,
} satisfies Meta;

export default meta;

/**
 * Args
 */
interface Args {
  disabled: boolean;
  removeDisabled: boolean;
  maximumSizeMb: number;
}

export const ImageSlotStory: StoryObj<Args> = {
  args: {
    disabled: false,
    removeDisabled: false,
    maximumSizeMb: 10,
  },
  render: (args: Args) => {
    const [imageUrl, setImageUrl] = useState<string | undefined>(undefined);

    return (
      <ImageSlot
        className="h-64 w-64"
        imageUrl={imageUrl}
        thumbnailUrl={imageUrl}
        alt="Portrait"
        maximumSize={args.maximumSizeMb * 1024 * 1024}
        accept={["image/jpeg", "image/png", "image/webp"]}
        onUpload={async (file) => {
          await new Promise((resolve) => setTimeout(resolve, 800));
          setImageUrl(URL.createObjectURL(file));
        }}
        onRemove={() => setImageUrl(undefined)}
        disabled={args.disabled}
        removeDisabled={args.removeDisabled}
      />
    );
  },
};
