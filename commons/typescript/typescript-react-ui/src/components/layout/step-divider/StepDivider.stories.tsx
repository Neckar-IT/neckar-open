/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { StepDivider as StepDividerComponent } from "./StepDivider";

/**
 * Meta Information
 */

const meta = {
  title: "Layout / StepDivider",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const StepDivider: StoryObj<typeof meta> = {
  render: () => {
    return (
      <div className="flex items-center rounded-md border">
        <div className="flex-1 px-4">Step 1</div>
        <StepDividerComponent />
        <div className="flex-1 px-4">Step 2</div>
        <StepDividerComponent />
        <div className="flex-1 px-4">Step 3</div>
      </div>
    );
  },
};
