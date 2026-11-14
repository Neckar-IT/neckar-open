/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { StepDivider as StepDividerComponent } from "./StepDivider";

/**
 * Meta Information
 */

export default {
  title: "Layout / StepDivider",
} satisfies StoryDefault;

/**
 * Stories
 */

export const StepDivider: Story = () => {
  return (
    <div className="flex items-center rounded-md border">
      <div className="flex-1 px-4">Step 1</div>
      <StepDividerComponent />
      <div className="flex-1 px-4">Step 2</div>
      <StepDividerComponent />
      <div className="flex-1 px-4">Step 3</div>
    </div>
  );
};
