/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Checkbox as CheckboxComponent } from ".";
import { Label } from "../label";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Checkbox",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Checkbox: StoryObj<typeof meta> = {
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
