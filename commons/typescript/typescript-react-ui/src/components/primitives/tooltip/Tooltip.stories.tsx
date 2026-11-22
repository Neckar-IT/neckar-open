/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../button";
import { TooltipContent, TooltipProvider, Tooltip as TooltipRoot, TooltipTrigger } from "./Tooltip";

const meta = {
  title: "Primitives / Tooltip",
  component: TooltipRoot,
} satisfies Meta;

export default meta;

export const Tooltip: StoryObj = {
  render: () => {
    return (
      <TooltipProvider>
        <TooltipRoot>
          <TooltipTrigger asChild>
            <Button variant="outline">Hover</Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>Add to library</p>
          </TooltipContent>
        </TooltipRoot>
      </TooltipProvider>
    );
  },
};
