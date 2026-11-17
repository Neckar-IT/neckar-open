/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Checkbox } from "../checkbox";
import { Label as LabelComponent } from "./Label";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Label",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Label: StoryObj<typeof meta> = {
  render: () => {
    return (
      <div>
        <div className="flex items-center space-x-2">
          <Checkbox id="terms" />
          <LabelComponent htmlFor="terms">Accept terms and conditions</LabelComponent>
        </div>
      </div>
    );
  },
};
