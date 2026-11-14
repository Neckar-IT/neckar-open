/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { ImageUpload, ImageUploadInput, ImageUploadPreview } from "./ImageUpload";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Media / ImageUpload",
  decorators: [
    (Story) => (
      <div className="mx-auto max-w-screen-sm">
        <Story />
      </div>
    ),
  ],
};

export default meta;

/**
 * Stories
 */

export const ImageUploadStory: Story = () => {
  const [value, setValue] = useState<File | undefined>(undefined);

  return (
    <ImageUpload accept="image/png, image/jpeg" className="flex items-start gap-2" value={value} onChange={setValue}>
      <ImageUploadPreview className="aspect-video w-32 rounded-md border object-contain" />
      <div className="flex-1">
        <ImageUploadInput />
        <p className="text-muted-foreground mt-1 text-xs">png und jpg, maximal 3 MB</p>
      </div>
    </ImageUpload>
  );
};
ImageUploadStory.storyName = "ImageUpload";
