/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Label } from "../label";
import { RadioGroupItem, RadioGroup as RadioGroupRoot } from "./RadioGroup";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / RadioGroup",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const RadioGroup: StoryObj<typeof meta> = {
  render: () => {
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
  },
};
