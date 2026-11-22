/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useState } from "react";
import { Button } from "../../primitives/button";
import { ConfirmationDialog, type ConfirmationDialogProps } from "./ConfirmationDialog";
import { DestructiveConfirmationDialog } from "./DestructiveConfirmationDialog";

const meta = {
  title: "Overlays / ConfirmationDialog",
  component: ConfirmationDialog,
} satisfies Meta<ConfirmationDialogProps>;

export default meta;

/**
 * Story Props
 */

interface StoryProps extends Pick<ConfirmationDialogProps, "title" | "description" | "isPending"> {
  asyncAction: boolean;
}

export const ConfirmationDialogStory: StoryObj<StoryProps> = {
  name: "ConfirmationDialog",
  args: {
    title: "Aktion bestätigen",
    description: "Möchten Sie diese Aktion ausführen?",
    isPending: undefined,
    asyncAction: false,
  },
  render: (args: StoryProps) => {
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
  },
};

export const DestructiveConfirmationDialogStory: StoryObj<StoryProps> = {
  name: "DestructiveConfirmationDialog",
  args: {
    title: "Sind Sie sicher?",
    description: "Das Löschen kann nicht rückgängig gemacht werden.",
    isPending: undefined,
    asyncAction: false,
  },
  render: (args: StoryProps) => {
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
          <DestructiveConfirmationDialog
            {...args}
            onConfirm={args.asyncAction ? handleAsyncConfirm : handleConfirm}
            onOpenChange={setOpen}
          />
        ) : null}
      </>
    );
  },
};

/**
 * Renders without `title`/`description` props to demonstrate the textKey defaults:
 * `confirmationDialog.title` for the neutral dialog, `destructiveConfirmationDialog.description`
 * for the destructive wrapper.
 */
export const ConfirmationDialogDefaultsStory: StoryObj = {
  name: "ConfirmationDialog (textKey defaults)",
  render: () => {
    const [open, setOpen] = useState(true);

    return (
      <>
        <Button onClick={() => setOpen(true)}>Open</Button>

        {open ? <ConfirmationDialog onConfirm={() => setOpen(false)} onOpenChange={setOpen} /> : null}
      </>
    );
  },
};

export const DestructiveConfirmationDialogDefaultsStory: StoryObj = {
  name: "DestructiveConfirmationDialog (default description)",
  render: () => {
    const [open, setOpen] = useState(true);

    return (
      <>
        <Button onClick={() => setOpen(true)}>Delete</Button>

        {open ? <DestructiveConfirmationDialog title="Eintrag löschen?" onConfirm={() => setOpen(false)} onOpenChange={setOpen} /> : null}
      </>
    );
  },
};
