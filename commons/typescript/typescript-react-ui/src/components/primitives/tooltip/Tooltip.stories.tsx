/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Button } from "../button";
import { TooltipContent, TooltipProvider, Tooltip as TooltipRoot, TooltipTrigger } from "./Tooltip";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Tooltip",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Tooltip: Story = () => {
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
};
