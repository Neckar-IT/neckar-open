/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useState } from "react";
import { Button } from "../../primitives/button";
import { ConfirmationDialog, type ConfirmationDialogProps } from "./ConfirmationDialog";
import { DestructiveConfirmationDialog } from "./DestructiveConfirmationDialog";

/**
 * Meta Information
 */

const meta: StoryDefault<ConfirmationDialogProps> = {
  title: "Overlays / ConfirmationDialog",
};

export default meta;

/**
 * Story Props
 */

interface StoryProps extends Pick<ConfirmationDialogProps, "title" | "description" | "isPending"> {
  asyncAction: boolean;
}

/**
 * Stories
 */

export const ConfirmationDialogStory: Story<StoryProps> = (args) => {
  const [open, setOpen] = useState(true);

  const handleConfirm = () => {
    setOpen(false);
    window.alert("Confirmed!");
  };

  const handleAsyncConfirm = async () => {
    await delay(2000);
    setOpen(false);
    window.alert("Confirmed!");
  };

  return (
    <>
      <Button onClick={() => setOpen(true)}>Open</Button>

      {open ? (
        <ConfirmationDialog {...args} onConfirm={args.asyncAction ? handleAsyncConfirm : handleConfirm} onOpenChange={setOpen} />
      ) : null}
    </>
  );
};
ConfirmationDialogStory.storyName = "ConfirmationDialog";
ConfirmationDialogStory.args = {
  title: "Aktion bestätigen",
  description: "Möchten Sie diese Aktion ausführen?",
  isPending: undefined,
  asyncAction: false,
};

export const DestructiveConfirmationDialogStory: Story<StoryProps> = (args) => {
  const [open, setOpen] = useState(true);

  const handleConfirm = () => {
    setOpen(false);
    window.alert("Confirmed!");
  };

  const handleAsyncConfirm = async () => {
    await delay(2000);
    setOpen(false);
    window.alert("Confirmed!");
  };

  return (
    <>
      <Button onClick={() => setOpen(true)}>Delete</Button>

      {open ? (
        <DestructiveConfirmationDialog {...args} onConfirm={args.asyncAction ? handleAsyncConfirm : handleConfirm} onOpenChange={setOpen} />
      ) : null}
    </>
  );
};
DestructiveConfirmationDialogStory.storyName = "DestructiveConfirmationDialog";
DestructiveConfirmationDialogStory.args = {
  title: "Sind Sie sicher?",
  description: "Das Löschen kann nicht rückgängig gemacht werden.",
  isPending: undefined,
  asyncAction: false,
};

/**
 * Renders without `title`/`description` props to demonstrate the textKey defaults:
 * `confirmationDialog.title` for the neutral dialog, `destructiveConfirmationDialog.description`
 * for the destructive wrapper.
 */
export const ConfirmationDialogDefaultsStory: Story = () => {
  const [open, setOpen] = useState(true);

  return (
    <>
      <Button onClick={() => setOpen(true)}>Open</Button>

      {open ? <ConfirmationDialog onConfirm={() => setOpen(false)} onOpenChange={setOpen} /> : null}
    </>
  );
};
ConfirmationDialogDefaultsStory.storyName = "ConfirmationDialog (textKey defaults)";

export const DestructiveConfirmationDialogDefaultsStory: Story = () => {
  const [open, setOpen] = useState(true);

  return (
    <>
      <Button onClick={() => setOpen(true)}>Delete</Button>

      {open ? <DestructiveConfirmationDialog title="Eintrag löschen?" onConfirm={() => setOpen(false)} onOpenChange={setOpen} /> : null}
    </>
  );
};
DestructiveConfirmationDialogDefaultsStory.storyName = "DestructiveConfirmationDialog (default description)";
