/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../../primitives/button";
import { SaveDialog, type SaveDialogProps } from "./SaveDialog";

/**
 * Meta Information
 */

const meta = {
  title: "Overlays / SaveDialog",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const SaveDialogStory: StoryObj<Pick<SaveDialogProps, "size">> = {
  name: "SaveDialog",
  args: {
    size: "sm",
  },
  argTypes: {
    size: {
      options: ["sm", "md", "lg", "xl", "full"],
      control: { type: "select" },
    },
  },
  render: ({ size }: Pick<SaveDialogProps, "size">) => {
    const [open, setOpen] = useState(true);

    const handleSubmit = () => {
      console.log("submitted");
      setOpen(false);
    };

    return (
      <>
        <Button onClick={() => setOpen(true)}>Open Dialog</Button>

        <SaveDialog open={open} size={size} textKeys={{ title: "Lorem ipsum" }} onOpenChange={setOpen} onSubmit={handleSubmit}>
          <p>
            Lorem ipsum dolor sit, amet consectetur adipisicing elit. Aliquid sunt est adipisci veritatis neque ab dignissimos quis,
            perspiciatis eveniet dicta non. Sed, nam quibusdam! Laboriosam aspernatur libero explicabo eaque sit.
          </p>
        </SaveDialog>
      </>
    );
  },
};
