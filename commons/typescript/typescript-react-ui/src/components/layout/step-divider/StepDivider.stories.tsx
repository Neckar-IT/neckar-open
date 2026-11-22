/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { StepDivider as StepDividerComponent } from "./StepDivider";

const meta = {
  title: "Layout / StepDivider",
  component: StepDividerComponent,
} satisfies Meta;

export default meta;

export const StepDivider: StoryObj = {
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
