/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Checkbox } from "../checkbox";
import { Label as LabelComponent } from "./Label";

const meta = {
  title: "Primitives / Label",
  component: LabelComponent,
} satisfies Meta;

export default meta;

export const Label: StoryObj = {
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
