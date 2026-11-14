import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { ImageSlot } from "./ImageSlot";

/**
 * Meta Information
 */
export default {
  title: "Media / ImageSlot",
} satisfies StoryDefault;

/**
 * Args
 */
interface Args {
  disabled: boolean;
  removeDisabled: boolean;
  maximumSizeMb: number;
}

/**
 * Stories
 */
export const ImageSlotStory: Story<Args> = (args) => {
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
};

ImageSlotStory.args = {
  disabled: false,
  removeDisabled: false,
  maximumSizeMb: 10,
};
