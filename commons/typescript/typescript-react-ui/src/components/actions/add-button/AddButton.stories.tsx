/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { Button } from "../../primitives/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../primitives/dialog";
import { AddButton } from "./AddButton";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Actions / AddButton",
};

export default meta;

/**
 * Stories
 */

export const AddButtonStory: Story = () => {
  return <AddButton dialog={ExampleDialog} title="This is the title" />;
};
AddButtonStory.storyName = "Add Button";

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
