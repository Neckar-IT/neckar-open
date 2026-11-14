/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Checkbox } from "../checkbox";
import { Label as LabelComponent } from "./Label";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Label",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Label: Story = () => {
  return (
    <div>
      <div className="flex items-center space-x-2">
        <Checkbox id="terms" />
        <LabelComponent htmlFor="terms">Accept terms and conditions</LabelComponent>
      </div>
    </div>
  );
};
