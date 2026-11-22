/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Checkbox as CheckboxComponent } from ".";
import { Label } from "../label";

const meta = {
  title: "Primitives / Checkbox",
  component: CheckboxComponent,
} satisfies Meta;

export default meta;

export const Checkbox: StoryObj = {
  render: () => {
    return (
      <div>
        <div className="flex items-center space-x-2">
          <CheckboxComponent id="terms" />
          <Label htmlFor="terms">Accept terms and conditions</Label>
        </div>
      </div>
    );
  },
};
