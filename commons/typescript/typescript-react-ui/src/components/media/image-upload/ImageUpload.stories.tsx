/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { ImageUpload, ImageUploadInput, ImageUploadPreview } from "./ImageUpload";

const meta = {
  title: "Media / ImageUpload",
  component: ImageUpload,
  decorators: [
    (Story) => (
      <div className="mx-auto max-w-screen-sm">
        <Story />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

export const ImageUploadStory: StoryObj = {
  name: "ImageUpload",
  render: () => {
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
  },
};
