/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Label } from "../label";
import { RadioGroupItem, RadioGroup as RadioGroupRoot } from "./RadioGroup";

/**
 * Meta Information
 */

export default {
  title: "Primitives / RadioGroup",
} satisfies StoryDefault;

/**
 * Stories
 */

export const RadioGroup: Story = () => {
  return (
    <RadioGroupRoot defaultValue="comfortable">
      <div className="flex items-center space-x-2">
        <RadioGroupItem id="r1" value="default" />
        <Label htmlFor="r1">Default</Label>
      </div>
      <div className="flex items-center space-x-2">
        <RadioGroupItem id="r2" value="comfortable" />
        <Label htmlFor="r2">Comfortable</Label>
      </div>
      <div className="flex items-center space-x-2">
        <RadioGroupItem id="r3" value="compact" />
        <Label htmlFor="r3">Compact</Label>
      </div>
    </RadioGroupRoot>
  );
};
