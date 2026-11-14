/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { Button } from "../../primitives/button";
import { SaveDialog, type SaveDialogProps } from "./SaveDialog";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Overlays / SaveDialog",
};

export default meta;

/**
 * Stories
 */

export const SaveDialogStory: Story<Pick<SaveDialogProps, "size">> = ({ size }) => {
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
};
SaveDialogStory.storyName = "SaveDialog";
SaveDialogStory.args = {
  size: "sm",
};
SaveDialogStory.argTypes = {
  size: {
    options: ["sm", "md", "lg", "xl", "full"],
    control: { type: "select" },
  },
};
