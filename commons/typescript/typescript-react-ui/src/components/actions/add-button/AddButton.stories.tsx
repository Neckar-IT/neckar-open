/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../../primitives/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../primitives/dialog";
import { AddButton } from "./AddButton";

/**
 * Meta Information
 */

const meta = {
  title: "Actions / AddButton",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const AddButtonStory: StoryObj<typeof meta> = {
  name: "Add Button",
  render: () => {
    return <AddButton dialog={ExampleDialog} title="This is the title" />;
  },
};

function ExampleDialog({ onOpenChange, title }: { onOpenChange: (open: boolean) => void; title: string }): React.ReactElement {
  return (
    <Dialog open={true} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogFooter>
            <Button onClick={() => onOpenChange(false)}>Close</Button>
          </DialogFooter>
        </DialogHeader>
      </DialogContent>
    </Dialog>
  );
}
