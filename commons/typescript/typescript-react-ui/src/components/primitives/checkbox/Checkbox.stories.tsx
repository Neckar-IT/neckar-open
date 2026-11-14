/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Checkbox as CheckboxComponent } from ".";
import { Label } from "../label";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Checkbox",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Checkbox: Story = () => {
  return (
    <div>
      <div className="flex items-center space-x-2">
        <CheckboxComponent id="terms" />
        <Label htmlFor="terms">Accept terms and conditions</Label>
      </div>
    </div>
  );
};
