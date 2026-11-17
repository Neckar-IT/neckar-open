/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Input as InputComponent } from "./Input";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Input",
  decorators: [
    (Story) => (
      <div className="w-80">
        <Story />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Input: StoryObj<typeof meta> = {
  render: () => {
    return <InputComponent placeholder="Email" type="email" />;
  },
};
