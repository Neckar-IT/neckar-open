/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Input as InputComponent } from "./Input";

const meta = {
  title: "Primitives / Input",
  component: InputComponent,
  decorators: [
    (Story) => (
      <div className="w-80">
        <Story />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

export const Input: StoryObj = {
  render: () => {
    return <InputComponent placeholder="Email" type="email" />;
  },
};
