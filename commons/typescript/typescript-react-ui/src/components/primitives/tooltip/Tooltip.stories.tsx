/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../button";
import { TooltipContent, TooltipProvider, Tooltip as TooltipRoot, TooltipTrigger } from "./Tooltip";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Tooltip",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Tooltip: StoryObj<typeof meta> = {
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
