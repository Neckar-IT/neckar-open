/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../../primitives/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "../../primitives/dialog";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";
import { SelectPlaceholder, type SelectPlaceholderProps } from "./SelectPlaceholder";

/**
 * Meta Information
 */

const meta = {
  title: "Inputs / SelectPlaceholder",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const SelectPlaceholderStory: StoryObj<SelectPlaceholderProps> = {
  name: "Select Placeholder",
  args: {
    className: "w-80",
    placeholder: "Select Data",
  },
  render: (props: SelectPlaceholderProps) => {
    return (
      <div className="space-y-8">
        <div>
          <p className="text-muted-foreground mb-1 font-mono text-sm">default</p>
          <SelectPlaceholder {...props} />
        </div>

        <div>
          <p className="text-muted-foreground mb-1 font-mono text-sm">warning</p>
          <SelectPlaceholder {...props} message="some warning message" variant="warning" />
        </div>

        <div>
          <p className="text-muted-foreground mb-1 font-mono text-sm">error</p>
          <SelectPlaceholder {...props} message="some error message with a lot of text that just won't end" variant="error" />
        </div>

        <div>
          <p className="text-muted-foreground mb-1 font-mono text-sm">in a dialog</p>
          <Dialog>
            <DialogTrigger asChild>
              <Button>Open Dialog</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>SelectPlaceholder in Dialog</DialogTitle>
              </DialogHeader>

              <SelectPlaceholder {...props} message="some error message with a lot of text that just won't end" variant="error" />

              <Tooltip>
                <TooltipTrigger>
                  <p>open tooltip</p>
                </TooltipTrigger>
                <TooltipContent>This is the content</TooltipContent>
              </Tooltip>
            </DialogContent>
          </Dialog>
        </div>
      </div>
    );
  },
};
